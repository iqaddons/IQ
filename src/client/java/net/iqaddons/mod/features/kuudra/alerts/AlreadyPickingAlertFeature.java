package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class AlreadyPickingAlertFeature extends KuudraFeature {

    private static final String ALREADY_PICKING_MESSAGE = "Someone else is currently trying to pick up these supplies!";

    public AlreadyPickingAlertFeature() {
        super(
                "supplyPickingAlert",
                "Supply Already Picking Alert",
                () -> PhaseOneConfig.supplyPickingAlert,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ChatReceivedEvent.class, this::onChat);
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (event.getStrippedMessage().contains(ALREADY_PICKING_MESSAGE)) {
            MessageUtil.showTitle("§c§lAlready Picking!", "", 0, 25, 0);
        }
    }
}
