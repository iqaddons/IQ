package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class HollowWandPlayerInteractMixin {

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void iq$blockHollowWandPlayerInteract(
            @NotNull PlayerEntity player,
            Entity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (shouldBlock(player, entity, hand)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void iq$blockHollowWandPlayerInteractAtLocation(
            @NotNull PlayerEntity player,
            Entity entity,
            EntityHitResult hitResult,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (shouldBlock(player, entity, hand)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    private boolean shouldBlock(@NotNull PlayerEntity player, Entity entity, Hand hand) {
        if (!PhaseFourConfig.hollowWandNoPlayerInteract
                || KuudraStateManager.get().phase() != KuudraPhase.BOSS
                || !(entity instanceof ClientPlayerEntity)
                || hand != Hand.MAIN_HAND) {
            return false;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }

        String itemName = StringUtils.stripFormatting(stack.getName().getString()).toLowerCase();
        return itemName.contains("hollow wand");
    }
}
