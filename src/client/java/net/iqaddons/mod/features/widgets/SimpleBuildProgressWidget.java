package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.BuildProgressOverlayUtil;
import org.jetbrains.annotations.NotNull;

public class SimpleBuildProgressWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine buildLine;

    private int currentProgress = 0;
    private long countdownEndMillis = -1L;

    public SimpleBuildProgressWidget() {
        super(
                "simpleBuildProgress",
                "Simple Build Progress",
                200.0f, 100.0f,
                1.2f,
                HudAnchor.TOP_LEFT
        );

        buildLine = HudLine.of("§b§lBuild: 0%")
                .showWhen(() -> stateManager.phase() == KuudraPhase.BUILD || hasActiveCountdown());

        setEnabledSupplier(BuildProgressOverlayUtil::isSimpleOverlayEnabled);
        setVisibilityCondition(() -> {
            KuudraPhase phase = stateManager.phase();
            if (phase == KuudraPhase.BUILD) return true;
            return phase == KuudraPhase.SUPPLIES && BuildProgressOverlayUtil.isBuildStartCountdownEnabled();
        });

        setExampleLines(HudLine.of("§b§lBuild: §e§l50%"));
    }

    @Override
    protected void onActivate() {
        currentProgress = 0;
        countdownEndMillis = -1L;
        updateBuildLine();

        clearLines();
        addLine(buildLine);

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
    }

    @Override
    protected void onDeactivate() {
        currentProgress = 0;
        countdownEndMillis = -1L;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        if (hasActiveCountdown()) {
            updateBuildLine();
            return;
        }

        if (countdownEndMillis > 0) {
            countdownEndMillis = -1L;
            updateBuildLine();
        }

        if (stateManager.phase() != KuudraPhase.BUILD) return;

        BuildProgressOverlayUtil.BuildProgressData data = BuildProgressOverlayUtil.getBuildProgressFromArmorStand();
        if (data == null) return;
        if (data.progress() == currentProgress) return;

        currentProgress = data.progress();
        updateBuildLine();
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (!PhaseTwoConfig.buildStartCountdownOverlay) return;
        if (event.currentSupply() < 6) return;

        countdownEndMillis = System.currentTimeMillis() + BuildProgressOverlayUtil.BUILD_START_COUNTDOWN_MS;
        updateBuildLine();
    }

    private void updateBuildLine() {
        if (hasActiveCountdown()) {
            long remainingMs = Math.max(0L, countdownEndMillis - System.currentTimeMillis());
            String color = BuildProgressOverlayUtil.getCountdownColor(remainingMs);
            if (color == null) color = "§c";

            buildLine.text(String.format("§b§lBuild: %s§l%ss", color, BuildProgressOverlayUtil.formatCountdownSeconds(remainingMs)));
            markDimensionsDirty();
            return;
        }

        buildLine.text(String.format("§b§lBuild: %s§l%d%%", getProgressColor(currentProgress), currentProgress));
        markDimensionsDirty();
    }

    private boolean hasActiveCountdown() {
        if (!PhaseTwoConfig.buildStartCountdownOverlay) return false;
        return countdownEndMillis > System.currentTimeMillis();
    }

    private @NotNull String getProgressColor(int progress) {
        if (progress >= 80) return "§a";
        if (progress >= 60) return "§2";
        if (progress >= 40) return "§e";
        if (progress >= 20) return "§6";
        return "§c";
    }
}
