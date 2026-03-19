package net.iqaddons.mod.model;

import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.kuudra.KuudraTier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record PersonalBest(
        long bestTimeMillis,
        Map<KuudraPhase, Long> phaseSplitsMillis,
        @NotNull KuudraTier tier,
        long recordedAtEpochMillis,
        @NotNull List<SupplyTiming> supplyTimings,
        @NotNull List<FreshTiming> freshTimings
) {

    @Contract(" -> new")
    public static @NotNull PersonalBest empty() {
        return new PersonalBest(
                -1L,
                new EnumMap<>(KuudraPhase.class),
                KuudraTier.UNKNOWN,
                0L,
                List.of(),
                List.of()
        );
    }

    public @NotNull KuudraTier safeTier() {
        return tier == null ? KuudraTier.UNKNOWN : tier;
    }

    public @NotNull List<SupplyTiming> safeSupplyTimings() {
        return supplyTimings == null ? List.of() : List.copyOf(supplyTimings);
    }

    public @NotNull List<FreshTiming> safeFreshTimings() {
        return freshTimings == null ? List.of() : List.copyOf(freshTimings);
    }

    public record SupplyTiming(
            @NotNull String playerName,
            int currentSupply,
            double seconds
    ) {
        public static @NotNull SupplyTiming of(@NotNull String playerName, int currentSupply, double seconds) {
            return new SupplyTiming(playerName, currentSupply, seconds);
        }
    }

    public record FreshTiming(
            @NotNull String playerName,
            double seconds
    ) {
        public static @NotNull FreshTiming of(@NotNull String playerName, double seconds) {
            return new FreshTiming(playerName, seconds);
        }
    }
}