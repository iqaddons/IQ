package net.iqaddons.mod.test;

import net.iqaddons.mod.gametest.MigrationSmokeTest;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures NanoVG too: it draws to the window after Minecraft's screenshot texture. */
@Mixin(value = Minecraft.class, priority = 900)
public class SurfaceCaptureMixin {
    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V"))
    private void iqtest$captureSurface(boolean tick, CallbackInfo ci) {
        if (MigrationSmokeTest.surfaceCapture == null) return;
        Minecraft client = (Minecraft) (Object) this;
        int width = client.getWindow().getWidth();
        int height = client.getWindow().getHeight();
        var pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            STBImageWrite.stbi_flip_vertically_on_write(true);
            if (!STBImageWrite.stbi_write_png(MigrationSmokeTest.surfaceCapture, width, height, 4, pixels, width * 4)) {
                throw new AssertionError("Could not save NanoVG screenshot");
            }
        } finally {
            STBImageWrite.stbi_flip_vertically_on_write(false);
            MemoryUtil.memFree(pixels);
            MigrationSmokeTest.surfaceCapture = null;
        }
    }
}
