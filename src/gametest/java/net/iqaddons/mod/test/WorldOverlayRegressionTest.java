package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.kuudra.waypoints.PileWaypointsFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.spot.PileLocation;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldOverlayBatch;
import net.iqaddons.mod.utils.render.WorldOverlayLayerPolicy;
import net.iqaddons.mod.utils.render.WorldOverlayPhaseSubmitter;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Guards camera-facing transforms and through-wall Pile rendering on 26.3. */
public final class WorldOverlayRegressionTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        AtomicBoolean completed = new AtomicBoolean();
        context.runOnClient(client -> {
            List<String> failures = new ArrayList<>();
            verifyCameraFacingTransforms(failures);
            verifyNoDepthOitOverride(failures);
            verifyPileVisibility(client.getDeltaTracker(), failures);
            if (!failures.isEmpty()) {
                throw new AssertionError(String.join("; ", failures));
            }
            completed.set(true);
        });
        context.waitFor(client -> completed.get());
    }

    private static void verifyCameraFacingTransforms(List<String> failures) {
        CameraRenderState camera = new CameraRenderState();
        camera.pos = new Vec3(10.0, 20.0, 30.0);
        camera.orientation.identity().rotateXYZ(0.25f, -0.45f, 0.15f);
        camera.viewRotationMatrix.identity().rotateXYZ(-0.7f, 0.8f, -0.35f);

        Capture capture = new Capture();
        OrderedSubmitNodeCollector collector = emptyCollector();
        WorldOverlayBatch batch = WorldOverlayBatch.begin(camera.pos, capture);
        try {
            WorldRenderUtils.drawText(
                    new PoseStack(), collector, camera, new Vec3(13.0, 24.0, 35.0),
                    Component.literal("orientation"), 0.05f, true, RenderColor.white
            );
            WorldRenderUtils.drawBillboardSquareOutline(
                    new PoseStack(), collector, camera, new Vec3(13.0, 24.0, 35.0),
                    1.0f, true, RenderColor.white
            );
        } finally {
            WorldOverlayBatch.end(batch);
        }

        Matrix4f expectedText = new Matrix4f()
                .translate(3.0f, 4.0f, 5.0f)
                .rotate(camera.orientation)
                .scale(0.05f, -0.05f, 0.05f);
        Matrix4f expectedBillboard = new Matrix4f()
                .translate(3.0f, 4.0f, 5.0f)
                .rotate(camera.orientation);
        if (capture.textPose == null || !matrixClose(capture.textPose, expectedText)) {
            failures.add("world text must face the camera using CameraRenderState.orientation");
        }
        if (capture.geometryPoses.isEmpty() || !matrixClose(capture.geometryPoses.getFirst(), expectedBillboard)) {
            failures.add("billboard geometry must face the camera using CameraRenderState.orientation");
        }
    }

    private static void verifyNoDepthOitOverride(List<String> failures) {
        if (WorldRenderUtils.Layers.BoxFilledNoCull.prepare().oitPipelineSet() != null
                || WorldRenderUtils.Layers.BoxOutlineNoCull.prepare().oitPipelineSet() != null
                || WorldRenderUtils.Layers.CircleFilledNoCull.prepare().oitPipelineSet() != null
                || WorldRenderUtils.Layers.CircleOutlineNoCull.prepare().oitPipelineSet() != null) {
            failures.add("through-wall geometry must not replace ALWAYS_PASS with a depth-tested OIT pipeline");
        }
    }

    private static void verifyPileVisibility(DeltaTracker deltaTracker, List<String> failures) {
        SupplyStateManager state = SupplyStateManager.get();
        List<PileLocation> previousPiles = List.copyOf(state.getRemainingPiles());
        boolean previousNames = PhaseOneConfig.PileWaypointsConfig.pileWaypointNames;
        boolean previousPlaceArea = PhaseOneConfig.PileWaypointsConfig.placeAreaHitbox;
        try {
            state.reloadPileLocations(List.of(new PileLocation("test", new Vec3(4.0, 79.0, 6.0), 0)));
            PhaseOneConfig.PileWaypointsConfig.pileWaypointNames = false;
            PhaseOneConfig.PileWaypointsConfig.placeAreaHitbox = false;

            Capture capture = new Capture();
            WorldOverlayBatch batch = WorldOverlayBatch.begin(Vec3.ZERO, capture);
            try {
                CameraRenderState camera = new CameraRenderState();
                camera.pos = Vec3.ZERO;
                WorldRenderEvent event = new WorldRenderEvent(
                        emptyCollector(), null, new PoseStack(), camera,
                        deltaTracker
                );
                invokePileRender(new PileWaypointsFeature(), event);
                batch.flush(emptyCollector());
            } finally {
                WorldOverlayBatch.end(batch);
            }
            if (capture.geometryPhases.isEmpty()
                    || capture.geometryPhases.stream().anyMatch(
                    phase -> phase != WorldOverlayLayerPolicy.RenderPhase.ALWAYS_ON_TOP)) {
                failures.add("Pile box and beam must be submitted as through-wall geometry");
            }
        } finally {
            state.reloadPileLocations(previousPiles);
            PhaseOneConfig.PileWaypointsConfig.pileWaypointNames = previousNames;
            PhaseOneConfig.PileWaypointsConfig.placeAreaHitbox = previousPlaceArea;
        }
    }

    private static void invokePileRender(PileWaypointsFeature feature, WorldRenderEvent event) {
        try {
            var method = PileWaypointsFeature.class.getDeclaredMethod("onRender", WorldRenderEvent.class);
            method.setAccessible(true);
            method.invoke(feature, event);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static OrderedSubmitNodeCollector emptyCollector() {
        return (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
                OrderedSubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{OrderedSubmitNodeCollector.class},
                (proxy, method, args) -> null
        );
    }

    private static boolean matrixClose(Matrix4f actual, Matrix4f expected) {
        float epsilon = 1.0E-5f;
        return Math.abs(actual.m00() - expected.m00()) < epsilon
                && Math.abs(actual.m01() - expected.m01()) < epsilon
                && Math.abs(actual.m02() - expected.m02()) < epsilon
                && Math.abs(actual.m10() - expected.m10()) < epsilon
                && Math.abs(actual.m11() - expected.m11()) < epsilon
                && Math.abs(actual.m12() - expected.m12()) < epsilon
                && Math.abs(actual.m20() - expected.m20()) < epsilon
                && Math.abs(actual.m21() - expected.m21()) < epsilon
                && Math.abs(actual.m22() - expected.m22()) < epsilon
                && Math.abs(actual.m30() - expected.m30()) < epsilon
                && Math.abs(actual.m31() - expected.m31()) < epsilon
                && Math.abs(actual.m32() - expected.m32()) < epsilon;
    }

    private static final class Capture implements WorldOverlayPhaseSubmitter {
        private final List<Matrix4f> geometryPoses = new ArrayList<>();
        private final List<WorldOverlayLayerPolicy.RenderPhase> geometryPhases = new ArrayList<>();
        private Matrix4f textPose;

        @Override
        public void iq$submitCustomGeometry(
                PoseStack matrices,
                RenderType renderType,
                SubmitNodeCollector.CustomGeometryRenderer renderer,
                WorldOverlayLayerPolicy.RenderPhase phase
        ) {
            geometryPoses.add(new Matrix4f(matrices.last().pose()));
            geometryPhases.add(phase);
        }

        @Override
        public void iq$submitText(
                PoseStack matrices, float x, float y, FormattedCharSequence text, boolean dropShadow,
                Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor,
                WorldOverlayLayerPolicy.RenderPhase phase
        ) {
            textPose = new Matrix4f(matrices.last().pose());
        }
    }
}
