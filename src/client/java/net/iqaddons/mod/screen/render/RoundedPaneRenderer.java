package net.iqaddons.mod.screen.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class RoundedPaneRenderer {

    private static final String MOD_ASSETS = "iq";
    private static final int PANE_UNIFORM_SIZE = 80;

    private static final RenderPipeline PANE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iq_rounded_pane"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ASSETS, "core/iq_screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ASSETS, "core/iq_rounded_pane"))
            .withUniform("PaneInfo", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

    private static DynamicUniformStorage<PaneUniform> paneUniforms;

    private RoundedPaneRenderer() {
    }

    public static void submit(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color, float radius) {
        submit(graphics, x, y, w, h, color, radius, radius, radius, radius, 0f, 0f, color);
    }

    public static void submit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int w,
            int h,
            int color,
            float radius,
            float glowWidth,
            float glowStrength
    ) {
        submit(graphics, x, y, w, h, color, radius, radius, radius, radius, glowWidth, glowStrength, color);
    }

    public static void submit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int w,
            int h,
            int fillColor,
            float radius,
            float glowWidth,
            float glowStrength,
            int glowColor
    ) {
        submit(graphics, x, y, w, h, fillColor, radius, radius, radius, radius, glowWidth, glowStrength, glowColor);
    }

    public static void submit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int w,
            int h,
            int fillColor,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            float glowWidth,
            float glowStrength,
            int glowColor
    ) {
        boolean hasFill = ((fillColor >>> 24) & 0xFF) != 0;
        boolean hasGlow = glowWidth > 0f && glowStrength > 0f && ((glowColor >>> 24) & 0xFF) != 0;
        if (w <= 0 || h <= 0 || (!hasFill && !hasGlow)) {
            return;
        }

        int pad = hasGlow ? Math.max(0, (int) Math.ceil(glowWidth)) : 0;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle fillBounds = new ScreenRectangle(x, y, w, h).transformAxisAligned(pose);
        ScreenRectangle drawBounds = pad == 0
                ? fillBounds
                : new ScreenRectangle(x - pad, y - pad, w + pad * 2, h + pad * 2).transformAxisAligned(pose);
        float scale = transformedScale(w, h, fillBounds);

        ScreenRectangle activeScissor = graphics.scissorStack.peek();
        ScreenRectangle clip = activeScissor == null
                ? drawBounds
                : drawBounds.intersection(activeScissor);
        if (clip == null || clip.width() <= 0 || clip.height() <= 0) {
            return;
        }

        float maxR = Math.min(w, h) * 0.5f;
        float tl = Math.max(0f, Math.min(topLeftRadius, maxR)) * scale;
        float tr = Math.max(0f, Math.min(topRightRadius, maxR)) * scale;
        float br = Math.max(0f, Math.min(bottomRightRadius, maxR)) * scale;
        float bl = Math.max(0f, Math.min(bottomLeftRadius, maxR)) * scale;
        float scaledGlowWidth = Math.max(0f, glowWidth) * scale;
        graphics.guiRenderState.addGuiElement(
                hasGlow
                        ? RoundedPaneRenderState.withGlow(
                        drawBounds, fillBounds, clip, tl, tr, br, bl, fillColor, glowColor, scaledGlowWidth, glowStrength)
                        : RoundedPaneRenderState.solid(fillBounds, clip, tl, tr, br, bl, fillColor)
        );
    }

    public static void render(RoundedPaneRenderState state) {
        GpuTextureView target = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        if (target == null) {
            return;
        }
        render(
                target,
                state.fillBounds().left(),
                state.fillBounds().top(),
                state.fillBounds().width(),
                state.fillBounds().height(),
                state.bounds().left(),
                state.bounds().top(),
                state.bounds().width(),
                state.bounds().height(),
                state.clip().left(),
                state.clip().top(),
                state.clip().width(),
                state.clip().height(),
                state.topLeftRadius(),
                state.topRightRadius(),
                state.bottomRightRadius(),
                state.bottomLeftRadius(),
                state.colorA(),
                state.colorB(),
                state.gradient(),
                state.softFadeShape(),
                state.glowWidth(),
                state.glowStrength()
        );
    }

    public static void render(
            GpuTextureView target,
            int fillLeftGui,
            int fillTopGui,
            int fillWidthGui,
            int fillHeightGui,
            int drawLeftGui,
            int drawTopGui,
            int drawWidthGui,
            int drawHeightGui,
            int clipLeftGui,
            int clipTopGui,
            int clipWidthGui,
            int clipHeightGui,
            float topLeftRadiusGui,
            float topRightRadiusGui,
            float bottomRightRadiusGui,
            float bottomLeftRadiusGui,
            int colorA,
            int colorB,
            boolean gradient,
            int softFadeShape,
            float glowWidth,
            float glowStrength
    ) {
        if (fillWidthGui <= 0 || fillHeightGui <= 0 || clipWidthGui <= 0 || clipHeightGui <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int scale = minecraft.getWindow().getGuiScale();
        int fillLeft = fillLeftGui * scale;
        int fillBottom = minecraft.getWindow().getHeight() - (fillTopGui + fillHeightGui) * scale;
        int fillWidth = fillWidthGui * scale;
        int fillHeight = fillHeightGui * scale;

        int drawLeft = drawLeftGui * scale;
        int drawBottom = minecraft.getWindow().getHeight() - (drawTopGui + drawHeightGui) * scale;
        int drawWidth = drawWidthGui * scale;
        int drawHeight = drawHeightGui * scale;

        int clipLeft = clipLeftGui * scale;
        int clipBottom = minecraft.getWindow().getHeight() - (clipTopGui + clipHeightGui) * scale;
        int clipWidth = clipWidthGui * scale;
        int clipHeight = clipHeightGui * scale;

        int scissorLeft = Math.max(drawLeft, clipLeft);
        int scissorBottom = Math.max(drawBottom, clipBottom);
        int scissorRight = Math.min(drawLeft + drawWidth, clipLeft + clipWidth);
        int scissorTop = Math.min(drawBottom + drawHeight, clipBottom + clipHeight);
        int scissorWidth = scissorRight - scissorLeft;
        int scissorHeight = scissorTop - scissorBottom;
        if (scissorWidth <= 0 || scissorHeight <= 0) {
            return;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var paneInfo = uniforms().writeUniform(new PaneUniform(
                fillLeft,
                fillBottom,
                fillWidth,
                fillHeight,
                topLeftRadiusGui * scale,
                topRightRadiusGui * scale,
                bottomRightRadiusGui * scale,
                bottomLeftRadiusGui * scale,
                colorA,
                colorB,
                gradient,
                softFadeShape,
                glowWidth * scale,
                glowStrength
        ));

        try (RenderPass pass = encoder.createRenderPass(() -> "IQ rounded GUI pane", target, OptionalInt.empty())) {
            pass.setPipeline(PANE_PIPELINE);
            pass.enableScissor(scissorLeft, scissorBottom, scissorWidth, scissorHeight);
            pass.setUniform("PaneInfo", paneInfo);
            pass.draw(0, 3);
        }
    }

    public static void endFrame() {
        if (paneUniforms != null) {
            paneUniforms.endFrame();
        }
    }

    private static DynamicUniformStorage<PaneUniform> uniforms() {
        if (paneUniforms == null) {
            paneUniforms = new DynamicUniformStorage<>("IQ rounded pane uniforms", PANE_UNIFORM_SIZE, 128);
        }
        return paneUniforms;
    }

    private static float transformedScale(int sourceW, int sourceH, ScreenRectangle transformed) {
        float scaleX = transformed.width() / Math.max(1f, sourceW);
        float scaleY = transformed.height() / Math.max(1f, sourceH);
        return Math.max(0.01f, Math.min(scaleX, scaleY));
    }

    private record PaneUniform(
            float left,
            float bottom,
            float width,
            float height,
            float topLeftRadius,
            float topRightRadius,
            float bottomRightRadius,
            float bottomLeftRadius,
            int colorA,
            int colorB,
            boolean gradient,
            int softFadeShape,
            float glowWidth,
            float glowStrength
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putVec4(left, bottom, width, height)
                    .putVec4(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius)
                    .putVec4(
                            ((colorA >>> 16) & 0xFF) / 255.0f,
                            ((colorA >>> 8) & 0xFF) / 255.0f,
                            (colorA & 0xFF) / 255.0f,
                            ((colorA >>> 24) & 0xFF) / 255.0f
                    )
                    .putVec4(
                            ((colorB >>> 16) & 0xFF) / 255.0f,
                            ((colorB >>> 8) & 0xFF) / 255.0f,
                            (colorB & 0xFF) / 255.0f,
                            ((colorB >>> 24) & 0xFF) / 255.0f
                    )
                    .putVec4(gradient ? 1.0f : 0.0f, (float) softFadeShape, glowWidth, glowStrength);
        }
    }
}
