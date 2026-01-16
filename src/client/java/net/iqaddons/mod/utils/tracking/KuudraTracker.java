package net.iqaddons.mod.utils.tracking;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.SkyBlockStatusEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class KuudraTracker {

    private static final String KUUDRA_AREA = "Kuudra";

    private final KuudraStateManager stateManager;
    private final SkyBlockTracker skyBlockTracker;

    public KuudraTracker(@NotNull SkyBlockTracker skyBlockTracker) {
        this.skyBlockTracker = skyBlockTracker;
        this.stateManager = KuudraStateManager.get();
    }

    public void start() {
        EventBus.subscribe(
                ChatReceivedEvent.class,
                this::onChat
        );

        EventBus.subscribe(
                SkyBlockStatusEvent.class,
                this::onSkyBlockStatus
        );

        log.info("KuudraTracker started");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!skyBlockTracker.isOnSkyBlock()) return;
        if (!isInKuudraArea()) return;

        String message = event.getStrippedMessage();
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected != null && detected != KuudraPhase.NONE) {
            stateManager.setPhase(detected);
        }
    }

    private void onSkyBlockStatus(@NotNull SkyBlockStatusEvent event) {
        if (!event.onSkyBlock() || !isInKuudraArea()) {
            stateManager.reset();
        }
    }

    private boolean isInKuudraArea() {
        return skyBlockTracker.isInArea(KUUDRA_AREA) || stateManager.isInKuudra();
    }
}