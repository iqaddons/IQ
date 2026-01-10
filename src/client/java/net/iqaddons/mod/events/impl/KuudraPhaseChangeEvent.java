package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.state.KuudraPhase;

public record KuudraPhaseChangeEvent(
        KuudraPhase previousPhase,
        KuudraPhase newPhase
) implements Event {

    public boolean isEnteringKuudra() {
        return previousPhase == KuudraPhase.NONE && newPhase != KuudraPhase.NONE;
    }

    public boolean isExitingKuudra() {
        return previousPhase != KuudraPhase.NONE && newPhase == KuudraPhase.NONE;
    }

    public boolean isRunCompleted() {
        return newPhase == KuudraPhase.COMPLETED;
    }
}
