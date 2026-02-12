package net.iqaddons.mod.events.dispatcher.detector;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.regex.Matcher;

import static net.iqaddons.mod.IQConstants.*;

public final class SupplyDetector {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final SupplyStateManager supplyStateManager;

    public SupplyDetector(@NotNull SupplyStateManager supplyStateManager) {
        this.supplyStateManager = supplyStateManager;
    }

    public void detect(@NotNull ChatReceivedEvent event, @NotNull String message, @NotNull Consumer<Event> postEvent) {
        if (message.contains(SUPPLY_PICKUP_MESSAGE)) {
            var player = client.player;
            if (player == null) return;

            var playerPos = player.getEntityPos();
            if (playerPos == null) return;

            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);
            postEvent.accept(new SupplyPickupEvent(
                    PreSpot.fromPlayerPosition(playerPos),
                    supplyStateManager.findSupplyNear(playerPos, 3),
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher supplyMatcher = SUPPLY_PLACE_PATTERN.matcher(message);
        if (supplyMatcher.find()) {
            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);

            String supplyCount = supplyMatcher.group(2);
            String formattedMessage = TextFormatUtil.toLegacyString(event.getText());
            double timeSeconds = supplyStateManager.getElapsedTimeMillis() / 1000.0;

            postEvent.accept(new SupplyPlaceEvent(
                    formattedMessage,
                    StringUtils.extractFormattedPlayerName(formattedMessage),
                    Integer.parseInt(supplyCount),
                    timeSeconds
            ));
            return;
        }

        Matcher supplyDroppedMatcher = SUPPLY_DROPPED_PATTERN.matcher(message);
        if (supplyDroppedMatcher.find()) {
            String droppedBy = StringUtils.extractFormattedPlayerName(TextFormatUtil.toLegacyString(event.getText()));
            postEvent.accept(new SupplyDropEvent(droppedBy, Integer.parseInt(supplyDroppedMatcher.group(2))));
        }
    }

    public void detectProgress(@NotNull TitleReceivedEvent event, Consumer<Event> postEvent) {
        String title = TextFormatUtil.toLegacyString(event.getTitle());
        Matcher progressMatcher = SUPPLY_PROGRESS_PATTERN.matcher(title);
        if (progressMatcher.find()) {
            int progress = Integer.parseInt(progressMatcher.group(1));

            boolean updated = supplyStateManager.setSupplyProgress(progress);
            if (updated) {
                var player = client.player;
                if (player == null) return;

                var playerPos = player.getEntityPos();
                if (playerPos == null) return;

                var progressEvent = new SupplyProgressEvent(
                        supplyStateManager.findSupplyNear(playerPos, 3),
                        PreSpot.fromPlayerPosition(playerPos),
                        event.getMessage(),
                        progress
                );

                postEvent.accept(progressEvent);
                if (progressEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
