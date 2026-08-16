package net.iqaddons.mod.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.iqaddons.mod.screen.render.RoundedPaneRenderState;
import net.iqaddons.mod.screen.render.RoundedPaneRenderer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

    @Unique
    private List<StructureEntry> iq$structureEntries;

    @Unique
    private boolean iq$drawingSubrange;

    @Shadow
    @Final
    private List<?> meshesToDraw;

    @Shadow
    @Final
    private List<?> draws;

    @Shadow
    private BufferBuilder bufferBuilder;

    @Shadow
    private RenderPipeline previousPipeline;

    @Shadow
    private TextureSetup previousTextureSetup;

    @Shadow
    private ScreenRectangle previousScissorArea;

    @Shadow
    private void recordMesh(BufferBuilder bufferBuilder, RenderPipeline pipeline, TextureSetup textureSetup, ScreenRectangle scissorArea) {
        throw new AssertionError();
    }

    @Shadow
    private void executeDrawRange(
            Supplier<String> name,
            RenderTarget target,
            GpuBufferSlice fogBuffer,
            GpuBufferSlice dynamicTransforms,
            GpuBuffer indexBuffer,
            VertexFormat.IndexType indexType,
            int start,
            int end
    ) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void iq$initStructureQueue(CallbackInfo ci) {
        this.iq$structureEntries = new ArrayList<>();
    }

    @Inject(method = "addElementToMesh", at = @At("HEAD"), cancellable = true)
    private void iq$captureRoundedPanes(GuiElementRenderState state, CallbackInfo ci) {
        if (!(state instanceof RoundedPaneRenderState)) {
            return;
        }
        iq$flushMesh();
        this.iq$structureEntries.add(new StructureEntry(this.meshesToDraw.size(), state));
        ci.cancel();
    }

    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void iq$interleaveRoundedPanes(
            Supplier<String> name,
            RenderTarget target,
            GpuBufferSlice fogBuffer,
            GpuBufferSlice dynamicTransforms,
            GpuBuffer indexBuffer,
            VertexFormat.IndexType indexType,
            int start,
            int end,
            CallbackInfo ci
    ) {
        if (this.iq$drawingSubrange || this.iq$structureEntries.isEmpty()) {
            return;
        }

        boolean lastRange = end >= this.draws.size();
        boolean relevant = false;
        for (StructureEntry entry : this.iq$structureEntries) {
            if (entry.meshIndex >= start && (entry.meshIndex < end || (lastRange && entry.meshIndex <= end))) {
                relevant = true;
                break;
            }
        }
        if (!relevant) {
            return;
        }

        ci.cancel();
        int cursor = start;
        for (StructureEntry entry : this.iq$structureEntries) {
            boolean inRange = entry.meshIndex >= start
                    && (entry.meshIndex < end || (lastRange && entry.meshIndex <= end));
            if (!inRange) {
                continue;
            }
            if (cursor < entry.meshIndex) {
                iq$drawSubrange(name, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, cursor, entry.meshIndex);
            }
            if (entry.state instanceof RoundedPaneRenderState pane) {
                RoundedPaneRenderer.render(pane);
            }
            cursor = Math.max(cursor, entry.meshIndex);
        }
        if (cursor < end) {
            iq$drawSubrange(name, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, cursor, end);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void iq$renderPanesWhenNoDraws(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (!this.draws.isEmpty() || this.iq$structureEntries.isEmpty()) {
            return;
        }
        for (StructureEntry entry : this.iq$structureEntries) {
            if (entry.state instanceof RoundedPaneRenderState pane) {
                RoundedPaneRenderer.render(pane);
            }
        }
        this.iq$structureEntries.clear();
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void iq$clearStructureEntries(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        this.iq$structureEntries.clear();
    }

    @Inject(method = "endFrame", at = @At("RETURN"))
    private void iq$endRoundedPaneUniforms(CallbackInfo ci) {
        RoundedPaneRenderer.endFrame();
    }

    @Unique
    private void iq$drawSubrange(
            Supplier<String> name,
            RenderTarget target,
            GpuBufferSlice fogBuffer,
            GpuBufferSlice dynamicTransforms,
            GpuBuffer indexBuffer,
            VertexFormat.IndexType indexType,
            int start,
            int end
    ) {
        if (start >= end) {
            return;
        }
        this.iq$drawingSubrange = true;
        try {
            this.executeDrawRange(name, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, start, end);
        } finally {
            this.iq$drawingSubrange = false;
        }
    }

    @Unique
    private void iq$flushMesh() {
        if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
            this.bufferBuilder = null;
            this.previousPipeline = null;
            this.previousTextureSetup = null;
            this.previousScissorArea = null;
        }
    }

    @Unique
    private record StructureEntry(int meshIndex, GuiElementRenderState state) {
    }
}
