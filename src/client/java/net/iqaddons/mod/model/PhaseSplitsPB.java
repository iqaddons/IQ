package net.iqaddons.mod.model;

import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the personal-best split time for each individual Kuudra phase.
 * Unlike {@link PersonalBest}, these PBs are tracked independently per phase
 * and are not required to come from the same run.
 */
public record PhaseSplitsPB(
        Map<KuudraPhase, Long> bestSplitsMillis,
        Integer buildPbFreshCount
) {

    @Contract(" -> new")
    public static @NotNull PhaseSplitsPB empty() {
        return new PhaseSplitsPB(new EnumMap<>(KuudraPhase.class), null);
    }

    /** Returns the best recorded time in milliseconds for the given phase, or -1 if none. */
    public long getPhaseMillis(@NotNull KuudraPhase phase) {
        return bestSplitsMillis.getOrDefault(phase, -1L);
    }

    /** Returns true if a valid PB has been recorded for the given phase. */
    public boolean hasPhase(@NotNull KuudraPhase phase) {
        Long value = bestSplitsMillis.get(phase);
        return value != null && value > 0;
    }

    /** Returns the build PB fresh count, or null if no build PB metadata has been recorded yet. */
    public Integer getBuildPbFreshCount() {
        return buildPbFreshCount;
    }
}


