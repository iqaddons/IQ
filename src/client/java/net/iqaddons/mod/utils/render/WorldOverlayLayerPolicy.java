package net.iqaddons.mod.utils.render;

import net.minecraft.client.renderer.rendertype.OutputTarget;

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

    /**
     * Keeps custom geometry in the same render target as vanilla entity and line overlays.
     * That target retains the terrain depth needed by ordinary (non-see-through) markers.
     */
    public static OutputTarget outputTarget() {
        return OutputTarget.ITEM_ENTITY_TARGET;
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
