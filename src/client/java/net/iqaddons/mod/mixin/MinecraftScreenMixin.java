package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenSetEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftScreenMixin {

    @Shadow
    @Nullable
    public Screen screen;

    @Unique
    private Screen iq$previousScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void iq$capturePreviousScreen(@Nullable Screen newScreen, CallbackInfo ci) {
        iq$previousScreen = screen;
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void iq$postScreenSetEvent(@Nullable Screen newScreen, CallbackInfo ci) {
        EventBus.post(new ScreenSetEvent(iq$previousScreen, newScreen));
    }
}
