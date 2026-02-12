package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestOpenEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestRerollEvent;
import net.iqaddons.mod.model.profit.chest.ChestType;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public final class ChestInteractionDetector {

    private static final int BUY_SLOT = 31;
    private static final int REROLL_SLOT = 50;
    private static final int SHARD_REROLL_SLOT = 51;
    private static final long WINDOW_STATE_TTL_TICKS = 20L * 60L * 5L;

    private final Map<Integer, ChestWindowState> windowStates = new ConcurrentHashMap<>();

    public void detect(@NotNull ScreenClickEvent event, long tickCount, @NotNull Consumer<Event> postEvent) {
        if (!(event.getScreen() instanceof GenericContainerScreen screen)) {
            return;
        }

        String title = screen.getTitle().getString();
        ChestType chestType = ChestType.fromString(title);
        if (chestType == ChestType.UNKNOWN) return;

        Slot slot = event.getSlot();
        if (slot == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        int windowId = handler.syncId;
        ChestWindowState state = windowStates.computeIfAbsent(windowId, key -> new ChestWindowState());
        state.lastInteractionTick = tickCount;

        if (slot.id == REROLL_SLOT && !state.rerolled && ChestProfitUtil.canUseReroll(slot.getStack(), "rerolled this chest")) {
            state.rerolled = true;
            postEvent.accept(new KuudraChestRerollEvent(windowId, KuudraChestRerollEvent.RerollType.ITEMS));
            return;
        }

        if (slot.id == SHARD_REROLL_SLOT && !state.shardRerolled && ChestProfitUtil.canUseReroll(slot.getStack(), "rerolled this shard")) {
            state.shardRerolled = true;
            postEvent.accept(new KuudraChestRerollEvent(windowId, KuudraChestRerollEvent.RerollType.SHARD));
            return;
        }

        if (slot.id != BUY_SLOT || state.bought) return;
        if (!isBuyAction(slot.getStack())) return;

        state.bought = true;
        postEvent.accept(new KuudraChestOpenEvent(windowId, title, handler.slots, chestType));
    }

    public void evictExpired(long tickCount) {
        int before = windowStates.size();
        windowStates.entrySet().removeIf(entry -> tickCount - entry.getValue().lastInteractionTick > WINDOW_STATE_TTL_TICKS);
        int removed = before - windowStates.size();
        if (removed > 0) {
            log.debug("Evicted {} stale chest interaction states", removed);
        }
    }

    private boolean isBuyAction(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String name = StringUtils.stripFormatting(stack.getName().getString());
        return name.equalsIgnoreCase("Open Reward Chest") || name.equalsIgnoreCase("Opened Reward Chest");
    }

    private static final class ChestWindowState {
        private boolean rerolled;
        private boolean shardRerolled;
        private boolean bought;
        private long lastInteractionTick;
    }
}
