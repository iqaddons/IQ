package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraDirectionChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraDirectionWidget extends HudWidget {

    private static final int DISPLAY_TICKS = 80;

    private int directionTicksRemaining = 0;
    private final HudLine directionLine = HudLine.of("§a§lFRONT")
            .showWhen(() -> directionTicksRemaining > 0);

    public KuudraDirectionWidget() {
        super("kuudra_direction",
                "Kuudra Direction",
                -4f,
                -60f,
                3.0f,
                HudAnchor.CENTER
        );

        setEnabledSupplier(() -> PhaseFourConfig.kuudraDirectionAlert);
        setVisibilityCondition(() -> {
            var phase = KuudraStateManager.get().phase();
            return phase == KuudraPhase.SKIP || phase == KuudraPhase.BOSS;
        });

        setExampleLines(HudLine.of("§a§lFRONT"));
    }

    @Override
    protected void onActivate() {
        directionTicksRemaining = 0;

        clearLines();
        addLines(directionLine);

        subscribe(KuudraDirectionChangeEvent.class, this::onDirectionChange);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        directionTicksRemaining = 0;
    }

    private void onDirectionChange(@NotNull KuudraDirectionChangeEvent event) {
        var newDirection = event.newDirection();

        directionLine.text(newDirection.getFormattedName());
        directionTicksRemaining = DISPLAY_TICKS;
        markDimensionsDirty();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        if (directionTicksRemaining > 0) {
            directionTicksRemaining--;
            if (directionTicksRemaining == 0) {
                markDimensionsDirty();
            }
        }
    }
}
