package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;

public class SupplyDroppedAlertFeature extends KuudraFeature {

    public SupplyDroppedAlertFeature() {
        super(
                "supplyDroppedTitle",
                "Supply Dropped Title",
                () -> KuudraGeneralConfig.KuudraNotifications.supplyDropped,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(SupplyDropEvent.class, event -> {
            var player = mc.player;
            if (player == null) return;
            if (!event.playerName().equalsIgnoreCase(player.getName().getString())) {
                return;
            }

            MessageUtil.showTitle("§c§lDropped", "", 0, 15, 5);
        });
    }
}