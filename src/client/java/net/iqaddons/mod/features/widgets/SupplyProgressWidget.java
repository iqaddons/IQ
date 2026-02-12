package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SupplyProgressWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine progressLine;

    private String currentProgress = "";

    public SupplyProgressWidget() {
        super(
                "supplyProgress",
                "Supply Progress",
                10.0f, 120.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        progressLine = HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r")
                .showWhen(() -> !currentProgress.isEmpty());

        setEnabledSupplier(() -> PhaseOneConfig.supplyProgressDisplay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.SUPPLIES);

        setExampleLines(List.of(progressLine));
    }

    @Override
    protected void onActivate() {
        currentProgress = "";
        progressLine.text("");

        clearLines();
        addLine(progressLine);

        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);
        subscribe(SupplyPickupEvent.class, event -> clearProgress());
        subscribe(SupplyDropEvent.class, event -> clearProgress());
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        progressLine.text(event.getProgressText());
        markDimensionsDirty();

        event.setCancelled(true);
    }

    private void clearProgress() {
        currentProgress = "";
        progressLine.text("");
        markDimensionsDirty();
    }

    @Override
    protected void onDeactivate() {
        clearProgress();
    }
}