package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.sound.SoundEvents;

@Slf4j
public class SupplyDroppedAlertFeature extends KuudraFeature {

    public SupplyDroppedAlertFeature() {
        super(
                "supplyDroppedTitle",
                "Supply Dropped Title",
                () -> KuudraGeneralConfig.kuudraNotifications
                        && KuudraGeneralConfig.KuudraNotifications.NotificationToggles.supplyDropped,
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

            String bold = KuudraGeneralConfig.KuudraNotifications.bold ? "§l" : "";
            String text = KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyDroppedText;
            if (text == null || text.isBlank()) text = "DROPPED";
            String alertText = KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyDroppedColor.code()
                    + bold + text;
            int duration = KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyDroppedDuration;
            MessageUtil.showAlert(alertText, duration, SoundEvents.BLOCK_ANVIL_LAND);
        });
    }
}