package net.iqaddons.mod.screen.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RoundedPaneRenderState implements GuiElementRenderState {

    private final ScreenRectangle drawBounds;
    private final ScreenRectangle fillBounds;
    private final ScreenRectangle clip;
    private final float topLeftRadius;
    private final float topRightRadius;
    private final float bottomRightRadius;
    private final float bottomLeftRadius;
    private final int colorA;
    private final int colorB;
    private final boolean gradient;
    private final int softFadeShape;
    private final float glowWidth;
    private final float glowStrength;

    public RoundedPaneRenderState(
            ScreenRectangle drawBounds,
            ScreenRectangle fillBounds,
            ScreenRectangle clip,
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
    ) {
        this.drawBounds = drawBounds;
        this.fillBounds = fillBounds;
        this.clip = clip;
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
        this.bottomRightRadius = bottomRightRadius;
        this.bottomLeftRadius = bottomLeftRadius;
        this.colorA = colorA;
        this.colorB = colorB;
        this.gradient = gradient;
        this.softFadeShape = softFadeShape;
        this.glowWidth = glowWidth;
        this.glowStrength = glowStrength;
    }

    public static RoundedPaneRenderState solid(
            ScreenRectangle rectangle,
            ScreenRectangle clip,
            float radius,
            int color
    ) {
        return new RoundedPaneRenderState(
                rectangle, rectangle, clip, radius, radius, radius, radius, color, color, false, 0, 0f, 0f
        );
    }

    public static RoundedPaneRenderState withGlow(
            ScreenRectangle drawBounds,
            ScreenRectangle fillBounds,
            ScreenRectangle clip,
            float radius,
            int fillColor,
            int glowColor,
            float glowWidth,
            float glowStrength
    ) {
        return new RoundedPaneRenderState(
                drawBounds, fillBounds, clip, radius, radius, radius, radius,
                fillColor, glowColor, false, 0, glowWidth, glowStrength
        );
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer vertexConsumer) {
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return clip;
    }

    @Override
    public @NotNull ScreenRectangle bounds() {
        return drawBounds;
    }

    public ScreenRectangle fillBounds() {
        return fillBounds;
    }

    public ScreenRectangle clip() {
        return clip;
    }

    public float topLeftRadius() {
        return topLeftRadius;
    }

    public float topRightRadius() {
        return topRightRadius;
    }

    public float bottomRightRadius() {
        return bottomRightRadius;
    }

    public float bottomLeftRadius() {
        return bottomLeftRadius;
    }

    public int colorA() {
        return colorA;
    }

    public int colorB() {
        return colorB;
    }

    public boolean gradient() {
        return gradient;
    }

    public int softFadeShape() {
        return softFadeShape;
    }

    public float glowWidth() {
        return glowWidth;
    }

    public float glowStrength() {
        return glowStrength;
    }
}
