package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.state.kuudra.KuudraPhase;

public record KuudraPhaseChangeEvent(
        KuudraPhase previousPhase,
        KuudraPhase currentPhase,
        long phaseDurationMillis
) implements Event {

    public boolean isEnteringKuudra() {
        return previousPhase == KuudraPhase.NONE && currentPhase != KuudraPhase.NONE;
    }

    public boolean isExitingKuudra() {
        return previousPhase != KuudraPhase.NONE && currentPhase == KuudraPhase.NONE;
    }

    public boolean isRunCompleted() {
        return currentPhase == KuudraPhase.COMPLETED;
    }
}
