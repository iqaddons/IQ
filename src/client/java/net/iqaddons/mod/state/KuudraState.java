package net.iqaddons.mod.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Slf4j
public final class KuudraState {

    @Getter
    private static KuudraPhase currentPhase = KuudraPhase.NONE;

    @Getter
    private static KuudraTier currentTier = KuudraTier.NONE;

    private static long phaseStartTime = 0;
    private static long runStartTime = 0;

    private static final long[] phaseTimes = new long[5];

    private KuudraState() {}

    public static void setPhase(KuudraPhase phase) {
        if (currentPhase == phase) return;

        KuudraPhase previous = currentPhase;
        if (previous.isInRun()) {
            phaseTimes[previous.getOrder()] = getPhaseTime();
        }

        currentPhase = phase;
        phaseStartTime = System.currentTimeMillis();

        if (phase == KuudraPhase.SUPPLIES && previous == KuudraPhase.QUEUE) {
            runStartTime = phaseStartTime;
            resetSplits();
        }

        log.info("Kuudra phase changed: {} -> {}", previous.getDisplayName(), phase.getDisplayName());
    }

    public static void setTier(KuudraTier tier) {
        if (currentTier != tier) {
            currentTier = tier;
            log.info("Kuudra tier set: {}", tier.getDisplayName());
        }
    }

    public static void reset() {
        setPhase(KuudraPhase.NONE);
        currentTier = KuudraTier.NONE;
        runStartTime = 0;
        resetSplits();
    }

    public static long getPhaseTime() {
        return phaseStartTime > 0 ? System.currentTimeMillis() - phaseStartTime : 0;
    }

    public static long getRunTime() {
        return runStartTime > 0 ? System.currentTimeMillis() - runStartTime : 0;
    }

    public static long getSplitTime(@NotNull KuudraPhase phase) {
        if (phase.getOrder() < 1 || phase.getOrder() > 4) return 0;
        return phaseTimes[phase.getOrder()];
    }

    public static boolean isInKuudra() {
        return currentPhase != KuudraPhase.NONE;
    }

    public static boolean isInRun() {
        return currentPhase.isInRun();
    }

    private static void resetSplits() {
        Arrays.fill(phaseTimes, 0);
    }
}