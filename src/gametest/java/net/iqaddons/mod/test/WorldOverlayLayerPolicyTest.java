package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.utils.render.WorldOverlayPhaseSubmitter;
import net.iqaddons.mod.utils.render.WorldOverlayLayerPolicy;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.network.chat.Component;

/** Guards the depth contract used by supplies, beacons and waypoint overlays. */
public final class WorldOverlayLayerPolicyTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        assertMode(false, WorldOverlayLayerPolicy.DepthMode.DEPTH_TESTED);
        assertMode(true, WorldOverlayLayerPolicy.DepthMode.ALWAYS_VISIBLE);
        assertPhase(false, WorldOverlayLayerPolicy.RenderPhase.AFTER_TRANSLUCENT_TERRAIN);
        assertPhase(true, WorldOverlayLayerPolicy.RenderPhase.ALWAYS_ON_TOP);
        if (WorldOverlayLayerPolicy.outputTarget() != OutputTarget.ITEM_ENTITY_TARGET) {
            throw new AssertionError("World overlays must use the item/entity target used by always-visible geometry");
        }
        assertOverlayPipeline("filled", WorldRenderUtils.Pipelines.filledCull, DepthStencilState.DEFAULT.depthTest());
        assertOverlayPipeline("filled see-through", WorldRenderUtils.Pipelines.filledNoCull, CompareOp.ALWAYS_PASS);
        assertOverlayPipeline("outline", WorldRenderUtils.Pipelines.outlineCull, DepthStencilState.DEFAULT.depthTest());
        assertOverlayPipeline("outline see-through", WorldRenderUtils.Pipelines.outlineNoCull, CompareOp.ALWAYS_PASS);
        assertOverlayPipeline("circle", WorldRenderUtils.Pipelines.circleFilledCull, DepthStencilState.DEFAULT.depthTest());
        assertOverlayPipeline("circle see-through", WorldRenderUtils.Pipelines.circleFilledNoCull, CompareOp.ALWAYS_PASS);
        assertOverlayPipeline("circle outline", WorldRenderUtils.Pipelines.circleOutlineCull, DepthStencilState.DEFAULT.depthTest());
        assertOverlayPipeline("circle outline see-through", WorldRenderUtils.Pipelines.circleOutlineNoCull, CompareOp.ALWAYS_PASS);
        assertPhaseRouting();
    }

    private static void assertMode(boolean throughWalls, WorldOverlayLayerPolicy.DepthMode expected) {
        WorldOverlayLayerPolicy.DepthMode actual = WorldOverlayLayerPolicy.depthMode(throughWalls);
        if (actual != expected) {
            throw new AssertionError("throughWalls=" + throughWalls + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertPhase(boolean throughWalls, WorldOverlayLayerPolicy.RenderPhase expected) {
        WorldOverlayLayerPolicy.RenderPhase actual = WorldOverlayLayerPolicy.renderPhase(throughWalls);
        if (actual != expected) {
            throw new AssertionError("throughWalls=" + throughWalls + ": expected phase " + expected + ", got " + actual);
        }
    }

    private static void assertOverlayPipeline(
            String name,
            com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            CompareOp expectedDepthTest
    ) {
        if (pipeline.isCull()) {
            throw new AssertionError(name + " overlay must render both faces");
        }
        if (pipeline.getColorTargetState().blendFunction().isEmpty()) {
            throw new AssertionError(name + " overlay must use alpha blending");
        }
        var depthState = pipeline.getDepthStencilState();
        if (depthState.depthTest() != expectedDepthTest) {
            throw new AssertionError(name + ": expected depth test " + expectedDepthTest
                    + ", got " + depthState.depthTest());
        }
        if (depthState.writeDepth()) {
            throw new AssertionError(name + " overlay must not write depth");
        }
    }

    private static void assertPhaseRouting() {
        PoseStack matrices = new PoseStack();

        SubmitNodeCollection normalGeometry = new SubmitNodeCollection();
        ((WorldOverlayPhaseSubmitter) (Object) normalGeometry).iq$submitCustomGeometry(
                matrices,
                WorldRenderUtils.Layers.BoxFilled,
                (pose, vertices) -> { },
                WorldOverlayLayerPolicy.RenderPhase.AFTER_TRANSLUCENT_TERRAIN
        );
        assertOnlyAfterTerrain(normalGeometry, "normal overlay geometry");

        SubmitNodeCollection throughWallGeometry = new SubmitNodeCollection();
        ((WorldOverlayPhaseSubmitter) (Object) throughWallGeometry).iq$submitCustomGeometry(
                matrices,
                WorldRenderUtils.Layers.BoxFilledNoCull,
                (pose, vertices) -> { },
                WorldOverlayLayerPolicy.RenderPhase.ALWAYS_ON_TOP
        );
        assertOnlyAlwaysOnTop(throughWallGeometry, "through-wall overlay geometry");

        var text = Component.literal("IQ overlay phase test").getVisualOrderText();
        SubmitNodeCollection normalText = new SubmitNodeCollection();
        ((WorldOverlayPhaseSubmitter) (Object) normalText).iq$submitText(
                matrices, 0, 0, text, true, Font.DisplayMode.NORMAL,
                0, 0xFFFFFFFF, 0, 0,
                WorldOverlayLayerPolicy.RenderPhase.AFTER_TRANSLUCENT_TERRAIN
        );
        assertOnlyAfterTerrain(normalText, "normal overlay text");

        SubmitNodeCollection throughWallText = new SubmitNodeCollection();
        ((WorldOverlayPhaseSubmitter) (Object) throughWallText).iq$submitText(
                matrices, 0, 0, text, true, Font.DisplayMode.SEE_THROUGH,
                0, 0xFFFFFFFF, 0, 0,
                WorldOverlayLayerPolicy.RenderPhase.ALWAYS_ON_TOP
        );
        assertOnlyAlwaysOnTop(throughWallText, "through-wall overlay text");
    }

    private static void assertOnlyAfterTerrain(SubmitNodeCollection nodes, String name) {
        if (nodes.afterTerrain.isEmpty() || !nodes.alwaysOnTop.isEmpty()) {
            throw new AssertionError(name + " must enter only the after-terrain phase");
        }
    }

    private static void assertOnlyAlwaysOnTop(SubmitNodeCollection nodes, String name) {
        if (!nodes.afterTerrain.isEmpty() || nodes.alwaysOnTop.isEmpty()) {
            throw new AssertionError(name + " must enter only the always-on-top phase");
        }
    }
}
