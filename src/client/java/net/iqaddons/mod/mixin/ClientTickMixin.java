package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void iq$onTick(CallbackInfo ci) {
        EventBus.post(ClientTickEvent.create((MinecraftClient) (Object) this));
    }
}