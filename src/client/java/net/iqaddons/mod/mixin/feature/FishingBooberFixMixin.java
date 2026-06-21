package net.iqaddons.mod.mixin.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iqaddons.mod.config.Configuration;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHook.class)
public abstract class FishingBooberFixMixin extends Projectile {

    public FishingBooberFixMixin(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world);
    }

    @WrapOperation(
            method = "onSyncedDataUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"
            )
    )
    private @Nullable Entity iq$onTrackedDataSet(Level world, int id, @NotNull Operation<Entity> original) {
        Entity entity = original.call(world, id);
        if (!Configuration.fixFishingHook) return entity;
        if (entity == null) return null;

        if (entity instanceof ArmorStand armorStand) {
            if (armorStand.getId() == this.getId() + 1) {
                return null;
            }
        }

        return entity;
    }
}
