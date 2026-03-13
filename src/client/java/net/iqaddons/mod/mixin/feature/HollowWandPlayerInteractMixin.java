package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(ClientPlayerInteractionManager.class)
public class HollowWandPlayerInteractMixin {

    @Unique
    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void iq$blockHollowWandPlayerInteract(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (shouldBlockInteraction(player, entity, hand)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true, require = 0)
    private void iq$blockHollowWandPlayerInteractAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (shouldBlockInteraction(player, entity, hand)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void iq$blockHollowWandPlayerAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (shouldBlockInteraction(player, target, Hand.MAIN_HAND)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean shouldBlockInteraction(PlayerEntity player, Entity entity, Hand hand) {
        if (hand != Hand.MAIN_HAND || !(entity instanceof PlayerEntity)) {
            return false;
        }

        if (client.player == null || client.world == null || client.interactionManager == null || player != client.player) {
            return false;
        }

        if (!PhaseFourConfig.hollowWandNoPlayerInteract || KuudraStateManager.get().phase() != KuudraPhase.BOSS) {
            return false;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            return false;
        }

        String itemName = StringUtils.stripFormatting(stack.getName().getString()).toLowerCase(Locale.ROOT);
        return itemName.contains("hollow wand");
    }
}
