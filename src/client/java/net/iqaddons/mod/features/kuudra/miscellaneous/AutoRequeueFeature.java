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
        subscribe(ClientTickEvent.class, this::onClientTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (pendingRequeueTicks < 0) return;
        if (!event.isInGame()) return;

        if (pendingRequeueTicks > 0) {
            pendingRequeueTicks--;
            if (pendingRequeueTicks > 0) return;
        }

        if (downtimeRequested) {
            log.debug("Skipping auto-requeue because DT was requested before command execution");
            pendingRequeueTicks = -1;
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
        mc.player.networkHandler.sendChatCommand("instancerequeue");
        MessageUtil.INFO.sendMessage("§aAuto Requeue: executing §f/instancerequeue§a.");
        log.info("Executed auto-requeue command");
    }
    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains("You are not allowed to use that command as a spectator!")) {
            pendingRequeueTicks = 15;
            return;
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
            return;
        }

        if (event.isExitingKuudra() && pendingRequeueTicks >= 0) {
            log.debug("Exited Kuudra before auto-requeue command was sent; clearing pending command");
            pendingRequeueTicks = -1;
        }

        if (event.isRunCompleted() && pendingRequeueTicks >= 0) {
            log.debug("Run completed phase detected while auto-requeue is pending");
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (!event.isCompleted() && !event.isFailed()) {
            pendingRequeueTicks = -1;
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
