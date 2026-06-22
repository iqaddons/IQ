package net.iqaddons.mod.manager;

import lombok.Getter;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
public final class BackboneAlertManager {

    private static final int BAR_SIZE = 20;

    private static final BackboneAlertManager INSTANCE = new BackboneAlertManager();

    private int ticksRemaining = 0;
    private int startingTicks = 0;
    private int rendTicksRemaining = 0;
    private int cooldownTicks = 0;

    private final Set<Integer> hitEntityIds = new HashSet<>();
    private int trackedBoneEntityId = -1;
    private int throwScanTicks = 0;
    private Vec3 throwOrigin = Vec3.ZERO;

    public static BackboneAlertManager get() {
        return INSTANCE;
    }

    public void startBackboneTimer(int ticks) {
        startingTicks = ticks;
        ticksRemaining = ticks;
        rendTicksRemaining = 0;
        trackedBoneEntityId = -1;
        throwScanTicks = 15;
        hitEntityIds.clear();
    }

    public void setThrowOrigin(@NotNull Vec3 throwOrigin) {
        this.throwOrigin = throwOrigin;
    }

    public boolean isRendActive() {
        return rendTicksRemaining > 0;
    }

    public boolean shouldDisplay() {
        return ticksRemaining > 0 || isRendActive();
    }

    @Contract(" -> new")
    public @NotNull BackboneAlertManager.BoneResult tick() {
        boolean shouldTriggerRend = false;

        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                rendTicksRemaining = 20;
                shouldTriggerRend = true;
            }
        }

        if (cooldownTicks > 0) cooldownTicks--;
        if (rendTicksRemaining > 0) rendTicksRemaining--;

        return new BoneResult(shouldTriggerRend);
    }

    public boolean trackBackHit(@NotNull Player player) {
        if (ticksRemaining <= 0 || isRendActive()) {
            return false;
        }

        ArmorStand boneStand = getTrackedBoneStand(player);
        if (boneStand == null) {
            return false;
        }

        Vec3 bonePos = boneStand.position();
        Optional<LivingEntity> hit = EntityDetectorUtil.getEntitiesOfType(LivingEntity.class).stream()
                .filter(entity -> entity.isAlive() && entity.getId() != player.getId())
                .filter(entity -> !(entity instanceof ArmorStand))
                .filter(entity -> entity.distanceToSqr(bonePos) <= getHitDistanceSq(entity))
                .filter(entity -> isBehindTarget(entity, bonePos))
                .filter(entity -> hitEntityIds.add(entity.getId()))
                .findFirst();

        if (hit.isEmpty()) {
            return false;
        }

        ticksRemaining = 0;
        rendTicksRemaining = 20;
        return true;
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public @NotNull String getProgressBar() {
        if (startingTicks <= 0) return "";

        int elapsedTicks = startingTicks - ticksRemaining;
        float percent = Math.max(0f, Math.min(1f, elapsedTicks / (float) startingTicks));

        int filledBars = Math.round(percent * BAR_SIZE);
        int emptyBars = BAR_SIZE - filledBars;

        String color = percent > 0.85f ? "§a" : percent > 0.6f ? "§6" : "§c";
        String filled = (color + "|").repeat(Math.max(0, filledBars));
        String empty = "§f|".repeat(Math.max(0, emptyBars));

        return "§8[" + filled + empty + "§8]";
    }

    public @NotNull String getPercentString() {
        if (startingTicks <= 0) return "§b0%";

        int elapsedTicks = startingTicks - ticksRemaining;
        float percent = Math.max(0f, Math.min(1f, elapsedTicks / (float) startingTicks));
        return "§b" + Math.round(percent * 100f) + "%";
    }

    public void reset() {
        ticksRemaining = 0;
        startingTicks = 0;
        rendTicksRemaining = 0;
        cooldownTicks = 0;
        trackedBoneEntityId = -1;
        throwScanTicks = 0;
        throwOrigin = Vec3.ZERO;
        hitEntityIds.clear();
    }

    private ArmorStand getTrackedBoneStand(@NotNull Player player) {
        if (player.level().getEntity(trackedBoneEntityId) instanceof ArmorStand armorStand && isBoneStand(armorStand)) {
            return armorStand;
        }

        trackedBoneEntityId = -1;
        if (throwScanTicks-- <= 0) {
            return null;
        }

        Vec3 viewDirection = player.getViewVector(1.0f).normalize();
        ArmorStand nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ArmorStand stand : EntityDetectorUtil.getAllArmorStands()) {
            if (!isBoneStand(stand)) continue;

            Vec3 standPos = stand.position();
            double throwDistance = standPos.distanceToSqr(throwOrigin);
            if (throwDistance > 26.0 * 26.0) continue;

            Vec3 fromPlayer = standPos.subtract(player.position());
            if (fromPlayer.lengthSqr() <= 0.01) continue;

            double alignment = fromPlayer.normalize().dot(viewDirection);
            if (alignment < 0.35) continue;

            double distance = fromPlayer.lengthSqr();
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = stand;
            }
        }

        if (nearest == null) {
            return null;
        }

        trackedBoneEntityId = nearest.getId();
        return nearest;
    }

    private boolean isBoneStand(@NotNull ArmorStand stand) {
        ItemStack heldItem = stand.getMainHandItem();
        return !heldItem.isEmpty() && heldItem.is(Items.BONE);
    }

    private double getHitDistanceSq(@NotNull LivingEntity entity) {
        double radius = 0.6 + (entity.getBbWidth() / 2.0);
        return radius * radius;
    }

    private boolean isBehindTarget(@NotNull LivingEntity target, @NotNull Vec3 bonePos) {
        Vec3 toBone = bonePos.subtract(target.position());
        Vec3 horizontalToBone = new Vec3(toBone.x, 0.0, toBone.z);
        if (horizontalToBone.lengthSqr() <= 1.0E-4) return false;

        Vec3 look = target.getViewVector(1.0f);
        Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
        if (horizontalLook.lengthSqr() <= 1.0E-4) return false;

        double dot = horizontalLook.normalize().dot(horizontalToBone.normalize());
        return dot < -0.35;
    }


    public record BoneResult(boolean triggerRendNow) {}
}
