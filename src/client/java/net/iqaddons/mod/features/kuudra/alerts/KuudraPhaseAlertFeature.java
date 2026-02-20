package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

public class KuudraPhaseAlertFeature extends KuudraFeature {

    public KuudraPhaseAlertFeature() {
        super(
                "kuudraPhaseAlert",
                "Kuudra Phase Alert",
                () -> KuudraGeneralConfig.KuudraNotifications.phaseChange,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        var currentPhase = event.currentPhase();
        if (!currentPhase.isActive()) return;

        MessageUtil.showAlert("§6§l" + currentPhase.name(), 25);
    }
}
