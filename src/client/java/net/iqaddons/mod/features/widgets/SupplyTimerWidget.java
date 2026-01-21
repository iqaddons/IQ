package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.skyblock.SupplyPickupEvent;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.iqaddons.mod.utils.hud.element.HudAnchor;
import net.iqaddons.mod.utils.hud.element.HudWidget;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.SupplyStateManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Slf4j
public class SupplyTimerWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private final List<SupplyPickupEntry> pickupHistory = Collections.synchronizedList(new ArrayList<>());

    private EventBus.Subscription<SupplyPickupEvent> supplyPickupSubscription;

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
                HudLine.of("§e§lSupply Times §7[§a3§7/§a6§7]"),
                HudLine.of("§fdarkjota §8(1/6) §e14.85s"),
                HudLine.of("§fPeHenrii §8(2/6) §a15.23s"),
                HudLine.of("§fckac10 §8(3/6) §b15.39s")
        ));
    }

    @Override
    protected void onActivate() {
        if (supplyState.getSuppliesPhaseStart() == null) {
            supplyState.startSuppliesPhase();
        }

        supplyPickupSubscription = EventBus.subscribe(SupplyPickupEvent.class, this::onSupplyPickup);

        resetLocalState();
        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        if (supplyPickupSubscription != null) {
            supplyPickupSubscription.unsubscribe();
            supplyPickupSubscription = null;
        }

        resetLocalState();
    }

    private void resetLocalState() {
        pickupHistory.clear();
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        pickupHistory.add(new SupplyPickupEntry(
                event.playerName(),
                supplyState.getTimeColor(),
                event.currentSupply(),
                event.pickupAt()
        ));

        updateDisplay();
    }

    private void updateDisplay() {
        clearLines();

        int totalCollected = supplyState.getSuppliesCollected();
        if (totalCollected == 0 && !pickupHistory.isEmpty()) {
            totalCollected = pickupHistory.size();
        }

        addLine(HudLine.of(String.format(
                "§e§lSupply Times §7[%s%d§7/§a6§7]",
                totalCollected >= 6 ? "§a" : "§e",
                totalCollected
        )));

        if (pickupHistory.isEmpty()) {
            long elapsed = supplyState.getElapsedTimeMillis();
            if (elapsed > 0) {
                addLine(HudLine.of(String.format("§7Waiting... %.2fs", elapsed / 1000.0)));
            }

            markDimensionsDirty();
            return;
        }

        for (SupplyPickupEntry entry : pickupHistory) {
            addLine(HudLine.of(String.format(
                    "§f%s §8(%d/6) %s%.2fs",
                    entry.playerName(), entry.supplyNumber(),
                    entry.color, entry.pickupAt()
            )));
        }

        markDimensionsDirty();
    }

    private record SupplyPickupEntry(
            String playerName,
            String color,
            int supplyNumber,
            double pickupAt
    ) {
    }
}