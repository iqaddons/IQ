package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.Configuration;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class FishingCastSoundMixin {

    @Inject(
            method = "onPlaySound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteFishingCastSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (Configuration.muteFishingCastSound
                && packet.getSound().matchesId(SoundEvents.ENTITY_FISHING_BOBBER_THROW.id())) {
            ci.cancel();
        }
    }
}
