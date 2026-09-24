package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ItemUseMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void iq$onUseItem(@NotNull Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        ItemUseEvent event = EventBus.post(
                new ItemUseEvent(hand, stack.copy())
        );

        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
            method = {
                    "interactBlock(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
                    "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$onUseItemOnBlock(
            @NotNull LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemStack stack = player.getItemInHand(hand);
        ItemUseEvent event = EventBus.post(
                new ItemUseEvent(hand, stack.copy())
        );

        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
