package net.iqaddons.mod.events.dispatcher;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.SupplyPickupEvent;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

import static net.iqaddons.mod.IQConstants.*;
import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerByName;

@Slf4j
public class KuudraEventsDispatcher extends EventDispatcher {

    private final SupplyStateManager supplyStateManager = SupplyStateManager.get();

    private volatile boolean onSkyBlock = false;
    private volatile String currentArea = "";

    @Override
    public void start() {
        subscribe(ClientTickEvent.class, this::onClientTick);
        subscribe(ChatReceivedEvent.class, this::onChat);
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
        performFreshDetect(message);
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
        }
    }

    private void performFreshDetect(@NotNull String message) {
        int buildProgress = getBuildingProgress();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            EventBus.post(new PlayerFreshEvent(
                    true, player.getName().getString(),
                    player.getId(), buildProgress,
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            if (client.world == null) return;

            String playerName = partyMatcher.group(1);
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

    private boolean isInArea(@NotNull String areaName) {
        return currentArea.toLowerCase().contains(areaName.toLowerCase());
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
}
