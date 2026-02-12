package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SupplyTimerWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private final List<SupplyPickupEntry> pickupHistory = Collections.synchronizedList(new ArrayList<>());

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
                HudLine.of("§bdarkjota §8(1/6) §f§l14.85s"),
                HudLine.of("§aPeHenrii §8(2/6) §f§l15.23s"),
                HudLine.of("§bckac10 §8(3/6) §f§l15.39s"),
                HudLine.of("§amennytb §8(4/6) §f§l16.04s")
        ));
    }

    @Override
    protected void onActivate() {
        if (supplyState.getSuppliesPhaseStart() == null) {
            supplyState.startSuppliesPhase();
        }

        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);

        resetLocalState();
        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        resetLocalState();
    }

    private void resetLocalState() {
        pickupHistory.clear();
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        pickupHistory.add(new SupplyPickupEntry(
                event.playerName(),
                supplyState.getTimeColor(),
                event.currentSupply(),
                event.placedAt()
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
                addLine(HudLine.of("§7No placed supplies yet..."));
            }

            markDimensionsDirty();
            return;
        }

        for (SupplyPickupEntry entry : pickupHistory) {
            addLine(HudLine.of(String.format(
                    "%s §8(%d/6) %s%.2fs",
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