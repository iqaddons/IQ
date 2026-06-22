package net.iqaddons.mod.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private SubmitNodeStorage submitNodeStorage;

    @Unique
    private MultiBufferSource.BufferSource iq$immediate;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void iq$onWorldRender(
            GraphicsResourceAllocator allocator,
            @NotNull DeltaTracker tickCounter,
            boolean renderBlockOutline,
            CameraRenderState camera,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice terrainFog,
            Vector4f fogColor,
            boolean shouldRenderSky,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci
    ) {
        PoseStack matrices = new PoseStack();
        matrices.mulPose(modelViewMatrix);

        EventBus.post(new WorldRenderEvent(
                iq$getImmediate(),
                submitNodeStorage,
                matrices,
                camera,
                tickCounter
        ));
    }

    @Unique
    private MultiBufferSource.BufferSource iq$getImmediate() {
        if (iq$immediate == null) {
            iq$immediate = MultiBufferSource.immediate(new ByteBufferBuilder(1536 * 20));
        }
        return iq$immediate;
    }
}