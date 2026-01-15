package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ManaDrainAlertFeature extends KuudraFeature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Pattern EXTREME_FOCUS_PATTERN = Pattern.compile("Used Extreme Focus! \\((\\d+) Mana\\)");
    private static final double AFFECT_RADIUS = 5.0;
    private static final int REAL_PLAYER_PING = 1;

    public ManaDrainAlertFeature() {
        super(
                "manaDrainNotify",
                "Mana Drain Notify",
                () -> Configuration.manaDrainNotify,
                KuudraPhase.RUN_PHASES.toArray(new KuudraPhase[0])
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
        log.info("Mana Drain Notify activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Mana Drain Notify deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        Matcher matcher = EXTREME_FOCUS_PATTERN.matcher(message);
        if (!matcher.find()) return;

        String manaAmount = matcher.group(1);
        int affectedPlayers = countAffectedPlayers();

        MessageUtil.PARTY.sendMessage("Used %s mana on %d players!".formatted(manaAmount, affectedPlayers));
        log.debug("Mana drain: {} mana on {} players", manaAmount, affectedPlayers);
    }

    private int countAffectedPlayers() {
        if (mc.player == null || mc.world == null) {
            return 0;
        }

        int count = 0;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            double distance = mc.player.distanceTo(player);
            if (distance > AFFECT_RADIUS) continue;
            if (isRealPlayer(player)) count++;
        }

        return count;
    }

    private boolean isRealPlayer(@NotNull AbstractClientPlayerEntity player) {
        if (mc.getNetworkHandler() == null) return false;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) return false;

        return entry.getLatency() == REAL_PLAYER_PING;
    }
}