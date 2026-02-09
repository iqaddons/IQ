package net.iqaddons.mod.mixin;

import net.iqaddons.mod.hud.HudManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class HudClickMixin {

    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (HudManager.get().handleClick(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
        }
    }
}