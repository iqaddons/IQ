package net.iqaddons.mod.mixin;

import net.iqaddons.mod.hud.nano.IqHudNanoRenderer;
import net.iqaddons.mod.nanovg.IqNanoVg;
import net.iqaddons.mod.nanovg.IqNanoVgConfiguration;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftScreenMixin {

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V",
                    shift = At.Shift.BEFORE
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
        if (IqNanoVgConfiguration.mode().tracesGl()) {
            IqNanoVg.tracePresent("end IQ NanoVG hook");
            IqNanoVg.tracePresent("before GpuSurface.present()");
        }
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V",
                    shift = At.Shift.AFTER
            )
    )
    private void iq$traceAfterPresent(boolean tick, CallbackInfo ci) {
        if (IqNanoVgConfiguration.mode().tracesGl()) IqNanoVg.tracePresent("after GpuSurface.present()");
    }
}
