package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.PrimitiveTopology;
import lombok.experimental.UtilityClass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@UtilityClass
public class WorldRenderUtils {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final float DEFAULT_OUTLINE_LINE_WIDTH = 1.0f;

    private static void submitWorldGeometry(
            @NotNull PoseStack matrices,
            @NotNull OrderedSubmitNodeCollector consumer,
            @NotNull RenderType layer,
            boolean throughWalls,
            @NotNull WorldOverlayBatch.GeometryWriter writer
    ) {
        WorldOverlayBatch batch = WorldOverlayBatch.active();
        if (batch != null) {
            batch.submit(layer, WorldOverlayLayerPolicy.renderPhase(throughWalls), writer);
            return;
        }
        consumer.submitCustomGeometry(matrices, layer, writer::write);
    }

    private static void submitTransformedWorldGeometry(
            @NotNull PoseStack matrices,
            @NotNull OrderedSubmitNodeCollector consumer,
            @NotNull RenderType layer,
            boolean throughWalls,
            @NotNull SubmitNodeCollector.CustomGeometryRenderer renderer
    ) {
        WorldOverlayBatch batch = WorldOverlayBatch.active();
        if (batch != null) {
            batch.submitImmediate(matrices, layer, throughWalls, renderer, consumer);
            return;
        }
        consumer.submitCustomGeometry(matrices, layer, renderer);
    }

    private static @NotNull RenderType filledLayer(boolean throughWalls) {
        return WorldOverlayLayerPolicy.depthMode(throughWalls) == WorldOverlayLayerPolicy.DepthMode.ALWAYS_VISIBLE
                ? Layers.BoxFilledNoCull : Layers.BoxFilled;
    }

    private static @NotNull RenderType circleFilledLayer(boolean throughWalls) {
        return WorldOverlayLayerPolicy.depthMode(throughWalls) == WorldOverlayLayerPolicy.DepthMode.ALWAYS_VISIBLE
                ? Layers.CircleFilledNoCull : Layers.CircleFilled;
    }

    private static @NotNull RenderType circleOutlineLayer(boolean throughWalls) {
        return WorldOverlayLayerPolicy.depthMode(throughWalls) == WorldOverlayLayerPolicy.DepthMode.ALWAYS_VISIBLE
                ? Layers.CircleOutlineNoCull : Layers.CircleOutline;
    }

    private static @NotNull RenderType outlineLayer(boolean throughWalls) {
        return WorldOverlayLayerPolicy.depthMode(throughWalls) == WorldOverlayLayerPolicy.DepthMode.ALWAYS_VISIBLE
                ? Layers.BoxOutlineNoCull : Layers.BoxOutline;
    }

    public static void drawFilled(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, @NotNull AABB box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.pushPose();

        Vec3 camPos = camera.pos.reverse();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        submitWorldGeometry(matrices, consumer, filledLayer(throughWalls), throughWalls, (entry, buffer) -> {
            drawFilledBox(
                    entry, buffer,
                    (float) box.minX, (float) box.minY, (float) box.minZ,
                    (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                    color.r, color.g, color.b, color.a
            );
        });
        matrices.popPose();
    }

    public static void drawOutline(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, AABB box, boolean throughWalls, @NotNull RenderColor color
    ) {
        drawOutline(matrices, consumer, camera, box, throughWalls, color, DEFAULT_OUTLINE_LINE_WIDTH);
    }

    public static void drawOutline(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, AABB box, boolean throughWalls, @NotNull RenderColor color, float lineWidth
    ) {
        matrices.pushPose();
        Vec3 camPos = camera.pos.reverse();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        float sanitizedLineWidth = sanitizeLineWidth(lineWidth);
        if (!throughWalls) {
            consumer.submitShapeOutline(
                    matrices,
                    Shapes.create(box),
                    outlineLayer(false),
                    color.argb,
                    sanitizedLineWidth,
                    true
            );
        } else {
            submitWorldGeometry(matrices, consumer, outlineLayer(true), true,
                    (entry, buffer) -> drawBox(entry, buffer, box, color.argb, sanitizedLineWidth));
        }
        matrices.popPose();
    }

    public static void drawFilledCircle(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        submitWorldGeometry(matrices, consumer, circleFilledLayer(throughWalls), throughWalls, (entry, buffer) -> {
            for (int i = 0; i < segmentCount; i++) {
                double angle1 = (Math.PI * 2.0) * i / segmentCount;
                double angle2 = (Math.PI * 2.0) * (i + 1) / segmentCount;

                float x1 = (float) (center.x + Math.cos(angle1) * radius);
                float z1 = (float) (center.z + Math.sin(angle1) * radius);

                float x2 = (float) (center.x + Math.cos(angle2) * radius);
                float z2 = (float) (center.z + Math.sin(angle2) * radius);

                buffer.addVertex(entry, (float) center.x, (float) center.y, (float) center.z)
                        .setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(entry, x1, (float) center.y, z1)
                        .setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(entry, x2, (float) center.y, z2)
                        .setColor(color.r, color.g, color.b, color.a);
            }
        });
        matrices.popPose();
    }

    public static void drawCircleOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        submitWorldGeometry(matrices, consumer, circleOutlineLayer(throughWalls), throughWalls, (entry, buffer) -> {
            for (int i = 0; i <= segmentCount; i++) {
                double angle = (Math.PI * 2.0) * i / segmentCount;
                float x = (float) (center.x + Math.cos(angle) * radius);
                float y = (float) center.y;
                float z = (float) (center.z + Math.sin(angle) * radius);

                buffer.addVertex(entry, x, y, z)
                        .setColor(color.r, color.g, color.b, color.a);
            }
        });
        matrices.popPose();
    }

