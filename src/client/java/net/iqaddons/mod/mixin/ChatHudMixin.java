package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD")
    )
    private void onAddMessageSimple(@NotNull Text message, CallbackInfo ci) {
        EventBus.post(new ChatReceivedEvent(message));
    }
}
