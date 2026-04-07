package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.PileConfigLoader;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.PileLocation;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PearlTrajectoryFeature extends KuudraFeature {

    private static final int MAX_SIMULATION_STEPS = 200;
    private static final double INITIAL_SPEED = 1.5;
    private static final double DRAG_AIR = 0.99;
    private static final double DRAG_WATER = 0.8;
    private static final double GRAVITY = 0.03;

    private static final long ALERT_COOLDOWN_MS = 1200L;

    private final List<Vec3d> points = new ArrayList<>();

    private Vec3d landingPoint;
    private boolean guaranteedPlacement;
    private long lastAlertAtMs;

    public PearlTrajectoryFeature() {
        super(
                "pearlTrajectory",
                "Pearl Trajectory",
                () -> PhaseOneConfig.pearlTrajectoryLine,
                KuudraPhase.SUPPLIES
        );

        PileConfigLoader.get().load();
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        points.clear();
        landingPoint = null;
        guaranteedPlacement = false;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        boolean wasGuaranteed = guaranteedPlacement;
        updateTrajectory();

        if (!PhaseOneConfig.pearlTrajectoryPlacementAlert) return;
        if (!guaranteedPlacement || wasGuaranteed) return;

        long now = System.currentTimeMillis();
        if (now - lastAlertAtMs < ALERT_COOLDOWN_MS) return;

        lastAlertAtMs = now;
        MessageUtil.showTitle("", "&a%SUPPLY Guaranteed Placed", 0, 18, 6);
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (points.size() < 2) return;

        RenderColor lineColor = RenderColor.fromArgb(PhaseOneConfig.pearlTrajectoryLineColor);
        for (int i = 1; i < points.size(); i++) {
            Vec3d start = points.get(i - 1);
            Vec3d end = points.get(i);
            event.drawLine(start, end, true, lineColor);
        }
    }

    private void updateTrajectory() {
        points.clear();
        landingPoint = null;
        guaranteedPlacement = false;

        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        if (!isHoldingPearl(player)) return;

        Vec3d pos = player.getEyePos().add(player.getRotationVec(1.0f).multiply(0.16));
        Vec3d velocity = player.getRotationVec(1.0f).normalize().multiply(INITIAL_SPEED);

        points.add(pos);

        for (int i = 0; i < MAX_SIMULATION_STEPS; i++) {
            Vec3d nextPos = pos.add(velocity);

            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    pos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                Vec3d impactPos = hit.getPos();
                points.add(impactPos);
                landingPoint = impactPos;
                break;
            }

            points.add(nextPos);

            BlockPos nextBlockPos = BlockPos.ofFloored(nextPos);
            boolean inWater = mc.world.getBlockState(nextBlockPos).getBlock() instanceof FluidBlock;
            velocity = velocity.multiply(inWater ? DRAG_WATER : DRAG_AIR).subtract(0, GRAVITY, 0);
            pos = nextPos;

            if (pos.y < mc.world.getBottomY() - 4) break;
        }

        if (landingPoint != null) {
            guaranteedPlacement = matchesAnyPile(landingPoint);
        }
    }

    private boolean matchesAnyPile(@NotNull Vec3d impact) {
        double toleranceSq = PhaseOneConfig.pearlTrajectoryPileTolerance * PhaseOneConfig.pearlTrajectoryPileTolerance;

        for (PileLocation pile : PileConfigLoader.get().getCached()) {
            Vec3d pilePos = pile.position();
            double dx = impact.x - pilePos.x;
            double dz = impact.z - pilePos.z;

            if ((dx * dx) + (dz * dz) <= toleranceSq) {
                return true;
            }
        }

        return false;
    }

    private boolean isHoldingPearl(@NotNull PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof EnderPearlItem) return true;

        ItemStack off = player.getOffHandStack();
        return off.getItem() instanceof EnderPearlItem;
    }
}
