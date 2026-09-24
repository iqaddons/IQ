package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.KuudraDebugManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class KuudraDistanceFeature extends KuudraFeature {

    private static final RenderColor ORANGE = RenderColor.fromHex(0xFFAA00);
    private static final RenderColor YELLOW = RenderColor.fromHex(0xFFFF55);
    private static final RenderColor GREEN = RenderColor.green;
    private static final RenderColor RED = RenderColor.red;

    private final KuudraDebugManager debugManager = KuudraDebugManager.get();

    private boolean wasInThrowBoneRange = false;
    private double previousDistance = Double.NaN;

    public KuudraDistanceFeature() {
        super(
                "kuudraDistanceDisplay",
                "Kuudra Distance Display",
                () -> PhaseFourConfig.kuudraDistanceDisplay,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        wasInThrowBoneRange = false;
        previousDistance = Double.NaN;
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive() && !debugManager.isActive()) return;
        if (mc.player == null) return;

        Vec3 playerPos = mc.player.position();
        Vec3 kuudraPos = debugManager.kuudraPosition().orElseGet(() -> bossInfo.bossEntity().position());
        Vec3 distanceTarget = debugManager.isActive()
                ? kuudraPos
                : closestHorizontalPoint(playerPos, bossInfo.bossEntity().getBoundingBox());
        double distance = horizontalDistance(playerPos, distanceTarget);

        event.drawText(
                kuudraPos.add(0, 8.5, 0),
                Component.literal(String.format(Locale.ROOT, "%.1fm", distance)),
                0.20f,
                true,
                getDistanceColor(distance)
        );

        boolean inThrowBoneRange = isThrowBoneRange(distance);
        boolean crossedIntoThrowBoneRange = crossedIntoThrowBoneRange(distance);
        if (PhaseFourConfig.KuudraDistanceConfig.throwBoneAlert
                && (inThrowBoneRange || crossedIntoThrowBoneRange)
                && !wasInThrowBoneRange) {
            MessageUtil.showTitle("", "\u00A7aThrow Bone", 0, 10, 5);
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0f, 1.4f);
        }
        wasInThrowBoneRange = inThrowBoneRange;
        previousDistance = distance;
    }

    private RenderColor getDistanceColor(double distance) {
        double min = greenMinDistance();
        double max = greenMaxDistance();

        if (distance >= min && distance <= max) return GREEN;
        if (distance < min - 2.0d) return RED;
        if (distance < min - 1.0d) return ORANGE;
        if (distance < min) return YELLOW;
        if (distance <= max + 2.0d) return YELLOW;
        return ORANGE;
    }

    private boolean isThrowBoneRange(double distance) {
        return distance >= greenMinDistance() && distance <= greenMaxDistance();
    }

    private boolean crossedIntoThrowBoneRange(double distance) {
        return !Double.isNaN(previousDistance)
                && previousDistance > greenMaxDistance()
                && distance < greenMinDistance();
    }

    private double horizontalDistance(@NotNull Vec3 from, @NotNull Vec3 to) {
        double dx = from.x - to.x;
        double dz = from.z - to.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private @NotNull Vec3 closestHorizontalPoint(@NotNull Vec3 playerPos, @NotNull AABB box) {
        double x = clamp(playerPos.x, box.minX, box.maxX);
        double z = clamp(playerPos.z, box.minZ, box.maxZ);
        return new Vec3(x, playerPos.y, z);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double greenMinDistance() {
        return Math.min(
                PhaseFourConfig.KuudraDistanceConfig.greenMinDistance,
                PhaseFourConfig.KuudraDistanceConfig.greenMaxDistance
        );
    }

    private double greenMaxDistance() {
        return Math.max(
                PhaseFourConfig.KuudraDistanceConfig.greenMinDistance,
                PhaseFourConfig.KuudraDistanceConfig.greenMaxDistance
        );
    }
}
