package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.PhaseSplitsPB;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.data.DataKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages personal-best split times for individual Kuudra phases (T5 Infernal only).
 * Each phase is tracked independently — PBs do not need to come from the same run.
 * Data is persisted via {@link IQPersistentDataStore}.
 */
@Slf4j
public final class PhaseSplitsPBManager {

    private static final DataKey<PhaseSplitsPB> PHASE_PB_KEY =
            DataKey.of("phaseSplitsPB", PhaseSplitsPB.class);

    private static final PhaseSplitsPBManager INSTANCE = new PhaseSplitsPBManager();

    private final IQPersistentDataStore store = IQPersistentDataStore.get();
    private volatile PhaseSplitsPB current;

    private PhaseSplitsPBManager() {
        PhaseSplitsPB persisted = store.getOrDefault(PHASE_PB_KEY, PhaseSplitsPB.empty());
        if (persisted != null && persisted.bestSplitsMillis() != null) {
            Map<KuudraPhase, Long> phaseMap = new EnumMap<>(KuudraPhase.class);
            for (Map.Entry<KuudraPhase, Long> entry : persisted.bestSplitsMillis().entrySet()) {
                try {
                    if (entry.getValue() != null && entry.getValue() > 0) {
                        phaseMap.put(entry.getKey(), entry.getValue());
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Failed to parse phase-split PB entry, skipping: {}", entry, e);
                }
            }
            current = new PhaseSplitsPB(phaseMap, persisted.getBuildPbFreshCount());
        } else {
            current = PhaseSplitsPB.empty();
        }
    }

    /**
     * Returns the best recorded time in milliseconds for the given phase, or -1 if none exists.
     */
    public long getBestPhaseMillis(@NotNull KuudraPhase phase) {
        return current.getPhaseMillis(phase);
    }

    /** Returns true if a valid PB has been stored for this phase. */
    public boolean hasPhase(@NotNull KuudraPhase phase) {
        return current.hasPhase(phase);
    }

    /** Returns true if any phase PB has been recorded. */
    public boolean hasAnyPB() {
        return !current.bestSplitsMillis().isEmpty();
    }

    /**
     * Attempts to set a new PB for the given phase.
     *
     * @param phase  the Kuudra phase
     * @param millis the time achieved in this run
     * @return {@code true} if this was a new PB and storage was updated, {@code false} otherwise
     */
    public synchronized boolean tryUpdatePhase(@NotNull KuudraPhase phase, long millis) {
        return tryUpdatePhase(phase, millis, null);
    }

    /**
     * Attempts to set a new PB for the given phase and optionally persist build fresh metadata.
     *
     * @param phase           the Kuudra phase
     * @param millis          the time achieved in this run
     * @param buildFreshCount fresh count to save only when phase is BUILD (nullable)
     * @return {@code true} if this was a new PB and storage was updated, {@code false} otherwise
     */
    public synchronized boolean tryUpdatePhase(
            @NotNull KuudraPhase phase,
            long millis,
            @Nullable Integer buildFreshCount
    ) {
        long previous = current.getPhaseMillis(phase);
        if (previous > 0 && millis >= previous) {
            return false; // not an improvement
        }

        Map<KuudraPhase, Long> updated = new EnumMap<>(KuudraPhase.class);
        updated.putAll(current.bestSplitsMillis());
        updated.put(phase, millis);

        Integer nextBuildFreshCount = current.getBuildPbFreshCount();
        if (phase == KuudraPhase.BUILD && buildFreshCount != null && buildFreshCount >= 0) {
            nextBuildFreshCount = buildFreshCount;
        }

        current = new PhaseSplitsPB(updated, nextBuildFreshCount);
        store.set(PHASE_PB_KEY, current);
        return true;
    }

    /** Returns persisted build PB fresh count metadata, or null if none is stored. */
    public @Nullable Integer getBuildPbFreshCount() {
        return current.getBuildPbFreshCount();
    }

    /** Returns an unmodifiable snapshot of all stored phase PBs. */
    public @NotNull @Unmodifiable Map<KuudraPhase, Long> getAllSplits() {
        return Map.copyOf(current.bestSplitsMillis());
    }

    public static @NotNull PhaseSplitsPBManager get() {
        return INSTANCE;
    }
}


