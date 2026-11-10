package net.iqaddons.mod.utils.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;

/** Defines the depth contract for IQ world overlays. */
public final class WorldOverlayLayerPolicy {

    private WorldOverlayLayerPolicy() {
    }

    public static DepthMode depthMode(boolean throughWalls) {
        return throughWalls ? DepthMode.ALWAYS_VISIBLE : DepthMode.DEPTH_TESTED;
    }

    public static RenderPhase renderPhase(boolean throughWalls) {
        return throughWalls ? RenderPhase.ALWAYS_ON_TOP : RenderPhase.AFTER_TRANSLUCENT_TERRAIN;
    }

    /** Uses the same OIT contract as vanilla translucent debug geometry in 26.3. */
    public static OitPipelineSet filledOitPipelines() {
        return RenderPipelines.OIT_DEBUG_FILLED_BOX;
    }

    /** Uses the same OIT contract as vanilla translucent lines in 26.3. */
    public static OitPipelineSet lineOitPipelines() {
        return RenderPipelines.OIT_LINES_TRANSLUCENT;
    }

    public enum DepthMode {
        DEPTH_TESTED,
        ALWAYS_VISIBLE
    }

    public enum RenderPhase {
        AFTER_TRANSLUCENT_TERRAIN,
        ALWAYS_ON_TOP
    }
}
