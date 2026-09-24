package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.skyblock.BackboneStateChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.BackboneAlertManager;
import org.jetbrains.annotations.NotNull;

public class BackboneWidget extends HudWidget {

    private final BackboneAlertManager manager = BackboneAlertManager.get();

    public BackboneWidget() {
        super(
                "backbone_alert",
                "Backbone Alert",
                307.0f,
                285.47922f,
                3.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseFourConfig.backboneAlert);
        setVisibilityCondition(manager::shouldDisplay);

        setExampleLines(HudLine.of("§8[§c||||||||§7||||||||||||§8] §b40%"));
    }

    @Override
    protected void onActivate() {
        subscribe(BackboneStateChangeEvent.class, this::onBackboneStateChange);
        refreshLines();
    }

    private void onBackboneStateChange(@NotNull BackboneStateChangeEvent event) {
        refreshLines();
    }

    private void refreshLines() {
        clearLines();
        if (manager.isRendActive()) {
            addLine(HudLine.of("§a§lREND NOW!"));
            return;
        }

        addLine(HudLine.of(manager.getProgressBar() + " " + manager.getPercentString()));
    }
}
