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
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class SupplyWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int BEACON_HEIGHT = 100;
    private static final double SUPPLY_PULL_RADIUS = 5.0;
    private static final double SUPPLY_VERTICAL_MARGIN = 4.0;

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
                RenderColor pullCircleColor = isBobberInsideSupplyPullRange(supply) ? RenderColor.green : color;
                event.drawCircleOutline(new Vec3(
                        supply.position().x + 0.5,
                        supply.position().y,
                        supply.position().z + 1.5
                ), 5, 60, false, pullCircleColor);
            }

            if (PhaseOneConfig.SupplyWaypointsConfig.supplyHitBox) {
                zombies.stream()
                        .filter(zombie -> zombie.distanceToSqr(supply.position()) < 9)
                        .forEach(zombie -> {
                            var zombieDistance = zombie.distanceTo(mc.player);
                            var interactionDistance = 3d;

                            event.drawStyledHitbox(
                                    zombie, false,
                                    zombieDistance > interactionDistance ?
                                            color :
                                            RenderColor.green,
                                    WorldRenderUtils.RenderStyle.BOTH
                            );
                        });
            }
        }
    }

    private boolean isBobberInsideSupplyPullRange(@NotNull SupplyPosition supply) {
        if (mc.player == null) return false;
        FishingHook bobber = mc.player.fishing;
        if (bobber == null) return false;

        Vec3 bobberPos = bobber.position();
        // Matches SupplyRodPullRecastFeature pull range logic.
        double centerX = supply.position().x + 0.5;
        double centerZ = supply.position().z + 1.5;
        double dx = bobberPos.x - centerX;
        double dz = bobberPos.z - centerZ;
        if ((dx * dx) + (dz * dz) > SUPPLY_PULL_RADIUS * SUPPLY_PULL_RADIUS) return false;

        Vec3 min = supply.getBoxMin();
        Vec3 max = supply.getBoxMax();
        return bobberPos.y >= min.y - SUPPLY_VERTICAL_MARGIN && bobberPos.y <= max.y + SUPPLY_VERTICAL_MARGIN;
    }
}