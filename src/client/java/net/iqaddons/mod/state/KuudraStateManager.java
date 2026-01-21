package net.iqaddons.mod.state;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
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

    private Instant phaseStartTime;
    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);

    public @NotNull KuudraPhase phase() {
        return currentPhase;
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
            phaseDurations.clear();
        }

        if (newPhase == KuudraPhase.BOSS) {
            KuudraLocationUtil.invalidateCache();
        }

        log.info("Phase: {} -> {} ({}ms)", previous.getDisplayName(), newPhase.getDisplayName(), phaseDurationMs);
        EventBus.post(new KuudraPhaseChangeEvent(previous, newPhase, phaseDurationMs));

        return true;
    }

    public void reset() {
        if (currentPhase != KuudraPhase.NONE) {
            KuudraPhase previous = currentPhase;
            currentPhase = KuudraPhase.NONE;
            phaseStartTime = null;
            phaseDurations.clear();

            KuudraLocationUtil.invalidateCache();

            log.info("State reset from {}", previous.getDisplayName());
            EventBus.post(new KuudraPhaseChangeEvent(previous, KuudraPhase.NONE, 0));
        }
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


