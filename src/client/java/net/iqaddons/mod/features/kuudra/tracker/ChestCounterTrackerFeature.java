package net.iqaddons.mod.features.kuudra.tracker;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestOpenEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.model.profit.ChestType;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ChestCounterTrackerFeature extends Feature {

    private static final long OVERLAY_TIMEOUT_MS = 3 * 60 * 1000L;
    public static boolean overlayVisible;

    private final ChestCounterManager manager = ChestCounterManager.get();

    private int chestCount;
    private long lastRunTimestamp;

    public ChestCounterTrackerFeature() {
        super("chestCounterTracker", "Chest Counter Tracker",
                () -> KuudraGeneralConfig.chestCounterTracker
        );
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraChestOpenEvent.class, this::onChestOpen);
        resetRuntimeState();
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (!event.isCompleted()) return;

        int current = manager.increment();
        lastRunTimestamp = System.currentTimeMillis();
        overlayVisible = true;

        if (current % 10 == 0 && current < ChestCounterManager.MAX_CHESTS && KuudraGeneralConfig.chestCounterPartyAnnouncements) {
            MessageUtil.PARTY.sendMessage("[IQ] Completed " + current + " runs, " + (ChestCounterManager.MAX_CHESTS - current) + " left to reach my chest limit.");
            playLevelUp(0.9f);
        }

        if (current == 59 && KuudraGeneralConfig.chestCounterPartyAnnouncements) {
            MessageUtil.PARTY.sendMessage("!dt [IQ] Run 59/60, opening chests next run.");
        }

        if (current == ChestCounterManager.MAX_CHESTS) {
            playLevelUp(1.0f);
            MessageUtil.showTitle("§b§l60/60", "§aChest limit reached", 0, 30, 10);
            if (KuudraGeneralConfig.chestCounterPartyAnnouncements) {
                MessageUtil.PARTY.sendMessage("[IQ] Run 60/60, opening chests...");
            }

            MessageUtil.sendFormattedMessage("§fYour chest tracker is full! Run §e/iq resetchests §fto reset your progress.");
        }
    }

    private void onChestOpen(@NotNull KuudraChestOpenEvent event) {
        if (event.chestType() == ChestType.PAID) {
            chestCount++;
            int chests = manager.getChests();
            if (chestCount >= 5 && chests >= ChestCounterManager.MAX_CHESTS) {
                manager.reset();
                resetRuntimeState();
                MessageUtil.sendFormattedMessage("§fChest tracker reset automatically (chests opened).");
                playLevelUp(1.0f);
            }
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
    }

    private void resetRuntimeState() {
        chestCount = 0;
        overlayVisible = false;
        lastRunTimestamp = 0L;
    }

    private void playLevelUp(float pitch) {
        if (mc.player == null) return;
        mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);
    }
}