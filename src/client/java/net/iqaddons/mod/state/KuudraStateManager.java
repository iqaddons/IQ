package net.iqaddons.mod.state;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.kuudra.KuudraRun;
import net.iqaddons.mod.state.kuudra.KuudraTier;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class KuudraStateManager {

    private static final KuudraStateManager INSTANCE = new KuudraStateManager();

    private volatile KuudraPhase currentPhase = KuudraPhase.NONE;
    private volatile KuudraTier currentTier = KuudraTier.NONE;

    private Instant phaseStartTime;
    private Instant runStartTime;
    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);

    public @NotNull KuudraPhase phase() {
        return currentPhase;
    }

    public @NotNull KuudraTier tier() {
        return currentTier;
    }

    public boolean isInKuudra() {
        return currentPhase != KuudraPhase.NONE;
    }

    public boolean isInRun() {
        return currentPhase.isInRun();
    }

    public Optional<Duration> currentPhaseDuration() {
        if (phaseStartTime == null) return Optional.empty();
        return Optional.of(Duration.between(phaseStartTime, Instant.now()));
    }

    public Optional<Duration> runDuration() {
        if (runStartTime == null) return Optional.empty();
        return Optional.of(Duration.between(runStartTime, Instant.now()));
    }

    public @NotNull Optional<Duration> getPhaseDuration(@NotNull KuudraPhase phase) {
        return Optional.ofNullable(phaseDurations.get(phase));
    }

    public boolean setPhase(@NotNull KuudraPhase newPhase) {
        if (currentPhase == newPhase) return false;
        if (!currentPhase.canTransitionTo(newPhase)) {
            log.warn("Invalid phase transition: {} -> {}", currentPhase, newPhase);
            return false;
        }

        KuudraPhase previous = currentPhase;
        long phaseDurationMs = recordPhaseEnd();

        currentPhase = newPhase;
        phaseStartTime = Instant.now();

        if (newPhase == KuudraPhase.SUPPLIES && previous == KuudraPhase.NONE) {
            runStartTime = phaseStartTime;
            phaseDurations.clear();
        }

        log.info("Phase: {} -> {} ({}ms)", previous.getDisplayName(), newPhase.getDisplayName(), phaseDurationMs);
        EventBus.post(new KuudraPhaseChangeEvent(previous, newPhase, phaseDurationMs));

        return true;
    }

    public void setTier(@NotNull KuudraTier tier) {
        if (currentTier != tier) {
            currentTier = tier;
            log.info("Tier set: {}", tier.getDisplayName());
        }
    }

    public void reset() {
        if (currentPhase != KuudraPhase.NONE) {
            KuudraPhase previous = currentPhase;
            currentPhase = KuudraPhase.NONE;
            currentTier = KuudraTier.NONE;
            phaseStartTime = null;
            runStartTime = null;
            phaseDurations.clear();

            log.info("State reset from {}", previous.getDisplayName());
            EventBus.post(new KuudraPhaseChangeEvent(previous, KuudraPhase.NONE, 0));
        }
    }

    public Optional<KuudraRun> buildRunSnapshot() {
        if (runStartTime == null) return Optional.empty();

        return Optional.of(new KuudraRun(
                currentTier,
                runStartTime,
                Map.copyOf(phaseDurations)
        ));
    }

    private long recordPhaseEnd() {
        if (phaseStartTime == null || !currentPhase.isInRun()) {
            return 0;
        }

        Duration duration = Duration.between(phaseStartTime, Instant.now());
        phaseDurations.put(currentPhase, duration);
        return duration.toMillis();
    }

    public static KuudraStateManager get() {
        return INSTANCE;
    }
}


