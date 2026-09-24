package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.SoundReceivedEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientSoundEventMixin {

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void iq$onSoundReceived(ClientboundSoundPacket packet, CallbackInfo ci) {
        EventBus.post(new SoundReceivedEvent(packet));
    }
}
