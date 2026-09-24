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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SupplyWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int BEACON_HEIGHT = 100;
    private static final float SUPPLY_PULL_CIRCLE_RADIUS = (float) SupplyPosition.PULL_CIRCLE_RADIUS;
    private static final int SUPPLY_PULL_CIRCLE_SEGMENTS = 60;
    private static final float SUPPLY_PULL_CIRCLE_THICKNESS = 0.08f;

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

        List<SupplyPosition> positions = new ArrayList<>();
        for (Giant giant : EntityDetectorUtil.getSupplyCarriers()) {
            positions.add(SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYRot(),
                        giant.getId()
            ));
        }

        supplyState.updateSupplyPositions(positions);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return;

        List<Zombie> zombies = PhaseOneConfig.SupplyWaypointsConfig.supplyHitBox
                ? EntityDetectorUtil.getEntitiesOfType(Zombie.class)
                : List.of();

        double halfBox = PhaseOneConfig.SupplyWaypointsConfig.supplyWaypointBoxSize / 2.0;
        RenderColor waypointColor = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyWaypointColor);
        RenderColor pullCircleColor = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyPullCircleColor);
        RenderColor pullCircleActiveColor = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyPullCircleActiveColor);
        RenderColor interactionBoxColor = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxColor);
        RenderColor interactionBoxInRangeColor = RenderColor.fromArgb(PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxInRangeColor);

        Set<Integer> renderedInteractionBoxes = new HashSet<>();
        for (SupplyPosition supply : supplies) {
            AABB renderBox = new AABB(
                    supply.position().x + 0.5 - halfBox,
                    supply.position().y - 1,
                    supply.position().z + 1.5 - halfBox,
                    supply.position().x + 0.5 + halfBox,
                    supply.position().y,
                    supply.position().z + 1.5 + halfBox);
            event.drawStyledWithBeam(renderBox, BEACON_HEIGHT, true, waypointColor, WorldRenderUtils.RenderStyle.BOTH);

            if (PhaseOneConfig.SupplyWaypointsConfig.supplyPullCircle) {
                renderSupplyPullCircle(
                        event,
                        supply.getPullCircleCenter(),
                        PhaseOneConfig.SupplyWaypointsConfig.supplyPullCircleBobberRange && isBobberInsideSupplyPullRange(supply)
                                ? pullCircleActiveColor
                                : pullCircleColor
                );
            }

            if (PhaseOneConfig.SupplyWaypointsConfig.supplyHitBox) {
                for (Zombie zombie : zombies) {
                    if (zombie.distanceToSqr(supply.position()) >= 9
                            || !renderedInteractionBoxes.add(zombie.getId())) continue;

                    renderInteractionHitbox(event, zombie, interactionBoxColor, interactionBoxInRangeColor);
                }
            }

        }
    }

    private void renderSupplyPullCircle(
            @NotNull WorldRenderEvent event,
            @NotNull Vec3 center,
            @NotNull RenderColor color
    ) {
        event.drawThickCircleOutline(center, SUPPLY_PULL_CIRCLE_RADIUS, SUPPLY_PULL_CIRCLE_THICKNESS,
                SUPPLY_PULL_CIRCLE_SEGMENTS, false, color);
    }

    private void renderInteractionHitbox(
            @NotNull WorldRenderEvent event,
            @NotNull Zombie zombie,
            @NotNull RenderColor normalColor,
            @NotNull RenderColor inRangeColor
    ) {
        AABB box = getInterpolatedBox(zombie, event);
        if (!PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxRange || mc.player == null) {
            event.drawStyledBox(box, false, normalColor, WorldRenderUtils.RenderStyle.BOTH);
            return;
        }

        AABB inRange = getInteractionRangeSection(box, mc.player.getEyePosition(), mc.player.entityInteractionRange());
        if (inRange == null) {
            event.drawStyledBox(box, false, normalColor, WorldRenderUtils.RenderStyle.BOTH);
            return;
        }

        if (box.minY < inRange.minY) {
            event.drawStyledBox(new AABB(box.minX, box.minY, box.minZ, box.maxX, inRange.minY, box.maxZ),
                    false, normalColor, WorldRenderUtils.RenderStyle.BOTH);
        }
        event.drawStyledBox(inRange, false, inRangeColor, WorldRenderUtils.RenderStyle.BOTH);
        if (inRange.maxY < box.maxY) {
            event.drawStyledBox(new AABB(box.minX, inRange.maxY, box.minZ, box.maxX, box.maxY, box.maxZ),
                    false, normalColor, WorldRenderUtils.RenderStyle.BOTH);
        }
    }

    private static @Nullable AABB getInteractionRangeSection(
            @NotNull AABB box,
            @NotNull Vec3 eyePosition,
            double interactionRange
    ) {
        double dx = axisDistance(eyePosition.x, box.minX, box.maxX);
        double dz = axisDistance(eyePosition.z, box.minZ, box.maxZ);
        double verticalRangeSq = interactionRange * interactionRange - dx * dx - dz * dz;
        if (verticalRangeSq <= 0.0) return null;

        double verticalRange = Math.sqrt(verticalRangeSq);
        double minY = Math.max(box.minY, eyePosition.y - verticalRange);
        double maxY = Math.min(box.maxY, eyePosition.y + verticalRange);
        if (minY >= maxY) return null;

        return new AABB(box.minX, minY, box.minZ, box.maxX, maxY, box.maxZ);
    }

    private static double axisDistance(double point, double min, double max) {
        if (point < min) return min - point;
        if (point > max) return point - max;
        return 0.0;
    }

    private @NotNull AABB getInterpolatedBox(@NotNull Zombie zombie, @NotNull WorldRenderEvent event) {
        float tickDelta = event.tickCounter().getGameTimeDeltaPartialTick(true);
        double x = zombie.xo + (zombie.getX() - zombie.xo) * tickDelta;
        double y = zombie.yo + (zombie.getY() - zombie.yo) * tickDelta;
        double z = zombie.zo + (zombie.getZ() - zombie.zo) * tickDelta;
        return zombie.getBoundingBox().move(x - zombie.getX(), y - zombie.getY(), z - zombie.getZ());
    }

    private boolean isBobberInsideSupplyPullRange(@NotNull SupplyPosition supply) {
        if (mc.player == null) return false;
        return mc.player.fishing != null && supply.isInsidePullCircle(mc.player.fishing.position());
    }
}
