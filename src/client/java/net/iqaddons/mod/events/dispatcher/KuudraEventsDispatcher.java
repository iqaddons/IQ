package net.iqaddons.mod.events.dispatcher;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.skyblock.*;
import net.iqaddons.mod.features.kuudra.tracker.KuudraProfitTrackerFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.profit.ChestType;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static net.iqaddons.mod.IQConstants.*;
import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerByName;

@Slf4j
public class KuudraEventsDispatcher extends EventDispatcher {

    private static final int BUY_SLOT = 31;
    private static final int REROLL_SLOT = 50;
    private static final int SHARD_REROLL_SLOT = 51;

    private final SupplyStateManager supplyStateManager = SupplyStateManager.get();

    private volatile boolean onSkyBlock = false;
    private volatile String currentArea = "";

    private final Map<Integer, ChestWindowState> windowStates = new ConcurrentHashMap<>();

    @Override
    public void start() {
        subscribe(ClientTickEvent.class, this::onClientTick);
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ScreenClickEvent.class, this::onScreenClick);
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(DEFAULT_CHECK_INTERVAL_TICKS)) {
            return;
        }

        boolean wasOnSkyBlock = onSkyBlock;
        onSkyBlock = ScoreboardUtils.hasTitle(SKYBLOCK_AREA_ID);
        if (wasOnSkyBlock != onSkyBlock) {
            EventBus.post(new SkyblockAreaChangeEvent(
                    onSkyBlock, currentArea,
                    onSkyBlock ? "joined" : "left")
            );

            log.info("SkyBlock status: {}", onSkyBlock ? "joined" : "left");
        }

        if (!onSkyBlock) {
            if (!currentArea.isEmpty()) {
                currentArea = "";
            }

            return;
        }

        String newArea = ScoreboardUtils.getArea();
        if (!newArea.equals(currentArea)) {
            String previousArea = currentArea;
            currentArea = newArea;

            EventBus.post(new SkyblockAreaChangeEvent(
                    onSkyBlock, previousArea, newArea));
            log.info("Area: {} -> {}", previousArea, newArea);
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!onSkyBlock) return;
        if (!isInArea(KUUDRA_AREA_ID)) return;

        String message = event.getStrippedMessage();
        performSupplyDetect(event, message);
        performFreshDetect(message, TextFormatUtil.toLegacyString(event.getText()));
    }

    private void onScreenClick(@NotNull ScreenClickEvent event) {
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

        if (slot.id == REROLL_SLOT && !state.rerolled && ChestProfitUtil.canUseReroll(slot.getStack(), "rerolled this chest")) {
            state.rerolled = true;
            EventBus.post(new KuudraChestRerollEvent(
                    windowId, KuudraChestRerollEvent.RerollType.ITEMS
            ));
            return;
        }

        if (slot.id == SHARD_REROLL_SLOT && !state.shardRerolled && ChestProfitUtil.canUseReroll(slot.getStack(), "rerolled this shard")) {
            state.shardRerolled = true;
            EventBus.post(new KuudraChestRerollEvent(
                    windowId, KuudraChestRerollEvent.RerollType.SHARD
            ));
            return;
        }

        if (slot.id != BUY_SLOT || state.bought) return;
        if (!isBuyAction(slot.getStack())) return;

        EventBus.post(new KuudraChestOpenEvent(
                windowId, title, handler.slots, chestType
        ));
    }

    private void performSupplyDetect(ChatReceivedEvent event, String message) {
        Matcher supplyMatcher = SUPPLY_PATTERN.matcher(message);
        if (supplyMatcher.find()) {
            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);

            String supplyCount = supplyMatcher.group(2);
            String formattedMessage = TextFormatUtil.toLegacyString(event.getText());
            double timeSeconds = supplyStateManager.getElapsedTimeMillis() / 1000.0;

            EventBus.post(new SupplyPickupEvent(
                    formattedMessage,
                    StringUtils.extractFormattedPlayerName(formattedMessage),
                    Integer.parseInt(supplyCount), timeSeconds
            ));
            return;
        }

        Matcher supplyDroppedMatcher = SUPPLY_DROPPED_PATTERN.matcher(message);
        if (supplyDroppedMatcher.find()) {
            String droppedBy = StringUtils.extractFormattedPlayerName(TextFormatUtil.toLegacyString(event.getText()));
            EventBus.post(new SupplyDropEvent(droppedBy, Integer.parseInt(supplyDroppedMatcher.group(2))));
        }
    }

    private void performFreshDetect(@NotNull String message, @NotNull String formattedMessage) {
        int buildProgress = getBuildingProgress();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            EventBus.post(new PlayerFreshEvent(
                    true, getPlayerNick(player),
                    player.getId(), buildProgress,
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            if (client.world == null) return;

            String playerName = StringUtils.extractFormattedPlayerName(formattedMessage);
            findPlayerByName(playerName).ifPresentOrElse(
                    player ->
                            EventBus.post(new PlayerFreshEvent(
                                    false, playerName, player.getId(),
                                    buildProgress, System.currentTimeMillis()
                            )),
                    () -> log.warn("Player '{}' not found in world", playerName)
            );
        }
    }

    private @NotNull String getPlayerNick(@NotNull ClientPlayerEntity player) {
        if (client.getNetworkHandler() == null) {
            return "§7" + player.getName().getString();
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null || entry.getDisplayName() == null) {
            return "§7" + player.getName().getString();
        }

        String displayName = TextFormatUtil.toLegacyString(entry.getDisplayName());
        return StringUtils.formatPlayerNick(displayName);
    }

    private boolean isInArea(@NotNull String areaName) {
        return currentArea.toLowerCase().contains(areaName.toLowerCase());
    }

    private boolean isBuyAction(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String name = StringUtils.stripFormatting(stack.getName().getString());
        return name.equalsIgnoreCase("Open Reward Chest") || name.equalsIgnoreCase("Opened Reward Chest");
    }

    private int getBuildingProgress() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = ScoreboardUtils.stripFormatting(line);
            Matcher matcher = PROTECT_ELLE_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse progress: {}", stripped);
                }
            }
        }
        return 0;
    }

    @Override
    public void stop() {}

    private static final class ChestWindowState {
        private boolean rerolled;
        private boolean shardRerolled;
        private boolean bought;
    }
}