    public static void drawThickCircleOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            float thickness,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(thickness) || radius <= 0.0f || thickness <= 0.0f) return;

        float halfThickness = Math.min(thickness / 2.0f, radius);
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        submitWorldGeometry(matrices, consumer, circleFilledLayer(throughWalls), throughWalls, (entry, buffer) -> {
            for (int i = 0; i < segmentCount; i++) {
                double angle1 = (Math.PI * 2.0) * i / segmentCount;
                double angle2 = (Math.PI * 2.0) * (i + 1) / segmentCount;

                float innerX1 = (float) (center.x + Math.cos(angle1) * innerRadius);
                float innerZ1 = (float) (center.z + Math.sin(angle1) * innerRadius);
                float outerX1 = (float) (center.x + Math.cos(angle1) * outerRadius);
                float outerZ1 = (float) (center.z + Math.sin(angle1) * outerRadius);
                float innerX2 = (float) (center.x + Math.cos(angle2) * innerRadius);
                float innerZ2 = (float) (center.z + Math.sin(angle2) * innerRadius);
                float outerX2 = (float) (center.x + Math.cos(angle2) * outerRadius);
                float outerZ2 = (float) (center.z + Math.sin(angle2) * outerRadius);
                float y = (float) center.y;

                triangle(buffer, entry, innerX1, y, innerZ1, outerX1, y, outerZ1, outerX2, y, outerZ2, color);
                triangle(buffer, entry, innerX1, y, innerZ1, outerX2, y, outerZ2, innerX2, y, innerZ2, color);
                triangle(buffer, entry, outerX2, y, outerZ2, outerX1, y, outerZ1, innerX1, y, innerZ1, color);
                triangle(buffer, entry, innerX2, y, innerZ2, outerX2, y, outerZ2, innerX1, y, innerZ1, color);
            }
        });
        matrices.popPose();
    }

    public static void drawThickSquareOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float size,
            float thickness,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        if (!Float.isFinite(size) || !Float.isFinite(thickness) || size <= 0.0f || thickness <= 0.0f) return;

        float halfSize = size / 2.0f;
        float halfThickness = Math.min(thickness / 2.0f, halfSize);
        double minX = center.x - halfSize;
        double maxX = center.x + halfSize;
        double minZ = center.z - halfSize;
        double maxZ = center.z + halfSize;
        double minY = center.y;
        double maxY = center.y + Math.max(0.01f, thickness * 0.25f);

        drawFilled(matrices, consumer, camera,
                new AABB(minX, minY, minZ, maxX, maxY, minZ + halfThickness),
                throughWalls, color);
        drawFilled(matrices, consumer, camera,
                new AABB(minX, minY, maxZ - halfThickness, maxX, maxY, maxZ),
                throughWalls, color);
        drawFilled(matrices, consumer, camera,
                new AABB(minX, minY, minZ + halfThickness, minX + halfThickness, maxY, maxZ - halfThickness),
                throughWalls, color);
        drawFilled(matrices, consumer, camera,
                new AABB(maxX - halfThickness, minY, minZ + halfThickness, maxX, maxY, maxZ - halfThickness),
                throughWalls, color);
    }

    public static void drawCircleWall(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            float height,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(height) || radius <= 0.0f || height <= 0.0f) return;

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        submitWorldGeometry(matrices, consumer, filledLayer(throughWalls), throughWalls, (entry, buffer) -> {
            float minY = (float) center.y;
            float maxY = minY + height;

            for (int i = 0; i < segmentCount; i++) {
                double angle1 = (Math.PI * 2.0) * i / segmentCount;
                double angle2 = (Math.PI * 2.0) * (i + 1) / segmentCount;

                float x1 = (float) (center.x + Math.cos(angle1) * radius);
                float z1 = (float) (center.z + Math.sin(angle1) * radius);
                float x2 = (float) (center.x + Math.cos(angle2) * radius);
                float z2 = (float) (center.z + Math.sin(angle2) * radius);

                quad(buffer, entry,
                        x1, minY, z1,
                        x2, minY, z2,
                        x2, maxY, z2,
                        x1, maxY, z1,
                        color.r, color.g, color.b, color.a);
                quad(buffer, entry,
                        x1, maxY, z1,
                        x2, maxY, z2,
                        x2, minY, z2,
                        x1, minY, z1,
                        color.r, color.g, color.b, color.a);
            }
        });
        matrices.popPose();
    }

    public static void drawCircleWireframeWall(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            float height,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(height) || radius <= 0.0f || height <= 0.0f) return;

        drawCircleOutline(matrices, consumer, camera, center, radius, segments, throughWalls, color);
        drawCircleOutline(matrices, consumer, camera, center.add(0.0, height * 0.5f, 0.0), radius, segments, throughWalls, color);
        drawCircleOutline(matrices, consumer, camera, center.add(0.0, height, 0.0), radius, segments, throughWalls, color);

        int verticalMarkers = Math.max(8, segmentCount / 5);
        double markerHalfWidth = 0.015;
        for (int i = 0; i < verticalMarkers; i++) {
            double angle = (Math.PI * 2.0) * i / verticalMarkers;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            AABB marker = new AABB(
                    x - markerHalfWidth,
                    center.y,
                    z - markerHalfWidth,
                    x + markerHalfWidth,
                    center.y + height,
                    z + markerHalfWidth
            );
            drawOutline(matrices, consumer, camera, marker, throughWalls, color);
        }
    }

    public static void drawBillboardSquareOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float size,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        float half = size / 2.0f;

        matrices.pushPose();
        translateBillboard(matrices, camera, center);

        submitTransformedWorldGeometry(matrices, consumer, circleOutlineLayer(throughWalls), throughWalls, (entry, buffer) -> {
            lineVertex(buffer, entry, -half, -half, color);
            lineVertex(buffer, entry, half, -half, color);
            lineVertex(buffer, entry, half, half, color);
            lineVertex(buffer, entry, -half, half, color);
            lineVertex(buffer, entry, -half, -half, color);
        });
        matrices.popPose();
    }

    public static void drawBillboardCircleOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);

        matrices.pushPose();
        translateBillboard(matrices, camera, center);

        submitTransformedWorldGeometry(matrices, consumer, circleOutlineLayer(throughWalls), throughWalls, (entry, buffer) -> {
            for (int i = 0; i <= segmentCount; i++) {
                double angle = (Math.PI * 2.0) * i / segmentCount;
                lineVertex(
                        buffer,
                        entry,
                        (float) (Math.cos(angle) * radius),
                        (float) (Math.sin(angle) * radius),
                        color
                );
            }
        });
        matrices.popPose();
    }

    public static void drawThickBillboardCircleOutline(
            @NotNull PoseStack matrices,
            OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            float thickness,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        final int segmentCount = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(thickness) || radius <= 0.0f || thickness <= 0.0f) return;

        float halfThickness = Math.min(thickness / 2.0f, radius);
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        matrices.pushPose();
        translateBillboard(matrices, camera, center);

        submitTransformedWorldGeometry(matrices, consumer, circleFilledLayer(throughWalls), throughWalls, (entry, buffer) -> {
            for (int i = 0; i < segmentCount; i++) {
                double angle1 = (Math.PI * 2.0) * i / segmentCount;
                double angle2 = (Math.PI * 2.0) * (i + 1) / segmentCount;

                float innerX1 = (float) (Math.cos(angle1) * innerRadius);
                float innerY1 = (float) (Math.sin(angle1) * innerRadius);
                float outerX1 = (float) (Math.cos(angle1) * outerRadius);
                float outerY1 = (float) (Math.sin(angle1) * outerRadius);
                float innerX2 = (float) (Math.cos(angle2) * innerRadius);
                float innerY2 = (float) (Math.sin(angle2) * innerRadius);
                float outerX2 = (float) (Math.cos(angle2) * outerRadius);
                float outerY2 = (float) (Math.sin(angle2) * outerRadius);

                triangle(buffer, entry, innerX1, innerY1, 0.0f, outerX1, outerY1, 0.0f, outerX2, outerY2, 0.0f, color);
                triangle(buffer, entry, innerX1, innerY1, 0.0f, outerX2, outerY2, 0.0f, innerX2, innerY2, 0.0f, color);
                triangle(buffer, entry, outerX2, outerY2, 0.0f, outerX1, outerY1, 0.0f, innerX1, innerY1, 0.0f, color);
                triangle(buffer, entry, innerX2, innerY2, 0.0f, outerX2, outerY2, 0.0f, innerX1, innerY1, 0.0f, color);
            }
        });
        matrices.popPose();
    }

    private static void translateBillboard(
            @NotNull PoseStack matrices,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center
    ) {
        Vec3 camPos = camera.pos;
        matrices.translate(
                center.x - camPos.x,
                center.y - camPos.y,
                center.z - camPos.z
        );
        matrices.mulPose(camera.orientation);
    }

    private static void lineVertex(
            @NotNull VertexConsumer buffer,
            PoseStack.@NotNull Pose entry,
            float x,
            float y,
            @NotNull RenderColor color
    ) {
        buffer.addVertex(entry, x, y, 0.0f).setColor(color.r, color.g, color.b, color.a);
    }

    private static void triangle(
            @NotNull VertexConsumer buffer,
            PoseStack.@NotNull Pose entry,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            @NotNull RenderColor color
    ) {
        buffer.addVertex(entry, x1, y1, z1).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(entry, x2, y2, z2).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(entry, x3, y3, z3).setColor(color.r, color.g, color.b, color.a);
    }

    public static void drawText(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, @NotNull Vec3 pos, Component text,
            float scale, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.pushPose();
        Vec3 camPos = camera.pos;

        matrices.translate(
                pos.x - camPos.x,
                pos.y - camPos.y,
                pos.z - camPos.z
        );

        matrices.mulPose(camera.orientation);
        matrices.scale(scale, -scale, scale);

        Font.DisplayMode displayMode = throughWalls
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.NORMAL;
        WorldOverlayBatch batch = WorldOverlayBatch.active();
        if (batch != null) {
            batch.submitText(
                    matrices, -mc.font.width(text) / 2f, 0,
                    text.getVisualOrderText(), true, displayMode,
                    LightCoordsUtil.FULL_BRIGHT, color.argb, 0, 0,
                    throughWalls, consumer
            );
        } else {
            consumer.submitText(matrices, -mc.font.width(text) / 2f, 0,
                    text.getVisualOrderText(), true, displayMode,
                    LightCoordsUtil.FULL_BRIGHT, color.argb, 0, 0);
        }
        matrices.popPose();
    }

    public static void drawBeam(
            PoseStack matrices, OrderedSubmitNodeCollector consumer,
            CameraRenderState camera, Vec3 pos, int height,
            boolean throughWalls, RenderColor color
    ) {
        drawFilled(
                matrices, consumer, camera,
                AABB.ofSize(pos, 0.5, 0, 0.5).expandTowards(0, height, 0),
                throughWalls, color
        );
    }

    public static void drawStyledBox(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, @NotNull AABB box, boolean throughWalls,
            @NotNull RenderColor color, @NotNull RenderStyle style
    ) {
        switch (style) {
            case SOLID -> drawFilled(matrices, consumer, camera, box, throughWalls, color);
            case OUTLINE -> drawOutline(matrices, consumer, camera, box, throughWalls, color);
            case BOTH -> {
                drawFilled(matrices, consumer, camera, box, throughWalls, color.withOpacity(color.a * 0.5f));
                drawOutline(matrices, consumer, camera, box, throughWalls, color);
            }
        }
    }

    public static void drawStyledHitBox(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, @NotNull Entity entity, @NotNull DeltaTracker tickCounter,
            boolean throughWalls, @NotNull RenderColor color, RenderStyle style
    ) {
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
        AABB box = getBox(entity, tickDelta);

        drawStyledBox(matrices, consumer, camera, box, throughWalls, color, style);
    }

    public static void drawTracer(
            @NotNull PoseStack matrices, @NotNull OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, @NotNull Vec3 pos, @NotNull RenderColor color
    ) {
        Vec3 camPos = camera.pos;

        matrices.pushPose();
        matrices.translate(-camPos.x(), -camPos.y(), -camPos.z());

        submitWorldGeometry(matrices, consumer, Layers.GuiLine, false, (entry, buffer) -> {
            Vec3 point = camPos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot));

            Vector3f normal = pos.toVector3f()
                    .sub((float) point.x(), (float) point.y(), (float) point.z())
                    .normalize(new Vector3f(1.0f, 1.0f, 1.0f));

            buffer.addVertex(entry, (float) point.x(), (float) point.y(), (float) point.z()).setColor(color.r, color.g, color.b, color.a).setNormal(entry, normal);
            buffer.addVertex(entry, (float) pos.x(), (float) pos.y(), (float) pos.z()).setColor(color.r, color.g, color.b, color.a).setNormal(entry, normal);
        });
        matrices.popPose();
    }

    public static void drawHitBox(
            @NotNull PoseStack matrices, OrderedSubmitNodeCollector consumer,
            @NotNull CameraRenderState camera, Entity entity, @NotNull DeltaTracker tickCounter,
            boolean troughWalls, RenderColor color
    ) {
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
        drawOutline(matrices, consumer, camera, getBox(entity, tickDelta), troughWalls, color);
    }

    @Contract("_, _ -> new")
    private static @NotNull AABB getBox(@NotNull Entity entity, float tickDelta) {
        double x = entity.xo + (entity.getX() - entity.xo) * tickDelta;
        double y = entity.yo + (entity.getY() - entity.yo) * tickDelta;
        double z = entity.zo + (entity.getZ() - entity.zo) * tickDelta;

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float halfWidth = width / 2.0f;

        return new AABB(
                x - halfWidth,
                y,
                z - halfWidth,
                x + halfWidth,
                y + height,
                z + halfWidth
        );
    }

    private static float sanitizeLineWidth(float lineWidth) {
        if (!Float.isFinite(lineWidth)) {
            return DEFAULT_OUTLINE_LINE_WIDTH;
        }

        return Math.clamp(lineWidth, 0.5f, 4.0f);
    }

    private static void drawBox(
            PoseStack.@NotNull Pose entry,
            @NotNull VertexConsumer consumer,
            @NotNull AABB box,
            int color,
            float lineWidth
    ) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        line(consumer, entry, minX, minY, minZ, maxX, minY, minZ, color, lineWidth);
        line(consumer, entry, maxX, minY, minZ, maxX, minY, maxZ, color, lineWidth);
        line(consumer, entry, maxX, minY, maxZ, minX, minY, maxZ, color, lineWidth);
        line(consumer, entry, minX, minY, maxZ, minX, minY, minZ, color, lineWidth);
        line(consumer, entry, minX, maxY, minZ, maxX, maxY, minZ, color, lineWidth);
        line(consumer, entry, maxX, maxY, minZ, maxX, maxY, maxZ, color, lineWidth);
        line(consumer, entry, maxX, maxY, maxZ, minX, maxY, maxZ, color, lineWidth);
        line(consumer, entry, minX, maxY, maxZ, minX, maxY, minZ, color, lineWidth);
        line(consumer, entry, minX, minY, minZ, minX, maxY, minZ, color, lineWidth);
        line(consumer, entry, maxX, minY, minZ, maxX, maxY, minZ, color, lineWidth);
        line(consumer, entry, maxX, minY, maxZ, maxX, maxY, maxZ, color, lineWidth);
        line(consumer, entry, minX, minY, maxZ, minX, maxY, maxZ, color, lineWidth);
    }

    private static void line(
            @NotNull VertexConsumer consumer,
            PoseStack.@NotNull Pose entry,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            int color,
            float lineWidth
    ) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
        consumer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, normal).setLineWidth(lineWidth);
        consumer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, normal).setLineWidth(lineWidth);
    }

    public static void drawFilledBox(
            PoseStack.Pose entry,
            VertexConsumer consumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float r, float g, float b, float a
    ) {

        quad(consumer, entry, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        quad(consumer, entry, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose entry,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float r, float g, float b, float a
    ) {
        consumer.addVertex(entry, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(entry, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(entry, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(entry, x4, y4, z4).setColor(r, g, b, a);
    }

    public enum RenderStyle {
        SOLID, OUTLINE, BOTH;
    }

    public static class Pipelines {

        public static final RenderPipeline.Snippet filledSnippet = RenderPipelines.DEBUG_FILLED_SNIPPET;
        public static final RenderPipeline.Snippet outlineSnippet = RenderPipelines.LINES_SNIPPET;

        public static final RenderPipeline filledNoCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_filled_no_cull"))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withCull(false)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline filledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_filled_cull"))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withCull(false)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .build()
        );

        public static final RenderPipeline outlineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_outline_no_cull"))
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build());

        public static final RenderPipeline outlineCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_outline_cull"))
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                .build());

        public static final RenderPipeline lineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_line_no_cull"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build());

        public static final RenderPipeline circleFilledNoCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_filled_no_cull"))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                        .withCull(false)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline circleFilledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_filled_cull"))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                        .withCull(false)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .build()
        );

        public static final RenderPipeline circleOutlineNoCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_outline_no_cull"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline circleOutlineCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_outline_cull"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                .build()
        );
    }

    public static class Parameters {

        public static final RenderSetup FILLED = RenderSetup.builder(Pipelines.filledCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();

        public static final RenderSetup FILLED_NO_CULL = RenderSetup.builder(Pipelines.filledNoCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();

        public static final RenderSetup LINES = RenderSetup.builder(Pipelines.outlineCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();

        public static final RenderSetup LINES_NO_CULL = RenderSetup.builder(Pipelines.outlineNoCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();
    }

    public static class Layers {

        public static final RenderType BoxFilled = RenderType.create(
                "iqaddons_box_filled",
                RenderSetup.builder(Pipelines.filledCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType BoxFilledNoCull = RenderType.create(
                "iqaddons_box_filled_no_cull",
                RenderSetup.builder(Pipelines.filledNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType BoxOutline = RenderType.create(
                "iqaddons_box_outline",
                RenderSetup.builder(Pipelines.outlineCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType BoxOutlineNoCull = RenderType.create(
                "iqaddons_box_outline_no_cull",
                RenderSetup.builder(Pipelines.outlineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType GuiLine = RenderType.create(
                "iqaddons_gui_line",
                RenderSetup.builder(Pipelines.lineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType CircleFilled = RenderType.create(
                "iqaddons_circle_filled",
                RenderSetup.builder(Pipelines.circleFilledCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType CircleFilledNoCull = RenderType.create(
                "iqaddons_circle_filled_no_cull",
                RenderSetup.builder(Pipelines.circleFilledNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType CircleOutline = RenderType.create(
                "iqaddons_circle_outline",
                RenderSetup.builder(Pipelines.circleOutlineCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );

        public static final RenderType CircleOutlineNoCull = RenderType.create(
                "iqaddons_circle_outline_no_cull",
                RenderSetup.builder(Pipelines.circleOutlineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutputTarget(WorldOverlayLayerPolicy.outputTarget())
                        .createRenderSetup()
        );
    }
}
