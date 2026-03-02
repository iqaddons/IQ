package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(MinecraftClient.class)
public class HollowWandPlayerInteractMixin {

    @Unique
    private static final long INTERACT_DELAY_MS = 30L;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void iq$preventHollowWandPlayerInteract(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        if (!PhaseFourConfig.hollowWandNoPlayerInteract
                || !isCrosshairOnPlayer(client.crosshairTarget)
                || !isHollowWand(client.player.getMainHandStack())
        ) return;

        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                client.player.getBlockPos(),
                Direction.DOWN
        ));

        CompletableFuture.delayedExecutor(INTERACT_DELAY_MS, TimeUnit.MILLISECONDS)
                .execute(() -> client.execute(() -> {
                    if (client.player != null && client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    }
                }));

        cir.setReturnValue(false);
    }

    @Unique
    private boolean isCrosshairOnPlayer(HitResult hitResult) {
        return hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PlayerEntity;
    }

    @Unique
    private boolean isHollowWand(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;

        String itemName = StringUtils.stripFormatting(stack.getName().getString()).toLowerCase(Locale.ROOT);
        return itemName.contains("hollow wand");
    }
}