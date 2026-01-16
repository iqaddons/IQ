package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.SupplyPickupEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
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

        subscribe(EventBus.subscribe(SupplyPickupEvent.class, this::onSupplyPickup));
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        MessageUtil.sendFormattedMessage(String.format(
                "%s §arecovered a supply in %s%ss §r§8(%s/6)",
                event.playerName(),
                supplyState.getTimeColor(),
                String.format("%.2f", (double) event.pickupAt()),
                event.currentSupply()
        ));
    }
}
