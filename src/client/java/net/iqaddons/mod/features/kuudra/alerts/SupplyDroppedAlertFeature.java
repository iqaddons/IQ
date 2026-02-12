package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;

public class SupplyDroppedAlertFeature extends KuudraFeature {

    public SupplyDroppedAlertFeature() {
        super(
                "supplyDroppedTitle",
                "Supply Dropped Title",
                () -> PhaseOneConfig.supplyDroppedTitle,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(SupplyDropEvent.class, event -> MessageUtil.showTitle("§c§lDropped", "", 0, 15, 5));
    }
}