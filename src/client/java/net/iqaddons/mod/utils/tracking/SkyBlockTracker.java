package net.iqaddons.mod.utils.tracking;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.SkyBlockStatusEvent;
import net.iqaddons.mod.utils.ScoreboardUtils;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Getter
public final class SkyBlockTracker {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private volatile boolean onSkyBlock = false;
    private volatile String currentArea = "";

    public void start() {
        EventBus.subscribe(ClientTickEvent.class, this::onTick);
        log.info("SkyBlockTracker started");
    }

    public boolean isInArea(@NotNull String areaName) {
        return currentArea.toLowerCase().contains(areaName.toLowerCase());
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(CHECK_INTERVAL_TICKS)) {
            return;
        }

        updateSkyBlockStatus();
        updateArea();
    }

    private void updateSkyBlockStatus() {
        boolean wasOnSkyBlock = onSkyBlock;
        onSkyBlock = ScoreboardUtils.hasTitle("SKYBLOCK");

        if (wasOnSkyBlock != onSkyBlock) {
            log.info("SkyBlock status: {}", onSkyBlock ? "joined" : "left");
            EventBus.post(new SkyBlockStatusEvent(onSkyBlock, currentArea));
        }
    }

    private void updateArea() {
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
            log.debug("Area: {} -> {}", previousArea, newArea);
        }
    }
}
