package net.iqaddons.mod.mixin;

import net.iqaddons.mod.utils.render.EntityGlowUtil;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(
            method = "getTeamColorValue",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        int customColor = EntityGlowUtil.getGlowColorInt(self.getId());
        if (customColor != -1) {
            cir.setReturnValue(customColor & 0x00FFFFFF);
        }
    }
}
