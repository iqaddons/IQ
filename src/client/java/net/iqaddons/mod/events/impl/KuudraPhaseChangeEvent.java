package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.state.KuudraPhase;

@Getter
@RequiredArgsConstructor
public class KuudraPhaseChangeEvent extends Event {

    private final KuudraPhase previousPhase;
    private final KuudraPhase newPhase;

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
