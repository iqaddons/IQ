package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class KuudraPhaseAlertFeature extends KuudraFeature {

    public KuudraPhaseAlertFeature() {
        super(
                "kuudraPhaseAlert",
                "Kuudra Phase Alert",
                () -> Configuration.kuudraPhaseAlert,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange));
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        var currentPhase = event.currentPhase();
        if (!currentPhase.isActive()) return;

        MessageUtil.showTitle("§6§l" + event.currentPhase().name(), "", 0, 25, 1);
    }
}
