package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.iqaddons.mod.utils.hud.element.HudAnchor;
import net.iqaddons.mod.utils.hud.element.HudWidget;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays supply pickup times for all players during Kuudra Phase 1.
 *
 * <p>Shows:
 * <ul>
 *   <li>Player name and their pickup time</li>
 *   <li>Color-coded based on speed</li>
 *   <li>Running timer for current phase</li>
 * </ul>
 */
@Slf4j
public class SupplyTimerWidget extends HudWidget {

    private static final Pattern SUPPLY_PATTERN = Pattern.compile(
            "(.+) recovered one of Elle's supplies! \\((\\d)/6\\)"
    );

    private final Map<String, SupplyPickup> pickups = new ConcurrentHashMap<>();
    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private long phaseStartTime = 0;
    private EventBus.Subscription<ChatReceivedEvent> chatSubscription;

    public SupplyTimerWidget() {
        super(
                "supplyTimer",
                "Supply Timer",
                10.0f, 10.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseOneConfig.supplyTimers);
        setVisibilityCondition(() -> stateManager.phase().isInRun());

        setExampleLines(List.of(
                HudLine.of("§e§lSupply Times"),
                HudLine.of("§a§lPlayer1 §7- §b23.45s"),
                HudLine.of("§e§lPlayer2 §7- §b25.12s"),
                HudLine.of("§c§lPlayer3 §7- §b28.90s"),
                HudLine.of("§2§lPlayer4 §7- §b21.33s")
        ));
    }

    @Override
    protected void onActivate() {
        chatSubscription = EventBus.subscribe(ChatReceivedEvent.class, this::onChat);

        phaseStartTime = System.currentTimeMillis();
        pickups.clear();
        updateDisplay();

        log.info("Supply Timer Widget activated");
    }

    @Override
    protected void onDeactivate() {
        if (chatSubscription != null) {
            chatSubscription.unsubscribe();
            chatSubscription = null;
        }

        pickups.clear();
        phaseStartTime = 0;
        log.info("Supply Timer Widget deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        Matcher matcher = SUPPLY_PATTERN.matcher(message);
        if (matcher.find()) {
            String playerName = matcher.group(1);
            int supplyCount = Integer.parseInt(matcher.group(2));
            long pickupTime = System.currentTimeMillis() - phaseStartTime;

            pickups.put(playerName, new SupplyPickup(playerName, pickupTime, supplyCount));
            updateDisplay();
        }
    }

    private void updateDisplay() {
        clearLines();
        addLine(HudLine.of("§e§lSupply Times"));

        log.info("Updating Supply Timer Widget display");

        if (pickups.isEmpty() && phaseStartTime > 0) {
            long elapsed = System.currentTimeMillis() - phaseStartTime;
            addLine(HudLine.of("§7Waiting... §b" + formatTime(elapsed)));
            markDimensionsDirty();
            return;
        }

        pickups.values().stream()
                .sorted(Comparator.comparingLong(a -> a.timeMs))
                .forEach(pickup -> {
                    String colorCode = getTimeColor(pickup.timeMs);
                    String timeStr = formatTime(pickup.timeMs);

                    HudLine line = HudLine.of(String.format(
                            "%s%s §7- §b%s",
                            colorCode,
                            pickup.playerName,
                            timeStr
                    ));

                    addLine(line);
                });

        log.info("Updated Supply Timer Widget display with {} pickups", pickups.size());

        markDimensionsDirty();
    }

    private @NotNull String formatTime(long timeMs) {
        double seconds = timeMs / 1000.0;
        return String.format("%.2fs", seconds);
    }

    private @NotNull String getTimeColor(long timeMs) {
        double seconds = timeMs / 1000.0;

        if (seconds < 20) return "§a§l"; // Green - Excellent
        if (seconds < 24) return "§2§l"; // Dark Green - Good
        if (seconds < 26) return "§e§l"; // Yellow - Average
        if (seconds < 28) return "§6§l"; // Gold - Slow
        return "§c§l"; // Red - Very Slow
    }

    /**
     * Record to hold supply pickup data.
     */
    private record SupplyPickup(
            String playerName,
            long timeMs,
            int supplyNumber
    ) {}
}