package net.iqaddons.mod.features;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

@Slf4j
public abstract class KuudraFeature extends Feature {

    private final Set<KuudraPhase> activePhases;
    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final List<EventBus.Subscription<?>> kuudraSubscriptions = new ArrayList<>();
    private volatile boolean kuudraActive = false;
    private volatile boolean inKuudraActivation = false;

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
        subscribe(EventBus.subscribe(KuudraPhaseChangeEvent.class, this::handlePhaseChange));

        if (shouldBeActiveForPhase(stateManager.phase())) {
            onKuudraActivate();
        }
    }

    @Override
    protected final void onDeactivate() {
        if (kuudraActive) {
            onKuudraDeactivate();
        }
    }

    @Override
    protected final <T extends Event> void subscribe(@NotNull EventBus.Subscription<T> subscription) {
        if (inKuudraActivation) {
            kuudraSubscriptions.add(subscription);
        } else {
            super.subscribe(subscription);
        }
    }

    private void handlePhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        boolean wasActive = shouldBeActiveForPhase(event.previousPhase());
        boolean nowActive = shouldBeActiveForPhase(event.currentPhase());

        if (!wasActive && nowActive) {
            activateKuudraFeatures();
        } else if (wasActive && !nowActive) {
            deactivateKuudraFeatures();
        }
    }

    private void activateKuudraFeatures() {
        if (kuudraActive) {
            log.warn("Attempted to activate {} while already kuudra-active", getName());
            return;
        }

        kuudraActive = true;
        log.debug("Kuudra activating: {} (phase: {})", getName(), stateManager.phase());
        try {
            inKuudraActivation = true;
            onKuudraActivate();
        } catch (Exception e) {
            log.error("Error during onKuudraActivate for {}", getName(), e);
        } finally {
            inKuudraActivation = false;
        }
    }

    private void deactivateKuudraFeatures() {
        if (!kuudraActive) {
            log.warn("Attempted to deactivate {} while not kuudra-active", getName());
            return;
        }

        kuudraActive = false;

        int subCount = kuudraSubscriptions.size();
        kuudraSubscriptions.forEach(EventBus.Subscription::unsubscribe);
        kuudraSubscriptions.clear();

        log.debug("Kuudra deactivating: {} (cleaned {} subscriptions)", getName(), subCount);

        try {
            onKuudraDeactivate();
        } catch (Exception e) {
            log.error("Error during onKuudraDeactivate for {}", getName(), e);
        }
    }

    private boolean shouldBeActiveForPhase(@NotNull KuudraPhase phase) {
        return stateManager.isInKuudra() && activePhases.contains(phase);
    }

    protected void onKuudraActivate() {}

    protected void onKuudraDeactivate() {}

    protected @NotNull KuudraPhase currentPhase() {
        return stateManager.phase();
    }
}
