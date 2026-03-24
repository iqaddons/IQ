package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityGlowUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static net.iqaddons.mod.utils.EntityGlowUtil.PRIORITY_TEAM_HIGHLIGHT;

@Slf4j
public class TeamHighlightFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int REAL_PLAYER_PING = 1;

    private final Set<Integer> highlightedPlayers = new HashSet<>();

    public TeamHighlightFeature() {
        super(
                "teamHighlight",
                "Team Highlight",
                () -> PhaseTwoConfig.teamHighlight,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        highlightedPlayers.clear();
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        clearAllHighlights();
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isExitingKuudra() || event.isRunCompleted()) {
            clearAllHighlights();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(UPDATE_INTERVAL_TICKS)) return;

        updateTeammateHighlights();
    }

    private void updateTeammateHighlights() {
        if (mc.world == null || mc.player == null) {
            clearAllHighlights();
            return;
        }

        Set<Integer> currentTeammates = new HashSet<>();
        RenderColor teamColor = RenderColor.fromArgb(PhaseTwoConfig.teamHighlightColor);

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!isRealPlayer(player)) continue;

            int playerId = player.getId();
            currentTeammates.add(playerId);
            highlightedPlayers.add(playerId);

            boolean hasHigherPriorityGlow = EntityGlowUtil.isGlowingWithPriority(playerId, PRIORITY_TEAM_HIGHLIGHT + 1);
            if (!hasHigherPriorityGlow) {
                RenderColor currentColor = EntityGlowUtil.getGlowColor(playerId);
                if (currentColor == null || currentColor.argb != teamColor.argb) {
                    EntityGlowUtil.setGlowing(playerId, teamColor, PRIORITY_TEAM_HIGHLIGHT);
                    log.debug("Highlighted teammate: {}", player.getName().getString());
                }
            }
        }

        Set<Integer> toRemove = new HashSet<>();
        for (Integer playerId : highlightedPlayers) {
            if (!currentTeammates.contains(playerId)) {
                RenderColor currentColor = EntityGlowUtil.getGlowColor(playerId);
                if (currentColor != null) EntityGlowUtil.removeGlowing(playerId, PRIORITY_TEAM_HIGHLIGHT);

                toRemove.add(playerId);
            }
        }
        highlightedPlayers.removeAll(toRemove);
    }


    private boolean isRealPlayer(@NotNull AbstractClientPlayerEntity player) {
        if (mc.getNetworkHandler() == null) return false;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) return false;

        return entry.getLatency() == REAL_PLAYER_PING;
    }

    private void clearAllHighlights() {
        for (Integer playerId : highlightedPlayers) {
            RenderColor currentColor = EntityGlowUtil.getGlowColor(playerId);
            if (currentColor != null) EntityGlowUtil.removeGlowing(playerId, PRIORITY_TEAM_HIGHLIGHT);
        }

        highlightedPlayers.clear();
    }
}