package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.TimeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class CustomSplitsWidget extends HudWidget {

    private static final Map<KuudraPhase, double[]> PHASE_THRESHOLDS = Map.of(
            KuudraPhase.SUPPLIES, new double[]{22.5, 24.7, 26.5, 28.0, 30.0},
            KuudraPhase.BUILD, new double[]{13.5, 15.0, 17.0, 19.0, 20.0},
            KuudraPhase.EATEN, new double[]{4.0, 5.3, 5.7, 6.0, 7.0},
            KuudraPhase.STUN, new double[]{0.0, 0.1, 0.3, 0.8, 1.0},
            KuudraPhase.DPS, new double[]{3.0, 3.6, 3.8, 4.2, 4.5},
            KuudraPhase.BOSS, new double[]{3.0, 4.2, 4.6, 5.0, 5.4}
    );

    private static final double[] OVERALL_THRESHOLDS = {53.0, 59.49, 65.0, 70.0, 80.0};

    private static final Map<KuudraPhase, Double> PACE_BENCHMARKS = Map.of(
            KuudraPhase.SUPPLIES, 23.5,
            KuudraPhase.BUILD, 14.3,
            KuudraPhase.EATEN, 5.0,
            KuudraPhase.STUN, 0.0,
            KuudraPhase.DPS, 3.5,
            KuudraPhase.BOSS, 4.5
    );

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final Map<KuudraPhase, Double> splits = new EnumMap<>(KuudraPhase.class);

    private final HudLine titleLine;
    private final HudLine suppliesLine;
    private final HudLine buildLine;
    private final HudLine eatenLine;
    private final HudLine stunLine;
    private final HudLine dpsLine;
    private final HudLine bossLine;
    private final HudLine overallLine;
    private final HudLine paceLine;

    public CustomSplitsWidget() {
        super(
                "customSplits",
                "Custom Splits",
                10.0f, 150.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§b§lKuudra Splits");
        suppliesLine = HudLine.of("§fSupplies §f0.00s");
        buildLine = HudLine.of("§fBuild: §f0.00s");
        eatenLine = HudLine.of("§fEaten: §f0.00s");
        stunLine = HudLine.of("§fStun: §f0.00s");
        dpsLine = HudLine.of("§fDPS: §f0.00s");
        bossLine = HudLine.of("§fBoss: §f0.00s");
        overallLine = HudLine.of("§fOverall: §f0.00s");
        paceLine = HudLine.of("§fPace: §f0.00s");

        setEnabledSupplier(() -> Configuration.customSplits);
        setVisibilityCondition(stateManager::isInKuudra);

        setExampleLines(List.of(
                HudLine.of("§b§lKuudra Splits"),
                HudLine.of("§fSupplies: §f22.45s"),
                HudLine.of("§fBuild: §914.32s"),
                HudLine.of("§fEaten: §a5.21s"),
                HudLine.of("§fStun: §f0.53s"),
                HudLine.of("§fDPS: §63.89s"),
                HudLine.of("§fBoss: §c5.12s"),
                HudLine.of("§fOverall: §a51.00s"),
                HudLine.of("§fPace: §953.50s")
        ));
    }

    @Override
    protected void onActivate() {
        resetSplits();
        loadExistingSplits();

        clearLines();
        addLines(
                titleLine, suppliesLine, buildLine,
                eatenLine, stunLine, dpsLine, bossLine,
                overallLine, paceLine
        );

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);

        updateDisplay();
        log.info("Custom Splits Widget activated");
    }

    private void loadExistingSplits() {
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            stateManager.getPhaseDuration(phase).ifPresent(duration -> {
                double seconds = duration.toMillis() / 1000.0;
                splits.put(phase, seconds);
            });
        }
    }

    private void resetSplits() {
        splits.clear();
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            splits.put(phase, 0.0);
        }
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            resetSplits();
            updateDisplay();
            return;
        }

        KuudraPhase completedPhase = event.previousPhase();
        if (completedPhase.isActive() && event.phaseDurationMillis() > 0) {
            double seconds = event.phaseDurationMillis() / 1000.0;
            splits.put(completedPhase, seconds);
        }

        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        KuudraPhase currentPhase = stateManager.phase();
        if (currentPhase == KuudraPhase.NONE || currentPhase == KuudraPhase.COMPLETED) {
            return;
        }

        stateManager.currentPhaseDuration().ifPresent(duration -> {
            double seconds = duration.toMillis() / 1000.0;
            splits.put(currentPhase, seconds);
        });

        updateDisplay();
    }

    private void updateDisplay() {
        updatePhaseLine(suppliesLine, "Supplies:", KuudraPhase.SUPPLIES);
        updatePhaseLine(buildLine, "Build:", KuudraPhase.BUILD);
        updatePhaseLine(eatenLine, "Eaten:", KuudraPhase.EATEN);
        updatePhaseLine(stunLine, "Stun:", KuudraPhase.STUN);
        updatePhaseLine(dpsLine, "DPS:", KuudraPhase.DPS);
        updatePhaseLine(bossLine, "Boss:", KuudraPhase.BOSS);

        double overall = calculateOverall();
        double pace = calculatePace();

        String overallColor = getSplitColor(overall, OVERALL_THRESHOLDS);
        String paceColor = getSplitColor(pace, OVERALL_THRESHOLDS);

        overallLine.text(String.format("§fOverall: %s%s", overallColor, TimeUtils.formatTime(overall)));
        paceLine.text(String.format("§fPace: %s%s", paceColor, TimeUtils.formatTime(pace)));

        markDimensionsDirty();
    }

    private void updatePhaseLine(@NotNull HudLine line, @NotNull String label, @NotNull KuudraPhase phase) {
        double time = splits.getOrDefault(phase, 0.0);
        String color = getSplitColor(time, phase);
        line.text(String.format("§f%s %s%s", label, color, TimeUtils.formatTime(time)));
    }

    private double calculateOverall() {
        return splits.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private double calculatePace() {
        double pace = 0.0;
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            double currentTime = splits.getOrDefault(phase, 0.0);
            double benchmark = PACE_BENCHMARKS.getOrDefault(phase, 0.0);

            pace += Math.max(currentTime, benchmark);
        }

        return pace;
    }

    private @NotNull String getSplitColor(double time, @NotNull KuudraPhase phase) {
        double[] thresholds = PHASE_THRESHOLDS.get(phase);
        if (thresholds == null) return "§d";

        return getSplitColor(time, thresholds);
    }

    private @NotNull String getSplitColor(double time, double @NotNull [] thresholds) {
        if (time <= 0) return "§f";

        if (time <= thresholds[0]) return "§f";
        if (time <= thresholds[1]) return "§9";
        if (time <= thresholds[2]) return "§a";
        if (time <= thresholds[3]) return "§6";
        if (time <= thresholds[4]) return "§c";
        return "§4";
    }
}