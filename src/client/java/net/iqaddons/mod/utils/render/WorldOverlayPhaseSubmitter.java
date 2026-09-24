package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

/** Routes IQ overlays into render phases that execute after translucent terrain. */
public interface WorldOverlayPhaseSubmitter {

    void iq$submitCustomGeometry(
            @NotNull PoseStack matrices,
            @NotNull RenderType renderType,
            @NotNull SubmitNodeCollector.CustomGeometryRenderer renderer,
            @NotNull WorldOverlayLayerPolicy.RenderPhase phase
    );

    void iq$submitText(
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
    );
}
