package net.iqaddons.mod.model.kuudra;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public record KuudraContext(
        @NotNull KuudraPhase phase,
        @NotNull Instant phaseStartTime,
        @NotNull Instant lastValidation,
        boolean inKuudraArea, boolean onSkyBlock,
        @NotNull KuudraBossInfo bossInfo
) {

    public static @NotNull KuudraContext empty() {
        return new KuudraContext(
                KuudraPhase.NONE, Instant.EPOCH, Instant.now(),
                false, false,
                KuudraBossInfo.empty()
        );
    }

    public static @NotNull KuudraContext entering() {
        return new KuudraContext(
                KuudraPhase.SUPPLIES, Instant.now(), Instant.now(),
                true, true,
                KuudraBossInfo.empty()
        );
    }

    public @NotNull KuudraContext withPhase(@NotNull KuudraPhase newPhase) {
        return new KuudraContext(
                newPhase, Instant.now(), Instant.now(),
                inKuudraArea, onSkyBlock, bossInfo
        );
    }

    public @NotNull KuudraContext validated() {
        return new KuudraContext(
                phase, phaseStartTime, Instant.now(),
                inKuudraArea, onSkyBlock, bossInfo
        );
    }

    @Contract("_ -> new")
    public @NotNull KuudraContext withBossInfo(@NotNull KuudraBossInfo newBossInfo) {
        return new KuudraContext(
                phase, phaseStartTime, lastValidation,
                inKuudraArea, onSkyBlock,
                newBossInfo
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