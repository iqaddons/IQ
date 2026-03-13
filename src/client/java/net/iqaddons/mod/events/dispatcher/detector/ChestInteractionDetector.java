package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestOpenEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestRerollEvent;
import net.iqaddons.mod.model.profit.chest.type.ChestType;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public final class ChestInteractionDetector {

    private static final int BUY_SLOT = 31;
    private static final int REROLL_SLOT = 50;
    private static final int SHARD_REROLL_SLOT = 51;
    private static final long WINDOW_STATE_TTL_TICKS = 20L * 60L * 5L;

    private static final String PAID_CHEST_REWARDS_MESSAGE = "PAID CHEST REWARDS";
    private static final String FREE_CHEST_REWARDS_MESSAGE = "FREE CHEST REWARDS";

    private final Map<Integer, ChestWindowState> windowStates = new ConcurrentHashMap<>();

    public void detect(@NotNull ScreenClickEvent event, long tickCount, @NotNull Consumer<Event> postEvent) {
        if (!(event.getScreen() instanceof GenericContainerScreen screen)) {
            return;
        }

        String title = screen.getTitle().getString();
        ChestType chestType = ChestType.fromString(title);
        if (chestType == ChestType.UNKNOWN) return;

        Slot slot = event.getSlot();
        if (slot == null || isOpened(slot.getStack())) return;

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
            postEvent.accept(new KuudraChestRerollEvent(
                    windowId,
                    KuudraChestRerollEvent.RerollType.SHARD)
            );
            return;
        }

        if (slot.id != BUY_SLOT || state.bought) return;
        if (!isBuyAction(slot.getStack())) return;

        state.bought = true;
        state.pendingOpen = new PendingChestOpen(
                windowId,
                title,
                new ArrayList<>(handler.slots),
                chestType
        );
    }

    public void detect(@NotNull ChatReceivedEvent event, long tickCount, @NotNull Consumer<Event> postEvent) {
        String message = event.getStrippedMessage();
        if (!message.contains(PAID_CHEST_REWARDS_MESSAGE) && !message.contains(FREE_CHEST_REWARDS_MESSAGE)) return;

        windowStates.values().stream()
                .filter(state -> state.pendingOpen != null)
                .max(Comparator.comparingLong(left -> left.lastInteractionTick))
                .ifPresent(state -> {
                    state.lastInteractionTick = tickCount;
                    postEvent.accept(new KuudraChestOpenEvent(
                            state.pendingOpen.windowId(),
                            state.pendingOpen.title(),
                            state.pendingOpen.slots(),
                            state.pendingOpen.chestType()
                    ));
                    state.pendingOpen = null;
                });
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

        return ChestProfitUtil.getLoreLines(stack)
                .stream()
                .map(StringUtils::stripFormatting)
                .anyMatch(str -> str.contains("Click to open!"));
    }

    private boolean isOpened(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return ChestProfitUtil.getLoreLines(stack)
                .stream()
                .map(StringUtils::stripFormatting)
                .anyMatch(str -> str.contains("Already opened!")
                        || str.contains("Chest already opened!")
                        || str.contains("You have already opened a chest!")
                );
    }

    private static final class ChestWindowState {
        private boolean rerolled;
        private boolean shardRerolled;
        private boolean bought;
        private PendingChestOpen pendingOpen;
        private long lastInteractionTick;
    }

    private record PendingChestOpen(
            int windowId,
            @NotNull String title,
            @NotNull List<Slot> slots,
            @NotNull ChestType chestType
    ) {}
}
