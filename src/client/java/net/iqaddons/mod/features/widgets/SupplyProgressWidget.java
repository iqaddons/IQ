package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SupplyProgressWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine progressLine;

    private String currentProgress = "";

    public SupplyProgressWidget() {
        super(
                "supplyProgress",
                "Supply Progress",
                430.0f, 360.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        progressLine = HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r")
                .showWhen(() -> !currentProgress.isEmpty());

        setEnabledSupplier(() -> PhaseOneConfig.supplyProgressDisplay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.SUPPLIES);

        setExampleLines(HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r"));
    }

    @Override
    protected void onActivate() {
        currentProgress = "";
        progressLine.text("");

        clearLines();
        addLine(progressLine);

        subscribe(SupplyPickupEvent.class, event -> clearProgress());
        subscribe(SupplyDropEvent.class, event -> clearProgress());
        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);

        subscribe(ClientTickEvent.class, event -> {
            if (currentProgress.isEmpty()) return;
            if (!event.isNthTick(40)) return;

            clearProgress();
        });
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        currentProgress = event.getProgressText();
        progressLine.text(currentProgress);
        markDimensionsDirty();

        if (event.getCurrentProgress() == 100) {
            clearProgress();
            MessageUtil.showAlert("§a§lSUPPLY PICKED UP!", 40);
        }

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