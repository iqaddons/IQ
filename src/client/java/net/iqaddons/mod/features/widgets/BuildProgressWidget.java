package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.BuildProgressOverlayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BuildProgressWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final HudLine titleLine;
    private final HudLine progressLine;
    private final HudLine buildersLine;
    private final HudLine freshLine;

    private int currentProgress = 0;
    private int builderCount = 0;
    private int freshCount = 0;
    private long countdownEndMillis = -1L;

    public BuildProgressWidget() {
        super(
                "buildProgress",
                "Build Progress",
                430.0f, 385.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§6§lBuild Progress")
                .showWhen(() -> stateManager.phase() == KuudraPhase.BUILD || hasActiveCountdown());
        progressLine = HudLine.of("§fProgress: §c0%")
                .showWhen(() -> stateManager.phase() == KuudraPhase.BUILD || hasActiveCountdown());
        buildersLine = HudLine.of("§fBuilders: §e0").showWhen(() -> !hasActiveCountdown() && builderCount > 0);
        freshLine = HudLine.of("§fFresh: §b0").showWhen(() -> !hasActiveCountdown() && freshCount > 0);

        setEnabledSupplier(BuildProgressOverlayUtil::isClassicOverlayEnabled);
        setVisibilityCondition(() -> {
            KuudraPhase phase = stateManager.phase();
            if (phase == KuudraPhase.BUILD) return true;
            return phase == KuudraPhase.SUPPLIES && BuildProgressOverlayUtil.isBuildStartCountdownEnabled();
        });

        setExampleLines(List.of(
                HudLine.of("§6§lBuild Progress"),
                HudLine.of("§fProgress: §e75%"),
                HudLine.of("§fBuilders: §e2"),
                HudLine.of("§fFresh: §b3")
        ));
    }

    @Override
    protected void onActivate() {
        currentProgress = 0;
        builderCount = 0;
        freshCount = 0;
        countdownEndMillis = -1L;

        clearLines();
        addLines(titleLine, progressLine, buildersLine, freshLine);

        updateDisplay();

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
    }

    @Override
    protected void onDeactivate() {
        currentProgress = 0;
        builderCount = 0;
        freshCount = 0;
        countdownEndMillis = -1L;
        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        if (hasActiveCountdown()) {
            updateDisplay();
            return;
        }

        if (countdownEndMillis > 0) {
            countdownEndMillis = -1L;
            updateDisplay();
        }

        if (stateManager.phase() != KuudraPhase.BUILD) return;

        BuildProgressOverlayUtil.BuildProgressData data = BuildProgressOverlayUtil.getBuildProgressFromArmorStand();
        if (data == null) return;

        if (data.progress() == currentProgress && data.builders() == builderCount) return;

        currentProgress = data.progress();
        builderCount = data.builders();
        updateDisplay();
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        if (stateManager.phase() != KuudraPhase.BUILD) return;

        freshCount++;
        updateDisplay();
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (!PhaseTwoConfig.buildStartCountdownOverlay) return;
        if (event.currentSupply() < 6) return;

        countdownEndMillis = System.currentTimeMillis() + BuildProgressOverlayUtil.BUILD_START_COUNTDOWN_MS;
        updateDisplay();
    }

    private void updateDisplay() {
        if (hasActiveCountdown()) {
            long remainingMs = Math.max(0L, countdownEndMillis - System.currentTimeMillis());
            String color = BuildProgressOverlayUtil.getCountdownColor(remainingMs);
            if (color == null) color = "§c";

            progressLine.text(String.format("§fStarting in: %s%ss", color, BuildProgressOverlayUtil.formatCountdownSeconds(remainingMs)));
            markDimensionsDirty();
            return;
        }

        progressLine.text(String.format("§fProgress: %s%d%%", getProgressColor(currentProgress), currentProgress));
        buildersLine.text(String.format("§fBuilders: §e%d", builderCount));
        freshLine.text(String.format("§fFresh: §b%d", freshCount));
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