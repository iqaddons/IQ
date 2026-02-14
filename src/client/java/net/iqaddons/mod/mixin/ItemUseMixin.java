package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ItemUseMixin {

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void iq$onUseItem(@NotNull PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = player.getStackInHand(hand);
        ItemUseEvent event = EventBus.post(
                new ItemUseEvent(hand, stack.copy())
        );

        if (event.isCancelled()) {
            cir.cancel();
        }
    }
}
