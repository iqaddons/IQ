package net.iqaddons.mod.events.dispatcher.detector;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.SupplyPickupEvent;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.regex.Matcher;

import static net.iqaddons.mod.IQConstants.SUPPLY_DROPPED_PATTERN;
import static net.iqaddons.mod.IQConstants.SUPPLY_PATTERN;

public final class SupplyDetector {

    private final SupplyStateManager supplyStateManager;

    public SupplyDetector(@NotNull SupplyStateManager supplyStateManager) {
        this.supplyStateManager = supplyStateManager;
    }

    public void detect(@NotNull ChatReceivedEvent event, @NotNull String message, @NotNull Consumer<Event> postEvent) {
        Matcher supplyMatcher = SUPPLY_PATTERN.matcher(message);
        if (supplyMatcher.find()) {
            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);

            String supplyCount = supplyMatcher.group(2);
            String formattedMessage = TextFormatUtil.toLegacyString(event.getText());
            double timeSeconds = supplyStateManager.getElapsedTimeMillis() / 1000.0;

            postEvent.accept(new SupplyPickupEvent(
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
}
