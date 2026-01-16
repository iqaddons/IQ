package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
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
    private static final Pattern PROTECT_ELLE_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");
    private static final Pattern PARTY_FRESH_PATTERN = Pattern.compile("Party > (?:\\[[^]]+] )?(\\w+): FRESH!");

    private static final int FRESH_DURATION_MS = 10_000; // 10 seconds

    private final ScheduledExecutorService scheduler;
    private final Map<Integer, Long> glowingPlayers = new ConcurrentHashMap<>();

    private volatile long localFreshStartTime = 0;
    private volatile boolean localFreshActive = false;

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
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        log.info("Fresh Message activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        glowingPlayers.keySet().forEach(EntityGlowUtil::removeGlowing);
        glowingPlayers.clear();

        localFreshActive = false;
        localFreshStartTime = 0;

        clearFreshTitle();

        log.info("Fresh Message deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        if (localFreshActive) {
            long elapsed = System.currentTimeMillis() - localFreshStartTime;
            long remaining = FRESH_DURATION_MS - elapsed;

            if (remaining <= 0) {
                localFreshActive = false;
                localFreshStartTime = 0;

                clearFreshTitle();
                log.debug("Fresh expired, cleared title");
            } else if (PhaseTwoConfig.freshCountdown){
                double remainingSeconds = remaining / 1000.0;
                MessageUtil.showTitle(String.format("%s%.2fs",
                                getCountdownColor(remainingSeconds),
                                remainingSeconds
                        ), "", 0, 5, 5
                );
            }
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            int buildProgress = getBuildingProgress();
            MessageUtil.PARTY.sendMessage("FRESH! (%d%%)".formatted(buildProgress));

            localFreshStartTime = System.currentTimeMillis();
            localFreshActive = true;

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
                FRESH_DURATION_MS,
                TimeUnit.MILLISECONDS
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

    private int getBuildingProgress() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = ScoreboardUtils.stripFormatting(line);

            Matcher matcher = PROTECT_ELLE_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    int progress = Integer.parseInt(matcher.group(1));
                    log.debug("Found build progress: {}% from line: {}", progress, stripped);
                    return progress;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse progress from: {}", stripped);
                }
            }
        }

        log.warn("Could not find 'Protect Elle' on scoreboard");
        return 0;
    }

    private void clearFreshTitle() {
        if (mc.inGameHud != null) {
            mc.inGameHud.clearTitle();
        }
    }

    @NotNull
    private String getCountdownColor(double remainingSeconds) {
        if (remainingSeconds > 6.0) {
            return "§a§l";
        } else if (remainingSeconds > 3.0) {
            return "§e§l";
        } else {
            return "§c§l";
        }
    }
}