package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
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
                () -> PhaseOneConfig.supplyWaypoints,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
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

        RenderColor color = RenderColor.fromArgb(PhaseOneConfig.supplyWaypointColor);
        for (SupplyPosition supply : supplies) {
            Box crateBox = new Box(
                    supply.position().x,
                    supply.position().y - 1,
                    supply.position().z,
                    supply.position().x + 1,
                    supply.position().y,
                    supply.position().z + 1
            );

            event.drawStyledWithBeam(crateBox, BEACON_HEIGHT, true, color, WorldRenderUtils.RenderStyle.BOTH);
        }
    }
}