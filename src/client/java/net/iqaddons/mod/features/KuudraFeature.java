package net.iqaddons.mod.features;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

@Slf4j
public abstract class KuudraFeature extends Feature {

    private final Set<KuudraPhase> activePhases;

    protected final KuudraStateManager stateManager = KuudraStateManager.get();

    private final AtomicBoolean kuudraActive = new AtomicBoolean(false);
    private final AtomicBoolean activating = new AtomicBoolean(false);
    private final AtomicInteger activationCount = new AtomicInteger(0);
    private final AtomicInteger subscriptionsStartIndex = new AtomicInteger(-1);

    protected KuudraFeature(
            @NotNull String id,
            @NotNull String name,
            @NotNull BooleanSupplier enabledSupplier,
            @NotNull KuudraPhase @NotNull ... activePhases
    ) {
        super(id, name, enabledSupplier);
        this.activePhases = activePhases.length > 0
                ? EnumSet.copyOf(Set.of(activePhases))
                : EnumSet.allOf(KuudraPhase.class);
    }

    @Override
    protected final void onActivate() {
        subscribe(KuudraPhaseChangeEvent.class, this::onKuudraPhaseChange);
        subscribe(KuudraRunEndEvent.class, this::onKuudraRunEnd);

        if (shouldBeActiveForPhase(stateManager.phase())) {
            activateKuudraFeature("Feature enabled while in applicable phase");
        }

        log.debug("Feature {} activated, watching phases: {}", getName(), activePhases);
    }

    @Override
    protected final void onDeactivate() {
        if (kuudraActive.get()) {
            deactivateKuudraFeature();
        }

        log.debug("Feature {} deactivated", getName());
    }

    private void onKuudraPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        boolean wasActive = shouldBeActiveForPhase(event.previousPhase());
        boolean nowActive = shouldBeActiveForPhase(event.currentPhase());

        if (!wasActive && nowActive) {
            activateKuudraFeature("Phase changed to " + event.currentPhase());
        } else if (wasActive && !nowActive) {
            deactivateKuudraFeature();
        }

        if (kuudraActive.get()) {
            onPhaseChange(event);
        }
    }

    private void onKuudraRunEnd(@NotNull KuudraRunEndEvent event) {
        if (kuudraActive.get()) deactivateKuudraFeature();
    }

    private void activateKuudraFeature(@NotNull String reason) {
        if (!activating.compareAndSet(false, true)) {
            log.warn("Reentrant activation attempted for {}", getName());
            return;
        }

        try {
            if (kuudraActive.get()) {
                log.debug("Already kuudra-active: {}", getName());
                return;
            }

            kuudraActive.set(true);
            subscriptionsStartIndex.set(subscriptionCount());
            int cycle = activationCount.incrementAndGet();

            log.info("Kuudra activating {}: {} (cycle {})", getName(), reason, cycle);
            try {
                onKuudraActivate();
            } catch (Exception e) {
                log.error("Error in onKuudraActivate for {}", getName(), e);
                kuudraActive.set(false);
                clearKuudraSubscriptions();
            }
        } finally {
            activating.set(false);
        }
    }

    private void deactivateKuudraFeature() {
        if (!kuudraActive.compareAndSet(true, false)) {
            log.debug("Already kuudra-inactive: {}", getName());
            return;
        }

        clearKuudraSubscriptions();
        try {
            onKuudraDeactivate();
        } catch (Exception e) {
            log.error("Error in onKuudraDeactivate for {}", getName(), e);
        }
    }

    private boolean shouldBeActiveForPhase(@NotNull KuudraPhase phase) {
        return phase.isInRun() && activePhases.contains(phase);
    }

    protected final @NotNull KuudraPhase currentPhase() {
        return stateManager.phase();
    }

    protected final @NotNull KuudraContext currentContext() {
        return stateManager.context();
    }

    protected final boolean isInPhase(@NotNull KuudraPhase @NotNull ... phases) {
        KuudraPhase current = stateManager.phase();
        for (KuudraPhase phase : phases) {
            if (current == phase) return true;
        }
        return false;
    }

    protected void onKuudraActivate() {}

    protected void onKuudraDeactivate() {}

    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {}

    private void clearKuudraSubscriptions() {
        int startIndex = subscriptionsStartIndex.getAndSet(-1);
        if (startIndex >= 0) {
            clearSubscriptionsFrom(startIndex);
        }
    }
}