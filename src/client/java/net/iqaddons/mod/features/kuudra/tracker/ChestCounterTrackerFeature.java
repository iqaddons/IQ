package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class ChestCounterTrackerFeature extends Feature {

    private static final long OVERLAY_TIMEOUT_MS = 3 * 60 * 1000L;

    private final ChestCounterManager manager = ChestCounterManager.get();

    private int paidChestCount;
    private boolean autoResetLocked;
    private boolean overlayVisible;
    private long lastRunTimestamp;

    public ChestCounterTrackerFeature() {
        super("chestCounterTracker", "Chest Counter Tracker", () -> KuudraGeneralConfig.chestCounterTracker);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
        resetRuntimeState();
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        String reason = event.reason();
        if (!event.completed() && !reason.contains("DEFEAT")) return;

        int current = manager.increment();
        lastRunTimestamp = System.currentTimeMillis();
        overlayVisible = true;

        if (current % 10 == 0 && current < ChestCounterManager.MAX_CHESTS && KuudraGeneralConfig.chestCounterPartyAnnouncements) {
            MessageUtil.PARTY.sendMessage("[IQ] Completed " + current + " runs, " + (ChestCounterManager.MAX_CHESTS - current) + " left to reach my chest limit.");
            playLevelUp(0.9f);
        }

        if (current == 59 && KuudraGeneralConfig.chestCounterPartyAnnouncements) {
            MessageUtil.PARTY.sendMessage("[IQ] Run 59/60, opening chests next run.");
        }

        if (current == ChestCounterManager.MAX_CHESTS) {
            playLevelUp(1.0f);
            MessageUtil.showTitle("§b§l60/60", "§aChest limit reached", 0, 30, 10);
            if (KuudraGeneralConfig.chestCounterPartyAnnouncements) {
                MessageUtil.PARTY.sendMessage("[IQ] Run 60/60, opening chests.");
            }

            MessageUtil.sendFormattedMessage("§fYour chest tracker is full! Run §b/iq resetchests §fto reset your progress.");
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!event.getStrippedMessage().contains("PAID CHEST REWARDS")) {
            return;
        }

        if (autoResetLocked) return;

        paidChestCount++;
        int chests = manager.getChests();
        if (paidChestCount >= 5 && chests >= ChestCounterManager.MAX_CHESTS) {
            manager.reset();
            resetRuntimeState();
            MessageUtil.sendFormattedMessage("§fChest tracker reset automatically (chests opened).");
            playLevelUp(1.0f);
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!overlayVisible || !event.isNthTick(10)) {
            return;
        }

        if (lastRunTimestamp > 0 && System.currentTimeMillis() - lastRunTimestamp >= OVERLAY_TIMEOUT_MS) {
            overlayVisible = false;
            MessageUtil.sendFormattedMessage("§fNo runs completed in the last 3 minutes. Overlay hidden.");
        }

        if (manager.getChests() < ChestCounterManager.MAX_CHESTS) {
            autoResetLocked = false;
        }
    }

    private void resetRuntimeState() {
        paidChestCount = 0;
        autoResetLocked = false;
        overlayVisible = false;
        lastRunTimestamp = 0L;
    }

    private void playLevelUp(float pitch) {
        if (mc.player == null) return;
        mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);
    }
}