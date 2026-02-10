package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(
            method = "onTitle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$onTitle(@NotNull TitleS2CPacket packet, CallbackInfo ci) {
        TitleReceivedEvent event = EventBus.post(new TitleReceivedEvent(packet.text()));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
