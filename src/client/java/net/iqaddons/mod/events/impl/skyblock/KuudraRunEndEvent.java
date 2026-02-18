package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;

public record KuudraRunEndEvent(
        @NotNull EndReason reason,
        @NotNull Duration totalDuration,
        @NotNull Map<KuudraPhase, Duration> phaseDurations
) implements Event {

    public @NotNull Duration getPhase(@NotNull KuudraPhase phase) {
        return phaseDurations.getOrDefault(phase, Duration.ZERO);
    }

    public boolean isCompleted() {
        return reason == EndReason.COMPLETED;
    }

    public boolean isFailed() {
        return reason == EndReason.DEFEATED;
    }

    public enum EndReason {
        COMPLETED,
        DEFEATED,
        DISCONNECTED,
        OTHER
    }
}