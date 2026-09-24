package net.iqaddons.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.iqaddons.mod.utils.render.WorldOverlayLayerPolicy;
import net.iqaddons.mod.utils.render.WorldOverlayPhaseSubmitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionMixin implements WorldOverlayPhaseSubmitter {

    @Override
    public void iq$submitCustomGeometry(
            @NotNull PoseStack matrices,
            @NotNull RenderType renderType,
            @NotNull SubmitNodeCollector.CustomGeometryRenderer renderer,
            @NotNull WorldOverlayLayerPolicy.RenderPhase phase
    ) {
        var submit = new CustomFeatureRenderer.Submit(matrices.last().copy(), renderType, renderer);
        iq$phase(phase).submit(submit);
    }

    @Override
    public void iq$submitText(
            @NotNull PoseStack matrices,
            float x,
            float y,
            @NotNull FormattedCharSequence text,
            boolean dropShadow,
            @NotNull Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor,
            @NotNull WorldOverlayLayerPolicy.RenderPhase phase
    ) {
        var submit = new TextFeatureRenderer.Submit(
                new Matrix4f(matrices.last().pose()), x, y, text, dropShadow,
                displayMode, lightCoords, color, backgroundColor, outlineColor
        );
        iq$phase(phase).submit(submit);
    }

    private @NotNull SimpleFeatureRenderPhase iq$phase(@NotNull WorldOverlayLayerPolicy.RenderPhase phase) {
        SubmitNodeCollection self = (SubmitNodeCollection) (Object) this;
        return switch (phase) {
            case AFTER_TRANSLUCENT_TERRAIN -> self.afterTerrain;
            case ALWAYS_ON_TOP -> self.alwaysOnTop;
        };
    }
}
