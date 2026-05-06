package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AutoRequeueFeature extends Feature {

    private static final int MIN_SAFE_REQUEUE_DELAY_TICKS = 10;

    private static final Pattern PARTY_DT_PATTERN = Pattern.compile(
            "^(?:Party >\\s*)?(?:\\[[^]]+]\\s*)?(?:\\[[^]]+]\\s*)?([A-Za-z0-9_]+):\\s*[!.]dt(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE
    );

    private int pendingRequeueTicks = -1;
    private boolean downtimeRequested = false;
    private String downtimeReason;
    private boolean waitingForRequeueResponse = false;

    public AutoRequeueFeature() {
        super("autoRequeue", "Auto Requeue",
                () -> KuudraGeneralConfig.autoRequeue
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(ClientTickEvent.class, this::onClientTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (!isEnabled()) {
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (pendingRequeueTicks < 0) return;
        if (!event.isInGame()) return;

        if (pendingRequeueTicks > 0) {
            pendingRequeueTicks--;
            if (pendingRequeueTicks > 0) return;
        }

        if (downtimeRequested) {
            log.debug("Skipping auto-requeue because DT was requested before command execution");
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (mc.player == null || mc.player.networkHandler == null) {
            log.debug("Skipping auto-requeue because player context is not ready");
            return;
        }

        if (mc.world == null || mc.currentScreen != null) {
            log.debug("Auto-requeue pending, waiting for world/screen to be ready");
            return;
        }

        pendingRequeueTicks = -1;
        waitingForRequeueResponse = true;
        mc.player.networkHandler.sendChatCommand("instancerequeue");
        MessageUtil.INFO.sendMessage("§aAuto Requeue: executing §f/instancerequeue§a.");
        log.info("Executed auto-requeue command");
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        if (!isEnabled()) return;

        String message = event.getStrippedMessage();
        if (message.contains("You are not allowed to use that command as a spectator!")) {
            if (!waitingForRequeueResponse || downtimeRequested) {
                return;
            }

            pendingRequeueTicks = 15;
            return;
        }

        if (waitingForRequeueResponse) {
            // Any other response means we should stop treating future spectator messages as our own retries.
            waitingForRequeueResponse = false;
        }

        Matcher matcher = PARTY_DT_PATTERN.matcher(message);
        if (!matcher.find()) return;

        String playerName = matcher.group(1);
        String reason = matcher.group(2);

        downtimeRequested = true;
        downtimeReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "No reason provided";

        if (pendingRequeueTicks >= 0) {
            pendingRequeueTicks = -1;
        }
        waitingForRequeueResponse = false;

        MessageUtil.WARNING.sendMessage(String.format("Auto Requeue Cancelled: %s requested DT (§f%s§e).",
                playerName, downtimeReason));
        MessageUtil.showTitle("§c§lDT requested", "§eAuto Requeue Cancelled", 5, 45, 10);
        log.info("DT detected from {}. Auto requeue blocked. reason={}", playerName, downtimeReason);
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            downtimeRequested = false;
            downtimeReason = null;
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (event.isExitingKuudra() && pendingRequeueTicks >= 0) {
            log.debug("Exited Kuudra before auto-requeue command was sent; clearing pending command");
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
        }

        if (event.isRunCompleted() && pendingRequeueTicks >= 0) {
            log.debug("Run completed phase detected while auto-requeue is pending");
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (!isEnabled()) {
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (!event.isCompleted() && !event.isFailed()) {
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (downtimeRequested) {
            MessageUtil.WARNING.sendMessage(String.format("DT detected. Auto Requeue will not be executed. (§f%s§e)",
                    downtimeReason != null
                            ? downtimeReason
                            : "No reason provided")
            );
            MessageUtil.showTitle("§c§lREQUEUE CANCELLED", "§eDT Detected", 5, 45, 10);
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        if (shouldAutoStopOnFastRun(event)) {
            pendingRequeueTicks = -1;
            waitingForRequeueResponse = false;
            return;
        }

        pendingRequeueTicks = Math.max(MIN_SAFE_REQUEUE_DELAY_TICKS, Math.max(1, KuudraGeneralConfig.AutoRequeueConfig.requeueDelay));
        waitingForRequeueResponse = false;
        log.info("Kuudra run completed. Scheduling /instancerequeue in {} ticks", pendingRequeueTicks);
    }

    private boolean shouldAutoStopOnFastRun(@NotNull KuudraRunEndEvent event) {
        if (!KuudraGeneralConfig.AutoRequeueConfig.autoStopAutoRequeue) {
            return false;
        }

        if (!event.isCompleted()) {
            return false;
        }

        long runMillis = Math.max(0L, event.totalDuration().toMillis());
        long stopThresholdMillis = Math.max(1L, Math.round(KuudraGeneralConfig.AutoRequeueConfig.autoStopAutoRequeueOverallSeconds * 1000.0));
        if (runMillis <= 0L || runMillis > stopThresholdMillis) {
            return false;
        }

        String runTime = String.format(Locale.ROOT, "%.2fs", runMillis / 1000.0);
        String stopTime = String.format(Locale.ROOT, "%.2fs", stopThresholdMillis / 1000.0);
        MessageUtil.WARNING.sendMessage("Auto Requeue paused: run overall " + runTime + " <= " + stopTime + ".");
        MessageUtil.showTitle("§e§lAUTO REQUEUE PAUSED", "§fRun: " + runTime, 5, 40, 10);
        log.info("Auto requeue paused by fast-run auto-stop (run={}ms, threshold={}ms)", runMillis, stopThresholdMillis);
        return true;
    }

    @Override
    protected void onDeactivate() {
        pendingRequeueTicks = -1;
        downtimeRequested = false;
        downtimeReason = null;
        waitingForRequeueResponse = false;
    }
}
