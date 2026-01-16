package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.render.EntityGlowUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FreshAlertFeature extends KuudraFeature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String FRESH_TOOLS_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";
    private static final Pattern BUILDING_PROGRESS_PATTERN = Pattern.compile("Building Progress:\\s*(\\d+)%");
    private static final Pattern PARTY_FRESH_PATTERN = Pattern.compile("Party > (?:\\[[^]]+] )?(\\w+): FRESH!");
    private static final int FRESH_DURATION_SECONDS = 10;

    private final ScheduledExecutorService scheduler;
    private final Map<Integer, Long> glowingPlayers = new ConcurrentHashMap<>();

    public FreshAlertFeature(ScheduledExecutorService scheduler) {
        super(
                "freshMessage",
                "Fresh Message",
                () -> PhaseTwoConfig.freshMessage,
                KuudraPhase.BUILD
        );

        this.scheduler = scheduler;
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
        log.info("Fresh Message activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        glowingPlayers.keySet().forEach(EntityGlowUtil::removeGlowing);
        glowingPlayers.clear();

        log.info("Fresh Message deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            int buildProgress = ScoreboardUtils.findLine("Building Progress")
                    .map(this::extractProgress)
                    .orElse(0);;
            MessageUtil.PARTY.sendMessage("FRESH! (%d%%)".formatted(buildProgress));

            applyFreshGlow(player.getId(), player.getName().getString());
            log.debug("Fresh activated at {}% progress", buildProgress);
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            String playerName = partyMatcher.group(1);
            if (mc.player != null && playerName.equals(mc.player.getName().getString())) {
                return;
            }

            findPlayerByName(playerName).ifPresent(player -> {
                applyFreshGlow(player.getId(), playerName);
                log.debug("Applied Fresh glow to party member: {}", playerName);
            });
        }
    }

    private void applyFreshGlow(int entityId, String playerName) {
        EntityGlowUtil.setGlowing(entityId, RenderColor.fromArgb(PhaseTwoConfig.freshHightlightColor));
        glowingPlayers.put(entityId, System.currentTimeMillis());
        log.debug("Applied Fresh glow to {} (id: {})", playerName, entityId);

        scheduler.schedule(
                () -> removeFreshGlow(entityId),
                FRESH_DURATION_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void removeFreshGlow(int entityId) {
        if (glowingPlayers.remove(entityId) != null) {
            EntityGlowUtil.removeGlowing(entityId);
            log.debug("Removed Fresh glow from entity {}", entityId);
        }
    }

    private Optional<AbstractClientPlayerEntity> findPlayerByName(@NotNull String name) {
        if (mc.world == null) return Optional.empty();

        return mc.world.getPlayers().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(name))
                .findFirst();
    }

    private int extractProgress(@NotNull String line) {
        String stripped = ScoreboardUtils.stripFormatting(line);
        Matcher matcher = BUILDING_PROGRESS_PATTERN.matcher(stripped);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse building progress: {}", stripped);
            }
        }
        return 0;
    }
}