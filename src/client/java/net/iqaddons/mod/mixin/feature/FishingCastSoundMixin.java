package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.config.Configuration;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class FishingCastSoundMixin {

    @Inject(
            method = "handleSoundEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteFishingCastSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (Configuration.muteFishingCastSound
                && packet.getSound().is(SoundEvents.FISHING_BOBBER_THROW.location())) {
            ci.cancel();
        }
    }
}
