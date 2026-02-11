package net.iqaddons.mod.features.kuudra.tracker;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraPriceCacheManager;
import net.iqaddons.mod.manager.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.profit.ChestData;
import net.iqaddons.mod.utils.ChestProfitParser;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KuudraProfitTrackerFeature extends Feature {

    private static final int BUY_SLOT = 31;
    private static final int REROLL_SLOT = 50;
    private static final int SHARD_REROLL_SLOT = 51;

    private final KuudraProfitTrackerManager manager = KuudraProfitTrackerManager.get();
    private final KuudraPriceCacheManager priceCache = KuudraPriceCacheManager.get();

    private final Map<Integer, ChestWindowState> windowStates = new ConcurrentHashMap<>();

    public KuudraProfitTrackerFeature() {
        super("kuudraProfitTracker", "Kuudra Profit Tracker",
                () -> KuudraGeneralConfig.kuudraProfitTracker
        );
    }

    @Override
    protected void onActivate() {
        priceCache.refreshAsyncIfStale();

        subscribe(KuudraRunEndEvent.class, this::onKuudraRunEnd);
        subscribe(ScreenClickEvent.class, this::onScreenClick);
    }

    private void onKuudraRunEnd(@NotNull KuudraRunEndEvent event) {
        manager.onRunEnd(event.totalDuration().toMillis(), !event.completed());
    }

    private void onScreenClick(@NotNull ScreenClickEvent event) {
        if (!(event.getScreen() instanceof GenericContainerScreen screen)) {
            return;
        }

        String title = screen.getTitle().getString();
        if (!title.contains("Paid Chest") && !title.contains("Free Chest")) {
            return;
        }

        Slot slot = event.getSlot();
        if (slot == null) {
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        int windowId = handler.syncId;
        ChestWindowState state = windowStates.computeIfAbsent(windowId, key -> new ChestWindowState());

        if (slot.id == REROLL_SLOT && !state.rerolled && canUseReroll(slot.getStack(), "rerolled this chest")) {
            state.rerolled = true;
            long kismetPrice = priceCache.getKismetPrice();
            manager.onReroll(false, kismetPrice);
            return;
        }

        if (slot.id == SHARD_REROLL_SLOT && !state.shardRerolled && canUseReroll(slot.getStack(), "rerolled this shard")) {
            state.shardRerolled = true;
            long wofPrice = priceCache.getWheelOfFatePrice();
            manager.onReroll(true, wofPrice);
            return;
        }

        if (slot.id != BUY_SLOT || state.bought) {
            return;
        }

        if (!isBuyAction(slot.getStack())) {
            return;
        }

        priceCache.refreshAsyncIfStale();

        ChestData parsed = ChestProfitParser.parseChest(handler.slots, priceCache, title);
        state.bought = true;

        manager.onChestBought(parsed);
    }

    private boolean canUseReroll(ItemStack stack, @NotNull String blockedPhrase) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String title = Formatting.strip(stack.getName().getString()).toLowerCase();
        if (!title.contains("reroll")) return false;

        String loreJoined = ChestProfitParser.getLoreLines(stack).stream()
                .map(ChestProfitParser::stripFormatting)
                .map(String::toLowerCase)
                .reduce("", (a, b) -> a + "\n" + b);

        return !loreJoined.contains(blockedPhrase.toLowerCase());
    }

    private boolean isBuyAction(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String name = Formatting.strip(stack.getName().getString());
        return name.equalsIgnoreCase("Open Reward Chest") || name.equalsIgnoreCase("Opened Reward Chest");
    }

    private static final class ChestWindowState {
        private boolean rerolled;
        private boolean shardRerolled;
        private boolean bought;
    }
}
