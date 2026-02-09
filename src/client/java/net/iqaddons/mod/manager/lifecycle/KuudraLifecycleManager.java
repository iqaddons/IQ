package net.iqaddons.mod.manager.lifecycle;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyBlockStatusEvent;
import net.iqaddons.mod.manager.state.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.manager.validator.KuudraStateValidator;
import net.iqaddons.mod.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class KuudraLifecycleManager {

    private static final int AREA_CHECK_INTERVAL_TICKS = 5;

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final KuudraStateValidator validator = new KuudraStateValidator();

    @Getter
    private volatile boolean started = false;

    private volatile boolean wasInKuudraArea = false;
    private volatile boolean wasOnSkyBlock = false;

    public void start() {
        if (started) {
            log.warn("KuudraLifecycleManager already started");
            return;
        }

        stateManager.start();

        EventBus.subscribe(ChatReceivedEvent.class, this::onChat);
        EventBus.subscribe(ClientTickEvent.class, this::onTick);
        EventBus.subscribe(SkyBlockStatusEvent.class, this::onSkyBlockStatus);

        started = true;
        log.info("KuudraLifecycleManager started");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected == null) return;
        log.debug("Detected phase trigger in chat: {} -> {}", message.substring(0, Math.min(50, message.length())), detected);

        if (detected == KuudraPhase.NONE) {
            if (stateManager.isInKuudra()) {
                stateManager.forceReset("Exit message detected: " + StringUtils.getShortMessage(message));
            }
            return;
        }

        stateManager.setPhase(detected);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isNthTick(AREA_CHECK_INTERVAL_TICKS)) {
            return;
        }

        if (!event.isInGame()) {
            if (stateManager.isInKuudra()) {
                stateManager.forceReset("Player left game");
            }

            return;
        }

        checkAreaChanges();
    }

    private void onSkyBlockStatus(@NotNull SkyBlockStatusEvent event) {
        if (!event.onSkyBlock() && stateManager.isInKuudra()) {
            stateManager.forceReset("Left SkyBlock");
        }
    }

    private void checkAreaChanges() {
        KuudraStateValidator.AreaInfo areaInfo = validator.detectAreaInfo();

        boolean inKuudraArea = areaInfo.inKuudra();
        boolean onSkyBlock = areaInfo.onSkyBlock();
        if (wasInKuudraArea && !inKuudraArea && stateManager.isInKuudra()) {
            log.info("Detected leaving Kuudra area (now in: {})", areaInfo.areaName());
            stateManager.forceReset("Left Kuudra area -> " + areaInfo.areaName());
        }

        if (wasOnSkyBlock && !onSkyBlock && stateManager.isInKuudra()) {
            log.info("Detected leaving SkyBlock");
            stateManager.forceReset("Left SkyBlock");
        }

        wasInKuudraArea = inKuudraArea;
        wasOnSkyBlock = onSkyBlock;
    }

    private static final KuudraLifecycleManager INSTANCE = new KuudraLifecycleManager();

    public static KuudraLifecycleManager get() {
        return INSTANCE;
    }
}