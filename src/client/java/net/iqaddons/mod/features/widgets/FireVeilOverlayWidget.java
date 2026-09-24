package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.FireVeilOverlayManager;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class FireVeilOverlayWidget extends HudWidget {

    private final FireVeilOverlayManager manager = FireVeilOverlayManager.get();
    private final HudLine countdownLine;

    public FireVeilOverlayWidget() {
        super(
                "fireVeilOverlay",
                "Fire Veil Overlay",
                424.5f, 278.58417f,
                1.2f,
                HudAnchor.TOP_LEFT
        );

        countdownLine = HudLine.of("§b§lFire Veil: §a§l5.00s")
                .showWhen(this::hasVisibleCountdown);

        setEnabledSupplier(() -> PhaseTwoConfig.fireVeilOverlay && PhaseTwoConfig.FireVeilOverlayConfig.abilityCountdown);
        setVisibilityCondition(this::hasVisibleCountdown);
        setExampleLines(HudLine.of("§b§lFire Veil: §e§l4.83s"));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLine(countdownLine);
        subscribe(ClientTickEvent.class, this::onTick);
        updateCountdownLine();
    }

    @Override
    protected void onDeactivate() {
        countdownLine.text("");
        markDimensionsDirty();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        updateCountdownLine();
    }

    private void updateCountdownLine() {
        long now = System.currentTimeMillis();
        if (manager.isReadyVisible(now)) {
            countdownLine.text("§b§lFire Veil: §a§lREADY");
            markDimensionsDirty();
            return;
        }

        long remainingMs = manager.getRemainingMs(now);
        countdownLine.text(String.format(
                Locale.ROOT,
                "§b§lFire Veil: %s§l%.2fs",
                getCountdownColor(remainingMs),
                remainingMs / 1000.0
        ));
        markDimensionsDirty();
    }

    private boolean hasVisibleCountdown() {
        return manager.shouldDisplayCountdown(System.currentTimeMillis());
    }

    private @NotNull String getCountdownColor(long remainingMs) {
        double ratio = Math.min(1.0, Math.max(0.0, remainingMs / (double) FireVeilOverlayManager.ABILITY_DURATION_MS));
        if (ratio > 0.75) return "§a";
        if (ratio > 0.50) return "§e";
        if (ratio > 0.25) return "§6";
        return "§c";
    }
}
