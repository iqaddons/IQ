package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.PersonalBest;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.kuudra.KuudraTier;
import net.iqaddons.mod.utils.data.DataKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class PersonalBestManager {

    private static final DataKey<PersonalBest> PB_KEY = DataKey.of("personalBest", PersonalBest.class);
    private static final PersonalBestManager INSTANCE = new PersonalBestManager();

    private final IQPersistentDataStore store = IQPersistentDataStore.get();

    private volatile PersonalBest current = PersonalBest.empty();

    private PersonalBestManager() {
        PersonalBest persisted = store.getOrDefault(PB_KEY, PersonalBest.empty());
        if (persisted.bestTimeMillis() > 0 || !persisted.phaseSplitsMillis().isEmpty() || Files.exists(IQPersistentDataStore.DATA_FILE)) {
            Map<KuudraPhase, Long> phaseMap = new EnumMap<>(KuudraPhase.class);
            for (Map.Entry<KuudraPhase, Long> entry : persisted.phaseSplitsMillis().entrySet()) {
                try {
                    phaseMap.put(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    log.warn("Failed to parse Kuudra phase split for PB tracker, skipping entry: {}", entry, e);
                }
            }

            List<PersonalBest.SupplyTiming> supplyTimings = new ArrayList<>();
            for (PersonalBest.SupplyTiming supplyTiming : persisted.safeSupplyTimings()) {
                if (supplyTiming.playerName() != null && !supplyTiming.playerName().isBlank() && supplyTiming.currentSupply() > 0) {
                    supplyTimings.add(supplyTiming);
                }
            }

            List<PersonalBest.FreshTiming> freshTimings = new ArrayList<>();
            for (PersonalBest.FreshTiming freshTiming : persisted.safeFreshTimings()) {
                if (freshTiming.playerName() != null && !freshTiming.playerName().isBlank() && freshTiming.seconds() >= 0) {
                    freshTimings.add(freshTiming);
                }
            }

            current = new PersonalBest(
                    persisted.bestTimeMillis(),
                    phaseMap,
                    persisted.safeTier(),
                    Math.max(0L, persisted.recordedAtEpochMillis()),
                    List.copyOf(supplyTimings),
                    List.copyOf(freshTimings)
            );
        }
    }

    public synchronized void updatePersonalBest(long totalMillis, @NotNull Map<KuudraPhase, Long> splitMillis) {
        updatePersonalBest(totalMillis, splitMillis, KuudraTier.UNKNOWN, 0L, List.of(), List.of());
    }

    public synchronized void updatePersonalBest(
            long totalMillis,
            @NotNull Map<KuudraPhase, Long> splitMillis,
            @NotNull KuudraTier tier,
            long recordedAtEpochMillis,
            @NotNull List<PersonalBest.SupplyTiming> supplyTimings,
            @NotNull List<PersonalBest.FreshTiming> freshTimings
    ) {
        Map<KuudraPhase, Long> nextSplits = new EnumMap<>(KuudraPhase.class);
        nextSplits.putAll(splitMillis);

        current = new PersonalBest(
                totalMillis,
                nextSplits,
                tier,
                recordedAtEpochMillis,
                List.copyOf(supplyTimings),
                List.copyOf(freshTimings)
        );
        store.set(PB_KEY, current);
    }

    public long getBestTimeMillis() {
        return current.bestTimeMillis();
    }

    public @NotNull @Unmodifiable Map<KuudraPhase, Long> getSplitsMillis() {
        return Map.copyOf(current.phaseSplitsMillis());
    }

    public @NotNull KuudraTier getTier() {
        return current.safeTier();
    }

    public long getRecordedAtEpochMillis() {
        return current.recordedAtEpochMillis();
    }

    public @NotNull List<PersonalBest.SupplyTiming> getSupplyTimings() {
        return current.safeSupplyTimings();
    }

    public @NotNull List<PersonalBest.FreshTiming> getFreshTimings() {
        return current.safeFreshTimings();
    }

    public boolean hasPersonalBest() {
        return current.bestTimeMillis() > 0;
    }

    public static @NotNull PersonalBestManager get() {
        return INSTANCE;
    }
}