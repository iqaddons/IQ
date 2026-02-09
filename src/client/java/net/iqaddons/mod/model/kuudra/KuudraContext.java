package net.iqaddons.mod.model.kuudra;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public record KuudraContext(
        @NotNull KuudraPhase phase,
        @NotNull Instant phaseStartTime,
        @NotNull Instant lastValidation,
        boolean inKuudraArea, boolean onSkyBlock,
        @NotNull String currentArea
) {

    public static @NotNull KuudraContext empty() {
        return new KuudraContext(
                KuudraPhase.NONE, Instant.EPOCH, Instant.now(),
                false, false, ""
        );
    }

    public static @NotNull KuudraContext entering(@NotNull String area) {
        return new KuudraContext(
                KuudraPhase.SUPPLIES, Instant.now(), Instant.now(),
                true, true, area
        );
    }

    public @NotNull KuudraContext withPhase(@NotNull KuudraPhase newPhase) {
        return new KuudraContext(
                newPhase, Instant.now(), Instant.now(),
                inKuudraArea, onSkyBlock, currentArea
        );
    }

    public @NotNull KuudraContext validated() {
        return new KuudraContext(
                phase, phaseStartTime, Instant.now(),
                inKuudraArea, onSkyBlock, currentArea
        );
    }

    public boolean isInRun() {
        return phase.isInRun() && inKuudraArea && onSkyBlock;
    }

    public @NotNull Duration phaseDuration() {
        if (phaseStartTime == Instant.EPOCH) {
            return Duration.ZERO;
        }
        return Duration.between(phaseStartTime, Instant.now());
    }
}