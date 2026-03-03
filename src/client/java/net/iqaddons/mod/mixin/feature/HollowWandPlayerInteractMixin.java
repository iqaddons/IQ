package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(PlayerEntity.class)
public class HollowWandPlayerInteractMixin {

    @Inject(method = "canInteractWithEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void iq$blockHollowWandPlayerEntityInteract(Entity entity, double additionalRange, CallbackInfoReturnable<Boolean> cir) {
        if (shouldBlockInteraction(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, require = 0)
    private void iq$blockHollowWandPlayerInteract(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (hand == Hand.MAIN_HAND && shouldBlockInteraction(entity)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    private boolean shouldBlockInteraction(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return false;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self != client.player) {
            return false;
        }

        if (!PhaseFourConfig.hollowWandNoPlayerInteract || KuudraStateManager.get().phase() != KuudraPhase.BOSS) {
            return false;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) {
            return false;
        }

        String itemName = StringUtils.stripFormatting(stack.getName().getString()).toLowerCase(Locale.ROOT);
        return itemName.contains("hollow wand");
    }
}
