package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AutoRequeueFeature extends Feature {

    private static final Pattern PARTY_DT_PATTERN = Pattern.compile(
            "^(?:Party > |(?:\\[[^]]+] )?)(?:\\[[^]]+] )?([A-Za-z0-9_]+):\\s*[!.]dt(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE
    );

    private int pendingRequeueTicks = -1;
    private boolean downtimeRequested = false;
    private String downtimeReason;

    public AutoRequeueFeature() {
        super("autoRequeue", "Auto Requeue",
                () -> KuudraGeneralConfig.autoRequeue
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
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

        MessageUtil.WARNING.sendMessage(String.format("Auto Requeue Cancelled: %s requested DT (&f%s&e).",
                playerName, downtimeReason));
        MessageUtil.showTitle("§c§lDT requested", "§eAuto Requeue Cancelled", 5, 45, 10);
        log.info("DT detected from {}. Auto requeue blocked. reason={}", playerName, downtimeReason);
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            downtimeRequested = false;
            downtimeReason = null;
            pendingRequeueTicks = -1;
            return;
        }

        if (event.isExitingKuudra() && pendingRequeueTicks >= 0) {
            log.debug("Exited Kuudra before auto-requeue command was sent; clearing pending command");
            pendingRequeueTicks = -1;
        }

        if (event.isRunCompleted() && pendingRequeueTicks >= 0) {
            if (mc.player == null || mc.player.networkHandler == null) return;

            pendingRequeueTicks--;
            if (pendingRequeueTicks > 0) return;

            pendingRequeueTicks = -1;

            if (downtimeRequested) {
                log.info("Skipping auto-requeue because DT was requested before command execution");
                return;
            }

            if (mc.world == null || mc.currentScreen != null) {
                log.debug("Skipping auto-requeue because player context is not ready");
                return;
            }

            mc.player.networkHandler.sendChatCommand("instancerequeue");
            MessageUtil.sendFormattedMessage("&aAuto Requeue: executado &f/instancerequeue&a.");
            log.info("Executed auto-requeue command");
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (!event.completed()) {
            pendingRequeueTicks = -1;
            return;
        }

        if (downtimeRequested) {
            MessageUtil.WARNING.sendMessage(String.format("DT detected. Auto Requeue will not be executed. (%s)",
                    downtimeReason != null
                            ? downtimeReason
                            : "No reason provided")
            );
            MessageUtil.showTitle("&cSem requeue", "&eDT detectado", 5, 45, 10);
            pendingRequeueTicks = -1;
            return;
        }

        pendingRequeueTicks = Math.max(1, KuudraGeneralConfig.requeueDelay);
        log.info("Kuudra run completed. Scheduling /instancerequeue in {} ticks", pendingRequeueTicks);
    }

    @Override
    protected void onDeactivate() {
        pendingRequeueTicks = -1;
        downtimeRequested = false;
        downtimeReason = null;
    }
}
