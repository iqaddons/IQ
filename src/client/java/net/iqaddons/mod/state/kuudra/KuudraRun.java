package net.iqaddons.mod.state.kuudra;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Builder
public record KuudraRun(
        Instant startTime,
        Map<KuudraPhase, Duration> phaseDurations
) {

    public KuudraRun {
        phaseDurations = new EnumMap<>(KuudraPhase.class);
    }

    public KuudraRun recordPhase(KuudraPhase phase, Duration duration) {
        phaseDurations.put(phase, duration);
        return this;
    }

    public Duration totalDuration() {
        return phaseDurations.values().stream()
                .reduce(Duration.ZERO, Duration::plus);
    }

    public @NotNull Optional<Duration> getPhaseDuration(@NotNull KuudraPhase phase) {
        return Optional.ofNullable(phaseDurations.get(phase));
    }
}
