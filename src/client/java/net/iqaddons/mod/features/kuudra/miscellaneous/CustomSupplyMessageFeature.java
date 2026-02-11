package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.SupplyPickupEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class CustomSupplyMessageFeature extends KuudraFeature {

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public CustomSupplyMessageFeature() {
        super(
                "customSupplyMessage",
                "Custom Supply Message",
                () -> PhaseOneConfig.supplyRecoverMessage,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        if (supplyState.getSuppliesPhaseStart() == null) {
            supplyState.startSuppliesPhase();
        }

        subscribe(SupplyPickupEvent.class, this::onSupplyPickup);
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        MessageUtil.sendFormattedMessage(String.format(
                "%s §arecovered a supply in %s%.2fs §r§8(%s/6)",
                event.playerName(), supplyState.getTimeColor(),
                event.pickupAt(), event.currentSupply()
        ));
    }
}
