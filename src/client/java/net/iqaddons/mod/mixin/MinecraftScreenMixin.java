package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenSetEvent;
import net.iqaddons.mod.hud.nano.IqHudNanoRenderer;
import net.iqaddons.mod.nanovg.IqNanoVg;
import net.iqaddons.mod.nanovg.IqNanoVgConfiguration;
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
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.onScreenChanged(iq$previousScreen, newScreen);
        EventBus.post(new ScreenSetEvent(iq$previousScreen, newScreen));
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V",
                    shift = At.Shift.AFTER
            )
    )
    private void iq$renderNanoVgAfterBlit(boolean tick, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.tracePresent("begin IQ NanoVG hook");
        boolean delayed = IqNanoVgConfiguration.mode().preparesResources() && IqNanoVg.shouldSkipRenderFrame();
        if (!delayed) {
            IqNanoVgConfiguration.mode().runScreen(() -> IqNanoVg.renderCurrentScreen(client));
            IqNanoVgConfiguration.mode().runHud(() -> IqHudNanoRenderer.renderCurrentHud(client));
        }
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.tracePresent("end IQ NanoVG hook");
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void iq$traceBeforePresent(boolean tick, CallbackInfo ci) {
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.tracePresent("before GpuSurface.present()");
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void iq$traceAfterPresent(boolean tick, CallbackInfo ci) {
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.tracePresent("after GpuSurface.present()");
    }

}
