package net.iqaddons.mod.events.dispatcher;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.dispatcher.detector.ChestInteractionDetector;
import net.iqaddons.mod.events.dispatcher.detector.FreshDetector;
import net.iqaddons.mod.events.dispatcher.detector.SupplyDetector;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.utils.ScoreboardUtils;
import org.jetbrains.annotations.NotNull;

import static net.iqaddons.mod.IQConstants.*;

@Slf4j
public class KuudraEventsDispatcher extends EventDispatcher {

    private final SupplyStateManager supplyStateManager = SupplyStateManager.get();

    private final ChestInteractionDetector chestInteractionDetector = new ChestInteractionDetector();
    private final SupplyDetector supplyDetector = new SupplyDetector(supplyStateManager);
    private final FreshDetector freshDetector = new FreshDetector();

    private volatile boolean onSkyBlock = false;
    private volatile String currentArea = "";

    private volatile long lastTickCount = 0L;

    @Override
    public void start() {
        subscribe(ClientTickEvent.class, this::onClientTick);
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ScreenClickEvent.class, this::onScreenClick);
        subscribe(TitleReceivedEvent.class, this::onTitleReceived);
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        lastTickCount = event.tickCount();
        chestInteractionDetector.evictExpired(lastTickCount);

        if (!event.isInGame() || !event.isNthTick(DEFAULT_CHECK_INTERVAL_TICKS)) {
            return;
        }

        boolean wasOnSkyBlock = onSkyBlock;
        onSkyBlock = ScoreboardUtils.hasTitle(SKYBLOCK_AREA_ID);
        if (wasOnSkyBlock != onSkyBlock) {
            EventBus.post(new SkyblockAreaChangeEvent(
                    onSkyBlock, currentArea,
                    onSkyBlock ? "joined" : "left")
            );

            log.info("SkyBlock status: {}", onSkyBlock ? "joined" : "left");
        }

        if (!onSkyBlock) {
            if (!currentArea.isEmpty()) {
                currentArea = "";
            }

            return;
        }

        String newArea = ScoreboardUtils.getArea();
        if (!newArea.equals(currentArea)) {
            String previousArea = currentArea;
            currentArea = newArea;

            EventBus.post(new SkyblockAreaChangeEvent(
                    onSkyBlock, previousArea, newArea));
            log.info("Area: {} -> {}", previousArea, newArea);
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!onSkyBlock || isOutOfKuudra()) return;

        String message = event.getStrippedMessage();
        supplyDetector.detect(event, message, EventBus::post);
        freshDetector.detect(event, message, EventBus::post);
    }

    private void onScreenClick(@NotNull ScreenClickEvent event) {
        if (!onSkyBlock) return;

        chestInteractionDetector.detect(event, lastTickCount, EventBus::post);
    }

    private void onTitleReceived(TitleReceivedEvent event) {
        if (!onSkyBlock || isOutOfKuudra()) return;

        supplyDetector.detectProgress(event, EventBus::post);
    }

    private boolean isOutOfKuudra() {
        return !currentArea.toLowerCase().contains(KUUDRA_AREA_ID.toLowerCase());
    }

    @Override
    public void stop() {}
}
