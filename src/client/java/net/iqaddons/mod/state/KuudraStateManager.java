package net.iqaddons.mod.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.state.kuudra.KuudraContext;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.validator.KuudraStateValidator;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class KuudraStateManager {

    private static final KuudraStateManager INSTANCE = new KuudraStateManager();

    private static final int HEARTBEAT_INTERVAL_TICKS = 10;

    private final AtomicReference<KuudraContext> contextRef = new AtomicReference<>(KuudraContext.empty());
    private final KuudraStateValidator validator = new KuudraStateValidator();
    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);

    @Getter
    private volatile boolean started = false;

    private EventBus.Subscription<ClientTickEvent> heartbeatSubscription;

    public void start() {
        if (started) {
            return;
        }

        heartbeatSubscription = EventBus.subscribe(ClientTickEvent.class, this::onHeartbeat);
        started = true;
    }

    public void stop() {
        if (!started) return;
        if (heartbeatSubscription != null) {
            heartbeatSubscription.unsubscribe();
            heartbeatSubscription = null;
        }

        forceReset("Manager stopped");
        started = false;
    }

    public @NotNull KuudraPhase phase() {
        return contextRef.get().phase();
    }

    public @NotNull KuudraContext context() {
        return contextRef.get();
    }

    public boolean isInKuudra() {
        return contextRef.get().phase() != KuudraPhase.NONE;
    }

    public boolean isInRun() {
        return contextRef.get().isInRun();
    }

    public boolean setPhase(@NotNull KuudraPhase newPhase) {
        KuudraContext current = contextRef.get();
        if (current.phase() == newPhase) return false;
        if (!current.phase().canTransitionTo(newPhase)) {
            log.warn("Invalid phase transition: {} -> {}", current.phase(), newPhase);
            return false;
        }

        if (newPhase == KuudraPhase.NONE) {
            return handleRunEnd(current, "Phase set to NONE");
        }

        if (current.phase() == KuudraPhase.NONE && newPhase == KuudraPhase.SUPPLIES) {
            return handleRunStart();
        }

        return performPhaseTransition(current, newPhase);
    }

    public void forceReset(@NotNull String reason) {
        KuudraContext current = contextRef.get();
        if (current.phase() == KuudraPhase.NONE) {
            return;
        }

        log.warn("Forcing state reset: {} (was in phase {})", reason, current.phase());
        handleRunEnd(current, reason);
    }

    public @NotNull Optional<Duration> getPhaseDuration(@NotNull KuudraPhase phase) {
        return Optional.ofNullable(phaseDurations.get(phase));
    }

    public @NotNull Optional<Duration> currentPhaseDuration() {
        KuudraContext ctx = contextRef.get();
        if (ctx.phase() == KuudraPhase.NONE) {
            return Optional.empty();
        }

        return Optional.of(ctx.phaseDuration());
    }

    private void onHeartbeat(@NotNull ClientTickEvent event) {
        if (!event.isNthTick(HEARTBEAT_INTERVAL_TICKS)) {
            return;
        }

        KuudraContext current = contextRef.get();
        if (current.phase() == KuudraPhase.NONE) {
            return;
        }

        KuudraStateValidator.ValidationResult result = validator.validate(current);
        if (result.isValid()) {
            KuudraContext validated = current.validated();
            contextRef.compareAndSet(current, validated);
            log.trace("Validation passed for phase {}", current.phase());
        } else {
            forceReset("Validation failures: " + result.reason());
        }
    }

    private boolean handleRunStart() {
        KuudraStateValidator.AreaInfo areaInfo = validator.detectAreaInfo();
        if (!areaInfo.canBeInRun()) {
            log.warn("Cannot start run - not in Kuudra area");
            return false;
        }

        KuudraContext newContext = KuudraContext.entering(areaInfo.areaName());
        KuudraContext old = contextRef.getAndSet(newContext);

        phaseDurations.clear();
        EventBus.post(new KuudraPhaseChangeEvent(
                old.phase(),
                KuudraPhase.SUPPLIES,
                0
        ));

        return true;
    }

    private boolean handleRunEnd(@NotNull KuudraContext current, @NotNull String reason) {
        if (current.phase().isInRun()) {
            phaseDurations.put(current.phase(), current.phaseDuration());
        }

        KuudraPhase previousPhase = current.phase();
        long phaseDurationMs = current.phaseDuration().toMillis();

        contextRef.set(KuudraContext.empty());
        Duration totalDuration = phaseDurations.values().stream()
                .reduce(Duration.ZERO, Duration::plus);

        log.info("Kuudra run ended: {} (total: {}ms)", reason, totalDuration.toMillis());

        EventBus.post(new KuudraPhaseChangeEvent(
                previousPhase,
                KuudraPhase.NONE,
                phaseDurationMs
        ));

        EventBus.post(new KuudraRunEndEvent(
                reason,
                previousPhase == KuudraPhase.COMPLETED,
                totalDuration,
                Map.copyOf(phaseDurations)
        ));

        phaseDurations.clear();
        return true;
    }

    private boolean performPhaseTransition(
            @NotNull KuudraContext current,
            @NotNull KuudraPhase newPhase
    ) {
        KuudraPhase previousPhase = current.phase();
        Duration phaseDuration = current.phaseDuration();
        if (previousPhase.isInRun()) {
            phaseDurations.put(previousPhase, phaseDuration);
        }

        KuudraContext newContext = current.withPhase(newPhase);
        boolean updated = contextRef.compareAndSet(current, newContext);

        if (!updated) {
            log.warn("Concurrent modification during phase transition");
            return false;
        }

        log.info("Phase transition: {} -> {} ({}ms)",
                previousPhase.getDisplayName(),
                newPhase.getDisplayName(),
                phaseDuration.toMillis());

        EventBus.post(new KuudraPhaseChangeEvent(
                previousPhase,
                newPhase,
                phaseDuration.toMillis()
        ));

        return true;
    }

    public static KuudraStateManager get() {
        return INSTANCE;
    }
}