package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenSetEvent;
import net.iqaddons.mod.nanovg.IqNanoVg;
import net.iqaddons.mod.nanovg.IqNanoVgConfiguration;
import net.iqaddons.mod.nanovg.IqNanoVgRenderable;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiScreenMixin {
    @Shadow public abstract Screen screen();
    @Unique private Screen iq$previousScreen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void iq$capturePreviousScreen(@Nullable Screen newScreen, CallbackInfo ci) {
        if (newScreen instanceof IqNanoVgRenderable && !IqNanoVg.isSupported()) {
            Gui gui = (Gui) (Object) this;
            Screen previous = screen();
            gui.setScreen(new AlertScreen(() -> gui.setScreen(previous),
                    Component.literal("IQ requires OpenGL"),
                    Component.literal("Select OpenGL in Minecraft's video settings and restart the game to use the IQ interface.")));
            ci.cancel();
            return;
        }
        iq$previousScreen = screen();
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void iq$postScreenSetEvent(@Nullable Screen newScreen, CallbackInfo ci) {
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.onScreenChanged(iq$previousScreen, newScreen);
        EventBus.post(new ScreenSetEvent(iq$previousScreen, newScreen));
    }
}
