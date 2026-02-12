package net.iqaddons.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.iqaddons.mod.config.Configuration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingBobberEntity.class)
public abstract class FishingHookMixin extends ProjectileEntity {

    public FishingHookMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @WrapOperation(
            method = "onTrackedDataSet",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getEntityById(I)Lnet/minecraft/entity/Entity;"
            )
    )
    private @Nullable Entity iq$onTrackedDataSet(World world, int id, @NotNull Operation<Entity> original) {
        Entity entity = original.call(world, id);
        if (!Configuration.fixFishingHook) return entity;
        if (entity == null) return null;

        if (entity instanceof ArmorStandEntity armorStand) {
            if (armorStand.getId() == this.getId() + 1) {
                return null;
            }
        }

        return entity;
    }
}
