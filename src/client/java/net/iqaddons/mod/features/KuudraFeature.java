package net.iqaddons.mod.features;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public abstract class KuudraFeature extends Feature {

    private final Set<KuudraPhase> activePhases;
    private final KuudraStateManager stateManager = KuudraStateManager.get();

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
        onKuudraDeactivate();
    }

    private void handlePhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        boolean wasActive = shouldBeActiveForPhase(event.previousPhase());
        boolean nowActive = shouldBeActiveForPhase(event.currentPhase());

        if (!wasActive && nowActive) {
            onKuudraActivate();
        } else if (wasActive && !nowActive) {
            onKuudraDeactivate();
        }
    }

    private boolean shouldBeActiveForPhase(@NotNull KuudraPhase phase) {
        return activePhases.contains(phase);
    }

    protected void onKuudraActivate() {}

    protected void onKuudraDeactivate() {}

    protected @NotNull KuudraPhase currentPhase() {
        return stateManager.phase();
    }
}
