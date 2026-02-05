package net.iqaddons.mod.features;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraContext;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public abstract class KuudraFeature extends Feature {

    private final Set<KuudraPhase> activePhases;

    protected final KuudraStateManager stateManager = KuudraStateManager.get();
    private final List<EventBus.Subscription<?>> kuudraSubscriptions = new ArrayList<>();

    private final AtomicBoolean kuudraActive = new AtomicBoolean(false);
    private final AtomicBoolean activating = new AtomicBoolean(false);
    private final AtomicInteger activationCount = new AtomicInteger(0);

    @Getter
    private EventBus.Subscription<KuudraPhaseChangeEvent> phaseChangeSubscription;
    private EventBus.Subscription<KuudraRunEndEvent> runEndSubscription;

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
        phaseChangeSubscription = EventBus.subscribe(
                KuudraPhaseChangeEvent.class,
                this::handlePhaseChange
        );

        runEndSubscription = EventBus.subscribe(
                KuudraRunEndEvent.class,
                this::handleRunEnd
        );

        if (shouldBeActiveForPhase(stateManager.phase())) {
            activateKuudraFeature("Feature enabled while in applicable phase");
        }

        log.debug("Feature {} activated, watching phases: {}", getName(), activePhases);
    }

    @Override
    protected final void onDeactivate() {
        if (kuudraActive.get()) {
            deactivateKuudraFeature("Feature disabled");
        }

        if (phaseChangeSubscription != null) {
            phaseChangeSubscription.unsubscribe();
            phaseChangeSubscription = null;
        }

        if (runEndSubscription != null) {
            runEndSubscription.unsubscribe();
            runEndSubscription = null;
        }

        log.debug("Feature {} deactivated", getName());
    }

    private void handlePhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        boolean wasActive = shouldBeActiveForPhase(event.previousPhase());
        boolean nowActive = shouldBeActiveForPhase(event.currentPhase());

        if (!wasActive && nowActive) {
            activateKuudraFeature("Phase changed to " + event.currentPhase());
        } else if (wasActive && !nowActive) {
            deactivateKuudraFeature("Phase changed to " + event.currentPhase());
        }

        if (kuudraActive.get()) {
            onPhaseChange(event);
        }
    }

    private void handleRunEnd(@NotNull KuudraRunEndEvent event) {
        if (kuudraActive.get()) {
            deactivateKuudraFeature("Run ended: " + event.reason());
        }
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
            int cycle = activationCount.incrementAndGet();

            log.info("Kuudra activating {}: {} (cycle {})", getName(), reason, cycle);
            try {
                onKuudraActivate();
            } catch (Exception e) {
                log.error("Error in onKuudraActivate for {}", getName(), e);
                kuudraActive.set(false);
                cleanupKuudraSubscriptions();
            }
        } finally {
            activating.set(false);
        }
    }

    private void deactivateKuudraFeature(@NotNull String reason) {
        if (!kuudraActive.compareAndSet(true, false)) {
            log.debug("Already kuudra-inactive: {}", getName());
            return;
        }

        int subscriptionCount = kuudraSubscriptions.size();
        cleanupKuudraSubscriptions();
        log.info("Kuudra deactivating {}: {} (cleaned {} subscriptions)",
                getName(), reason, subscriptionCount);

        try {
            onKuudraDeactivate();
        } catch (Exception e) {
            log.error("Error in onKuudraDeactivate for {}", getName(), e);
        }
    }

    private void cleanupKuudraSubscriptions() {
        for (EventBus.Subscription<?> subscription : kuudraSubscriptions) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing from event", e);
            }
        }

        kuudraSubscriptions.clear();
    }

    private boolean shouldBeActiveForPhase(@NotNull KuudraPhase phase) {
        return phase.isInRun() && activePhases.contains(phase);
    }

    protected final <T extends Event> void subscribe(
            @NotNull Class<T> eventClass,
            @NotNull Consumer<T> handler
    ) {
        EventBus.Subscription<T> subscription = EventBus.subscribe(eventClass, handler);
        kuudraSubscriptions.add(subscription);
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
}