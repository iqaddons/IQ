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
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class SupplyWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 1;
    private static final int BEACON_HEIGHT = 100;

    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final MinecraftClient mc = MinecraftClient.getInstance();

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

        List<ZombieEntity> zombies = PhaseOneConfig.supplyHitBox
                ? EntityDetectorUtil.getEntitiesOfType(ZombieEntity.class)
                : List.of();

        RenderColor color = RenderColor.fromArgb(PhaseOneConfig.supplyWaypointColor);
        double halfBox = PhaseOneConfig.supplyWaypointBoxSize / 2.0;
        for (SupplyPosition supply : supplies) {
            Vec3d renderPos = getInterpolatedSupplyPosition(event, supply);
            event.drawStyledWithBeam(new Box(
                    renderPos.x + 0.5 - halfBox,
                    renderPos.y - 1,
                    renderPos.z + 1.5 - halfBox,
                    renderPos.x + 0.5 + halfBox,
                    renderPos.y,
                    renderPos.z + 1.5 + halfBox
            ), BEACON_HEIGHT, true, color, WorldRenderUtils.RenderStyle.BOTH);

            if (PhaseOneConfig.supplyHitBox) {
                zombies.stream()
                        .filter(zombie -> zombie.squaredDistanceTo(renderPos) < 9)
                        .forEach(zombie ->
                                event.drawStyledHitbox(
                                        zombie, false,
                                        color, WorldRenderUtils.RenderStyle.BOTH)
                        );
            }
        }
    }

    private @NotNull Vec3d getInterpolatedSupplyPosition(@NotNull WorldRenderEvent event, @NotNull SupplyPosition supply) {
        if (mc.world == null) return supply.position();

        Entity entity = mc.world.getEntityById(supply.entityId());
        if (!(entity instanceof GiantEntity giant)) return supply.position();

        float tickDelta = event.tickCounter().getTickProgress(true);
        double x = giant.lastX + (giant.getX() - giant.lastX) * tickDelta;
        double z = giant.lastZ + (giant.getZ() - giant.lastZ) * tickDelta;

        return SupplyPosition.fromGiant(x, z, giant.getYaw(), giant.getId()).position();
    }
}