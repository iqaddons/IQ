package net.iqaddons.mod.features.kuudra.waypoints.supply;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.supply.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class SupplyWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int BEACON_HEIGHT = 100;

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public SupplyWaypointsFeature() {
        super(
                "supplyWaypoints",
                "Supply Waypoints",
                () -> Configuration.PhaseOneConfig.supplyWaypoints,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onRender));
        log.info("Supply waypoints activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Supply waypoints deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(UPDATE_INTERVAL_TICKS)) return;

        List<GiantEntity> carriers = EntityDetectorUtil.getSupplyCarriers();
        List<SupplyPosition> positions = carriers.stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYaw(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(positions);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return;

        RenderColor color = RenderColor.fromArgb(Configuration.PhaseOneConfig.supplyWaypointColor);
        for (SupplyPosition supply : supplies) {
            Box crateBox = new Box(
                    supply.position().x,
                    supply.position().y - 1,
                    supply.position().z,
                    supply.position().x + 1,
                    supply.position().y,
                    supply.position().z + 1
            );

            event.drawFilledWithBeam(crateBox, BEACON_HEIGHT, true, color);
        }
    }
}