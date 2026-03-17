package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TextColor;
import net.iqaddons.mod.utils.TimeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomSplitsWidget extends HudWidget {

    private static final Map<KuudraPhase, double[]> PHASE_THRESHOLDS = Map.of(
            KuudraPhase.SUPPLIES, new double[]{21.5, 24.7, 26.5, 28.0, 30.0},
            KuudraPhase.BUILD, new double[]{12, 15.0, 17.0, 19.0, 20.0},
            KuudraPhase.EATEN, new double[]{4.0, 5.3, 5.7, 6.0, 7.0},
            KuudraPhase.STUN, new double[]{0.0, 0.0, 0.1, 0.3, 0.8},
            KuudraPhase.DPS, new double[]{3.0, 3.6, 3.8, 4.2, 4.5},
            KuudraPhase.SKIP, new double[]{3.0, 4.2, 4.5, 4.8, 5.3},
            KuudraPhase.BOSS, new double[]{1.8, 2.3, 2.8, 3.3, 4.0}
    );

    private static final double[] OVERALL_THRESHOLDS = {53.0, 59.49, 65.0, 70.0, 80.0};

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final Map<KuudraPhase, Double> splits = new EnumMap<>(KuudraPhase.class);

    private final HudLine titleLine;
    private final HudLine suppliesLine;
    private final HudLine buildLine;
    private final HudLine eatenLine;
    private final HudLine stunLine;
    private final HudLine dpsLine;
    private final HudLine skipLine;
    private final HudLine bossLine;
    private final HudLine overallLine;
    private final HudLine paceLine;

    public CustomSplitsWidget() {
        super(
                "customSplits",
                "Custom Splits",
                6.5f, 6.5f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§b§lKuudra Splits");
        suppliesLine = HudLine.of("§7Supplies §f0.00s");
        buildLine = HudLine.of("§7Build: §f0.00s");
        eatenLine = HudLine.of("§7Eaten: §f0.00s");
        stunLine = HudLine.of("§7Stun: §f0.00s");
        dpsLine = HudLine.of("§7DPS: §f0.00s");
        skipLine = HudLine.of("§7Skip: §f0.00s");
        bossLine = HudLine.of("§7Boss: §f0.00s");
        overallLine = HudLine.of("§7Overall: §f0.00s");
        paceLine = HudLine.of("§7Pace: §f0.00s");

        setEnabledSupplier(() -> KuudraGeneralConfig.customSplits);
        setVisibilityCondition(() -> ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID));

        setExampleLines(List.of(
                HudLine.of("§b§lKuudra Splits"),
                HudLine.of("§7Supplies: §f22.45s"),
                HudLine.of("§7Build: §914.32s"),
                HudLine.of("§7Eaten: §a5.21s"),
                HudLine.of("§7Stun: §f0.53s"),
                HudLine.of("§7DPS: §63.89s"),
                HudLine.of("§7Skip: §f0.00s"),
                HudLine.of("§7Boss: §c5.12s"),
                HudLine.of("§7Overall: §a51.00s"),
                HudLine.of("§7Pace: §953.50s")
        ));
    }

    @Override
    protected void onActivate() {
        resetSplits();
        loadExistingSplits();

        clearLines();
        addLines(
                titleLine, suppliesLine, buildLine,
                eatenLine, stunLine, dpsLine, skipLine,
                bossLine, overallLine, paceLine
        );

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);

        updateDisplay();
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

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        boolean stillInKuudraInstance = event.onSkyBlock() && event.newArea().contains(IQConstants.KUUDRA_AREA_ID);
        if (stillInKuudraInstance) {
            return;
        }

        resetSplits();
        updateDisplay();
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) {
            resetSplits();
            updateDisplay();
        }
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
        updatePhaseLine(suppliesLine, "Supplies", KuudraPhase.SUPPLIES);
        updatePhaseLine(buildLine, "Build", KuudraPhase.BUILD);
        updatePhaseLine(eatenLine, "Eaten", KuudraPhase.EATEN);
        updatePhaseLine(stunLine, "Stun", KuudraPhase.STUN);
        updatePhaseLine(dpsLine, "DPS", KuudraPhase.DPS);
        updatePhaseLine(skipLine, "Skip", KuudraPhase.SKIP);
        updatePhaseLine(bossLine, "Boss", KuudraPhase.BOSS);

        double overall = calculateOverall();
        overallLine.text(String.format("%sOverall: %s%s",
                KuudraGeneralConfig.SplitColorConfig.overall.code(),
                getOverallColor(overall),
                TimeUtils.formatTime(overall))
        );

        double pace = calculatePace();
        paceLine.text(String.format("%sPace: %s%s",
                KuudraGeneralConfig.SplitColorConfig.pace.code(),
                getOverallColor(pace),
                TimeUtils.formatTime(pace))
        );

        markDimensionsDirty();
    }

    private void updatePhaseLine(@NotNull HudLine line, @NotNull String label, @NotNull KuudraPhase phase) {
        double time = splits.getOrDefault(phase, 0.0);

        line.text(String.format("%s%s: %s%s",
                getPhaseColor(label), label,
                getSplitColor(time, phase),
                TimeUtils.formatTime(time))
        );
    }

    private double calculateOverall() {
        return splits.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private double calculatePace() {
        Map<KuudraPhase, Double> paceBenchmarks = Map.of(
                KuudraPhase.SUPPLIES, KuudraGeneralConfig.CustomSplitsBenchmarks.supplies,
                KuudraPhase.BUILD, KuudraGeneralConfig.CustomSplitsBenchmarks.build,
                KuudraPhase.EATEN, KuudraGeneralConfig.CustomSplitsBenchmarks.eaten,
                KuudraPhase.STUN, KuudraGeneralConfig.CustomSplitsBenchmarks.stun,
                KuudraPhase.SKIP, KuudraGeneralConfig.CustomSplitsBenchmarks.skip,
                KuudraPhase.DPS, KuudraGeneralConfig.CustomSplitsBenchmarks.dps,
                KuudraPhase.BOSS, KuudraGeneralConfig.CustomSplitsBenchmarks.boss
        );

        double pace = 0.0;
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            double currentTime = splits.getOrDefault(phase, 0.0);
            double benchmark = paceBenchmarks.getOrDefault(phase, 0.0);

            pace += Math.max(currentTime, benchmark);
        }

        return pace;
    }

    private @NotNull String getSplitColor(double time, @NotNull KuudraPhase phase) {
        double[] thresholds = PHASE_THRESHOLDS.get(phase);
        if (thresholds == null) return "§f";

        return getSplitColor(time, thresholds);
    }

    private @NotNull String getSplitColor(double time, double @NotNull [] thresholds) {
        if (time <= 0) return "§f";

        if (time <= thresholds[0]) return TextColor.WHITE.code();
        if (time <= thresholds[1]) return TextColor.BLUE.code();
        if (time <= thresholds[2]) return TextColor.GREEN.code();
        if (time <= thresholds[3]) return TextColor.GOLD.code();
        if (time <= thresholds[4]) return TextColor.RED.code();
        return TextColor.DARK_RED.code();
    }

    private String getPhaseColor(@NotNull String label) {
        return switch (label) {
            case "Supplies" -> KuudraGeneralConfig.SplitColorConfig.supplies.code();
            case "Build" -> KuudraGeneralConfig.SplitColorConfig.build.code();
            case "Eaten" -> KuudraGeneralConfig.SplitColorConfig.eaten.code();
            case "Stun" -> KuudraGeneralConfig.SplitColorConfig.stun.code();
            case "DPS" -> KuudraGeneralConfig.SplitColorConfig.dps.code();
            case "Skip" -> KuudraGeneralConfig.SplitColorConfig.skip.code();
            case "Boss" -> KuudraGeneralConfig.SplitColorConfig.boss.code();
            default -> "§8";
        };
    }

    private @NotNull String getOverallColor(double time) {
        if (time <= 0) return "§f";
        if (time >= 48.0 && time <= 59.0) return "§9";
        return getSplitColor(time, OVERALL_THRESHOLDS);
    }
}