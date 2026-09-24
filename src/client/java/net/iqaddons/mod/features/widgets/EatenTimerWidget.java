package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ActionBarReceivedEvent;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.MobEffectReceivedEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Pattern;

import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public class EatenTimerWidget extends HudWidget {

    private static final int INITIAL_TIMER_TICKS = 95;
    private static final int TWO_SUBTITLE_TIMER_TICKS = 75;
    private static final int ONE_SUBTITLE_TIMER_TICKS = 55;
    private static final int COUNTDOWN_END_TIMER_TICKS = 35;
    private static final int TICKS_AFTER_BLINDNESS = 9;
    private static final int NOW_VISIBLE_TICKS = 20;

    private static final Pattern COUNTDOWN_SUBTITLE_PATTERN = Pattern.compile("^[123]$");
    private static final String STUNNED_MESSAGE = "Kuudra is stunned!";

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine timerLine;

    private long currentTick = -1L;
    private long eatenAtTick = -1L;
    private long nowUntilTick = -1L;
    private long pendingStartAtTick = -1L;
    private long lastProcessedTick = -1L;
    private boolean hadBlindness = false;

    public EatenTimerWidget() {
        super(
                "eatenTimer",
                "Eaten Timer",
                422.0f, 316.44556f,
                1.6000001f,
                HudAnchor.TOP_LEFT
        );

        timerLine = HudLine.of("§b§lEaten: §a§l4.00s")
                .showWhen(this::hasVisibleTimer);

        setEnabledSupplier(() -> PhaseThreeConfig.eatenTimer);
        setVisibilityCondition(() -> isTrackedPhase() || hasVisibleTimer());
        setExampleLines(HudLine.of("§b§lEaten: §e§l1.51s"));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLine(timerLine);

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(TitleReceivedEvent.class, this::onTitleReceived);
        subscribe(ActionBarReceivedEvent.class, this::onActionBarReceived);
        subscribe(MobEffectReceivedEvent.class, this::onMobEffectReceived);
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(KuudraRunEndEvent.class, event -> resetTimer("run ended: " + event.reason()));
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        log.info("[EatenTimer] Activated. phase={} enabled={}", stateManager.phase(), PhaseThreeConfig.eatenTimer);
    }

    @Override
    protected void onDeactivate() {
        resetTimer("widget deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        currentTick = event.tickCount();
        if (lastProcessedTick == currentTick) {
            return;
        }
        lastProcessedTick = currentTick;

        if (!PhaseThreeConfig.eatenTimer) {
            resetTimer("feature disabled");
            return;
        }

        if (!event.isInGame()) {
            resetTimer("not in game");
            return;
        }

        if (!isTrackedPhase() && !hasVisibleTimer()) {
            resetTimer("outside tracked phase: " + stateManager.phase());
            return;
        }

        boolean hasBlindness = event.client().player != null
                && event.client().player.hasEffect(MobEffects.BLINDNESS);
        if (hasBlindness && !hadBlindness) {
            handleBlindnessDetected("effect state");
        }
        hadBlindness = hasBlindness;

        if (isWaitingForCountdownEnd() && currentTick >= pendingStartAtTick) {
            log.info("[EatenTimer] Countdown ended at tick {}. Starting with {} ticks.", currentTick, COUNTDOWN_END_TIMER_TICKS);
            syncTimer(COUNTDOWN_END_TIMER_TICKS);
        }

        if (isCountingDown() && getRemainingTicks() <= 0) {
            showNow();
        }

        if (hasVisibleTimer()) {
            updateTimerLine();
            return;
        }

        if (eatenAtTick >= 0L || nowUntilTick >= 0L) {
            clearVisibleState();
        }
    }

    private void onTitleReceived(@NotNull TitleReceivedEvent event) {
        String title = normalizePacketText(event.getTitle().getString());
        String subtitle = normalizePacketText(event.getSubtitle().getString());
        String countdownText = getCountdownText(title, subtitle);

        if (isStunnedMessage(title) || isStunnedMessage(subtitle)) {
            resetTimer("stunned title/subtitle");
            return;
        }

        if (!title.isBlank() || !subtitle.isBlank()) {
            log.info(
                    "[EatenTimer] Title/subtitle packet received at tick {} while phase={}. title='{}' subtitle='{}' countdown={}",
                    currentTick,
                    stateManager.phase(),
                    title,
                    subtitle,
                    countdownText
            );
        }

        if (!isTrackedPhase() || countdownText == null) {
            return;
        }

        if (PhaseThreeConfig.eatenTimerConfig.hideDefaultCountdown) {
            event.setCancelled(true);
        }

        int countdownValue = Integer.parseInt(countdownText);
        handleCountdownSync("countdown", countdownValue);
    }

    private void onMobEffectReceived(@NotNull MobEffectReceivedEvent event) {
        if (!isTrackedPhase() || currentTick < 0L || !MobEffects.BLINDNESS.equals(event.getEffect())) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null || event.getEntityId() != player.getId()) {
            return;
        }

        log.info("[EatenTimer] BLINDNESS packet received at tick {}. duration={}", currentTick, event.getDurationTicks());
        handleBlindnessDetected("effect packet");
        hadBlindness = true;
    }

    private void onActionBarReceived(@NotNull ActionBarReceivedEvent event) {
        if (!isTrackedPhase()) {
            return;
        }

        String message = normalizePacketText(event.getStrippedMessage());
        if (!message.isBlank() && message.length() <= 40) {
            log.info("[EatenTimer] Overlay received at tick {} while phase={}: '{}'", currentTick, stateManager.phase(), message);
        }

        String countdownText = getCountdownText(message, "");
        if (countdownText == null) {
            return;
        }

        int countdownValue = Integer.parseInt(countdownText);
        handleCountdownSync("overlay", countdownValue);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (isStunnedMessage(message)
                || message.contains("Sending to server")
                || message.contains("Starting in 4 seconds...")) {
            resetTimer("chat trigger: " + message);
        }
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        log.info("[EatenTimer] Phase change {} -> {}.", event.previousPhase(), event.currentPhase());
        if (event.isEnteringKuudra() || event.isExitingKuudra() || event.isRunCompleted()) {
            resetTimer("phase hard reset: " + event.previousPhase() + " -> " + event.currentPhase());
            return;
        }

        if (!isTrackedPhase(event.currentPhase()) && !hasVisibleTimer()) {
            resetTimer("phase left tracked window: " + event.currentPhase());
        }
    }

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        if (!event.onSkyBlock() || (!event.newArea().isBlank() && !event.newArea().contains(KUUDRA_AREA_ID))) {
            resetTimer("area change: onSkyBlock=" + event.onSkyBlock() + " newArea=" + event.newArea());
        }
    }

    private void syncTimer(int ticksRemaining) {
        if (currentTick < 0L) {
            return;
        }

        pendingStartAtTick = -1L;
        eatenAtTick = currentTick + Math.max(0, ticksRemaining);
        nowUntilTick = -1L;
        log.info("[EatenTimer] Timer synced. currentTick={} eatenAtTick={} remainingTicks={}", currentTick, eatenAtTick, ticksRemaining);
        updateTimerLine();
    }

    private void waitForCountdownEnd(int countdownValue) {
        if (currentTick < 0L) {
            return;
        }

        pendingStartAtTick = currentTick + Math.max(0, countdownValue * 20L);
        eatenAtTick = -1L;
        nowUntilTick = -1L;
        timerLine.text("");
        markDimensionsDirty();
        log.info("[EatenTimer] Waiting for countdown end. currentTick={} countdown={} pendingStartAtTick={}",
                currentTick, countdownValue, pendingStartAtTick);
    }

    private void handleCountdownSync(@NotNull String source, int countdownValue) {
        int ticksRemaining = ticksForCountdownValue(countdownValue);
        if (ticksRemaining < 0) {
            return;
        }

        switch (PhaseThreeConfig.eatenTimerConfig.startMode) {
            case COUNTDOWN_START -> {
                log.info("[EatenTimer] Sync from {} {} -> {} ticks.", source, countdownValue, ticksRemaining);
                syncTimer(ticksRemaining);
            }
            case COUNTDOWN_END -> waitForCountdownEnd(countdownValue);
            case BLINDNESS_EFFECT -> log.info("[EatenTimer] Ignoring {} {} until blindness effect.", source, countdownValue);
        }
    }

    private int ticksForCountdownValue(int countdownValue) {
        return switch (countdownValue) {
            case 3 -> INITIAL_TIMER_TICKS;
            case 2 -> TWO_SUBTITLE_TIMER_TICKS;
            case 1 -> ONE_SUBTITLE_TIMER_TICKS;
            default -> -1;
        };
    }

    private void handleBlindnessDetected(@NotNull String source) {
        if (PhaseThreeConfig.eatenTimerConfig.startMode == PhaseThreeConfig.EatenTimerStartMode.BLINDNESS_EFFECT
                || isCountingDown()
                || isWaitingForCountdownEnd()) {
            log.info("[EatenTimer] BLINDNESS detected from {} at tick {}. Re-syncing to {} ticks.",
                    source, currentTick, TICKS_AFTER_BLINDNESS);
            syncTimer(TICKS_AFTER_BLINDNESS);
        }
    }

    private void showNow() {
        eatenAtTick = -1L;
        nowUntilTick = currentTick + NOW_VISIBLE_TICKS;
        log.info("[EatenTimer] Timer reached zero. Showing READY until tick {}.", nowUntilTick);
        updateTimerLine();
    }

    private void resetTimer() {
        resetTimer("unspecified");
    }

    private void resetTimer(@NotNull String reason) {
        if (hasVisibleTimer() || eatenAtTick >= 0L || nowUntilTick >= 0L) {
            log.info("[EatenTimer] Resetting timer: {}", reason);
        }
        clearVisibleState();
        currentTick = -1L;
        lastProcessedTick = -1L;
        hadBlindness = false;
    }

    private void clearVisibleState() {
        eatenAtTick = -1L;
        nowUntilTick = -1L;
        pendingStartAtTick = -1L;
        timerLine.text("");
        markDimensionsDirty();
    }

    private void updateTimerLine() {
        if (isShowingNow()) {
            timerLine.text("§b§lEaten: §a§lREADY");
            markDimensionsDirty();
            return;
        }

        int remainingTicks = Math.max(0, getRemainingTicks());
        timerLine.text(String.format(
                Locale.ROOT,
                "§b§lEaten: %s§l%s",
                getTimerColor(remainingTicks),
                formatTimerText(remainingTicks)
        ));
        markDimensionsDirty();
    }

    private @NotNull String formatTimerText(int remainingTicks) {
        return switch (PhaseThreeConfig.eatenTimerConfig.timerType) {
            case TIMER_MS -> (remainingTicks * 50) + "ms";
            case TIMER_TICKS -> remainingTicks + "t";
            case TIMER_SECONDS -> String.format(Locale.ROOT, "%.2fs", remainingTicks / 20.0);
        };
    }

    private boolean hasVisibleTimer() {
        return isCountingDown() || isShowingNow();
    }

    private boolean isWaitingForCountdownEnd() {
        return pendingStartAtTick >= 0L && currentTick >= 0L;
    }

    private boolean isCountingDown() {
        return eatenAtTick >= 0L && currentTick >= 0L && currentTick <= eatenAtTick;
    }

    private boolean isShowingNow() {
        return nowUntilTick >= 0L && currentTick >= 0L && currentTick <= nowUntilTick;
    }

    private int getRemainingTicks() {
        if (eatenAtTick < 0L || currentTick < 0L) {
            return 0;
        }

        return (int) Math.max(0L, eatenAtTick - currentTick);
    }

    private @NotNull String getTimerColor(int remainingTicks) {
        double ratio = Math.min(1.0, Math.max(0.0, remainingTicks / (double) INITIAL_TIMER_TICKS));
        if (ratio > 0.75) {
            return "§a";
        }
        if (ratio > 0.50) {
            return "§e";
        }
        if (ratio > 0.25) {
            return "§6";
        }
        return "§c";
    }

    private boolean isTrackedPhase() {
        return isTrackedPhase(stateManager.phase());
    }

    private boolean isTrackedPhase(@NotNull KuudraPhase phase) {
        return phase == KuudraPhase.BUILD || phase == KuudraPhase.EATEN;
    }

    private boolean isStunnedMessage(@NotNull String message) {
        return message.contains(STUNNED_MESSAGE);
    }

    private @NotNull String normalizePacketText(@NotNull String message) {
        return message
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("[\\p{Cntrl}\\u200B-\\u200F\\uFEFF]", "")
                .trim();
    }

    private String getCountdownText(@NotNull String title, @NotNull String subtitle) {
        if (COUNTDOWN_SUBTITLE_PATTERN.matcher(subtitle).matches()) {
            return subtitle;
        }

        if (COUNTDOWN_SUBTITLE_PATTERN.matcher(title).matches()) {
            return title;
        }

        return null;
    }
}
