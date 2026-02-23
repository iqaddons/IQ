package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraDirectionChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraDirectionWidget extends HudWidget {

    private final HudLine directionLine = HudLine.of("§a§lFRONT");

    public KuudraDirectionWidget() {
        super("kuudra_direction",
                "Kuudra Direction",
                0f,
                -80f,
                1.0f,
                HudAnchor.CENTER
        );

        setEnabledSupplier(() -> PhaseFourConfig.kuudraDirectionAlert);
        setVisibilityCondition(() -> {
            var phase = KuudraStateManager.get().phase();
            return phase == KuudraPhase.BOSS;
        });

        setExampleLines(HudLine.of("§a§lFRONT"));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLines(directionLine);

        subscribe(KuudraDirectionChangeEvent.class, this::onDirectionChange);
    }

    private void onDirectionChange(@NotNull KuudraDirectionChangeEvent event) {
        var newDirection = event.newDirection();

        directionLine.text(newDirection.getFormattedName());
        markDimensionsDirty();
    }
}
