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
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        List<Giant> carriers = EntityDetectorUtil.getSupplyCarriers();
        List<SupplyPosition> positions = carriers.stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYRot(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(positions);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return;

        List<Zombie> zombies = PhaseOneConfig.SupplyWaypointsConfig.supplyHitBox
                ? EntityDetectorUtil.getEntitiesOfType(Zombie.class)
                : List.of();

        double halfBox = PhaseOneConfig.SupplyWaypointsConfig.supplyWaypointBoxSize / 2.0;
        for (SupplyPosition supply : supplies) {
            AABB renderBox = new AABB(
                    supply.position().x + 0.5 - halfBox,
                    supply.position().y - 1,
                    supply.position().z + 1.5 - halfBox,
                    supply.position().x + 0.5 + halfBox,
                    supply.position().y,
                    supply.position().z + 1.5 + halfBox);
            RenderColor color = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyWaypointColor);

            event.drawStyledWithBeam(
                    renderBox, BEACON_HEIGHT,
                    true, color,
                    WorldRenderUtils.RenderStyle.BOTH
            );

            if (PhaseOneConfig.SupplyWaypointsConfig.supplyPullCircle) {
                event.drawCircleOutline(new Vec3(
                        supply.position().x + 0.5,
                        supply.position().y,
                        supply.position().z + 1.5
                ), 5, 60, false, color);
            }

            if (PhaseOneConfig.SupplyWaypointsConfig.supplyHitBox) {
                zombies.stream()
                        .filter(zombie -> zombie.distanceToSqr(supply.position()) < 9)
                        .forEach(zombie -> {
                            event.drawStyledHitbox(
                                    zombie, false,
                                    zombie.distanceTo(mc.player) > 3 ?
                                            color :
                                            RenderColor.green,
                                    WorldRenderUtils.RenderStyle.BOTH
                            );
                        });
            }
        }
    }
}