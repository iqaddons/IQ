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
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
        if (!(event.getScreen() instanceof ContainerScreen screen)) {
            return;
        }

        String title = screen.getTitle().getString();
        ChestType chestType = ChestType.fromString(title);
        if (chestType == ChestType.UNKNOWN) return;

        Slot slot = event.getSlot();
        if (slot == null || isOpened(slot.getItem())) return;

        AbstractContainerMenu handler = screen.getMenu();
        int windowId = handler.containerId;
        ChestWindowState state = windowStates.computeIfAbsent(windowId, key -> new ChestWindowState());
        state.lastInteractionTick = tickCount;

        // Minecraft recycles syncId values (~0-127). If this windowId was used by a previous
        // chest that was already fully processed (bought=true, pendingOpen=null), reset the
        // state so the new chest interaction is not silently ignored.
        if (state.pendingOpens.isEmpty()) {
            state.rerolled = false;
            state.shardRerolled = false;
        }

        if (slot.index == REROLL_SLOT && !state.rerolled && ChestProfitUtil.canUseReroll(slot.getItem(), "rerolled this chest")) {
            state.rerolled = true;
            postEvent.accept(new KuudraChestRerollEvent(windowId, KuudraChestRerollEvent.RerollType.ITEMS));
            return;
        }

        if (slot.index == SHARD_REROLL_SLOT && !state.shardRerolled && ChestProfitUtil.canUseReroll(slot.getItem(), "rerolled this shard")) {
            state.shardRerolled = true;
            postEvent.accept(new KuudraChestRerollEvent(
                    windowId,
                    KuudraChestRerollEvent.RerollType.SHARD)
            );
            return;
        }

        if (slot.index != BUY_SLOT) return;
        // Allow empty slot: on fast clicks the server slot-update packet may not have arrived
        // yet, so the stack appears empty even though this is a valid buy action.
        // Only reject if the item is present but explicitly not a buy action.
        if (!slot.getItem().isEmpty() && !isBuyAction(slot.getItem())) return;

        state.pendingOpens.addLast(new PendingChestOpen(
                windowId,
                title,
                new ArrayList<>(handler.slots),
                chestType
        ));
    }

    public void detect(@NotNull ChatReceivedEvent event, long tickCount, @NotNull Consumer<Event> postEvent) {
        String message = event.getStrippedMessage();
        if (!message.contains(PAID_CHEST_REWARDS_MESSAGE) && !message.contains(FREE_CHEST_REWARDS_MESSAGE)) return;

        windowStates.values().stream()
                .filter(state -> !state.pendingOpens.isEmpty())
                .max(Comparator.comparingLong(left -> left.lastInteractionTick))
                .ifPresent(state -> {
                    state.lastInteractionTick = tickCount;
                    PendingChestOpen pendingOpen = state.pendingOpens.pollFirst();
                    if (pendingOpen == null) return;
                    postEvent.accept(new KuudraChestOpenEvent(
                            pendingOpen.windowId(),
                            pendingOpen.title(),
                            pendingOpen.slots(),
                            pendingOpen.chestType()
                    ));
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
        // volatile: fields are written on the Minecraft main thread (ScreenClickEvent) but
        // read on the Netty I/O thread (ChatReceivedEvent). Without volatile the JVM gives
        // no visibility guarantee, causing the Netty thread to see stale null values for
        // pendingOpen and silently drop the KuudraChestOpenEvent.
        private volatile boolean rerolled;
        private volatile boolean shardRerolled;
        private final Deque<PendingChestOpen> pendingOpens = new ConcurrentLinkedDeque<>();
        private volatile long lastInteractionTick;
    }

    private record PendingChestOpen(
            int windowId,
            @NotNull String title,
            @NotNull List<Slot> slots,
            @NotNull ChestType chestType
    ) {}
}
