package net.iqaddons.mod.tracking;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.SkyBlockStatusEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.data.KuudraPhase;
import net.iqaddons.mod.state.data.KuudraTier;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class KuudraTracker {

    private static final String KUUDRA_AREA = "Kuudra";

    private final KuudraStateManager stateManager;
    private final SkyBlockTracker skyBlockTracker;

    private EventBus.Subscription<ChatReceivedEvent> chatSubscription;
    private EventBus.Subscription<SkyBlockStatusEvent> statusSubscription;

    public KuudraTracker(@NotNull SkyBlockTracker skyBlockTracker) {
        this.skyBlockTracker = skyBlockTracker;
        this.stateManager = KuudraStateManager.get();
    }

    public void start() {
        chatSubscription = EventBus.subscribe(
                ChatReceivedEvent.class,
                this::onChat
        );

        statusSubscription = EventBus.subscribe(
                SkyBlockStatusEvent.class,
                this::onSkyBlockStatus
        );

        log.info("KuudraTracker started");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!skyBlockTracker.isOnSkyBlock()) return;
        if (!isInKuudraArea()) return;

        String message = event.getStrippedMessage();

        detectPhaseFromMessage(message);
        detectTierFromMessage(message);
    }

    private void onSkyBlockStatus(@NotNull SkyBlockStatusEvent event) {
        if (!event.onSkyBlock()) {
            stateManager.reset();
        }
    }

    private void detectPhaseFromMessage(@NotNull String message) {
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected != null) {
            stateManager.setPhase(detected);
        }
    }

    private void detectTierFromMessage(@NotNull String message) {
        if (!message.toUpperCase().contains("KUUDRA")) return;

        KuudraTier detected = KuudraTier.fromText(message);
        if (detected != null) {
            stateManager.setTier(detected);
        }
    }

    private boolean isInKuudraArea() {
        return skyBlockTracker.isInArea(KUUDRA_AREA) || stateManager.isInKuudra();
    }

    public void stop() {
        if (chatSubscription != null) {
            chatSubscription.unsubscribe();
            chatSubscription = null;
        }
        if (statusSubscription != null) {
            statusSubscription.unsubscribe();
            statusSubscription = null;
        }
    }
}