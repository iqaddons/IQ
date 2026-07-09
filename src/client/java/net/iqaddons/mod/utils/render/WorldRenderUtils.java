package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
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

    public static void drawFilled(
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
            @NotNull CameraRenderState camera, @NotNull AABB box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.pushPose();

        Vec3 camPos = camera.pos.reverse();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls ? consumer.getBuffer(Layers.BoxFilledNoCull) : consumer.getBuffer(Layers.BoxFilled);
        drawFilledBox(
                matrices, buffer,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                color.r, color.g, color.b, color.a
        );
        matrices.popPose();
    }

    public static void drawOutline(
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
            @NotNull CameraRenderState camera, AABB box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.pushPose();
        Vec3 camPos = camera.pos.reverse();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls
                ? consumer.getBuffer(Layers.BoxOutlineNoCull)
                : consumer.getBuffer(Layers.BoxOutline);

        drawBox(
                matrices.last(), buffer, box,
                color.argb, 1
        );
        matrices.popPose();
    }

    public static void drawFilledCircle(
            @NotNull PoseStack matrices,
            MultiBufferSource.BufferSource consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        if (segments < 3) segments = 3;

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        PoseStack.Pose entry = matrices.last();

        VertexConsumer buffer = consumer.getBuffer(
                throughWalls ? Layers.CircleFilledNoCull : Layers.CircleFilled
        );

        for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2.0) * i / segments;
            double angle2 = (Math.PI * 2.0) * (i + 1) / segments;

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

        matrices.popPose();
    }

    public static void drawCircleOutline(
            @NotNull PoseStack matrices,
            MultiBufferSource.BufferSource consumer,
            @NotNull CameraRenderState camera,
            @NotNull Vec3 center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        if (segments < 3) segments = 3;

        matrices.pushPose();

        Vec3 camPos = camera.pos;
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        PoseStack.Pose entry = matrices.last();

        VertexConsumer buffer = consumer.getBuffer(
                throughWalls ? Layers.CircleOutlineNoCull : Layers.CircleOutline
        );

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0) * i / segments;
            float x = (float) (center.x + Math.cos(angle) * radius);
            float y = (float) center.y;
            float z = (float) (center.z + Math.sin(angle) * radius);

            buffer.addVertex(entry, x, y, z)
                    .setColor(color.r, color.g, color.b, color.a);
        }

        matrices.popPose();
    }

    public static void drawText(
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
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

        mc.font.drawInBatch(
                text,
                -mc.font.width(text) / 2f,
                0,
                color.argb,
                true,
                matrices.last().pose(),
                consumer,
                throughWalls
                        ? Font.DisplayMode.SEE_THROUGH
                        : Font.DisplayMode.NORMAL,
                0,
                LightCoordsUtil.FULL_BRIGHT
        );

        consumer.endBatch();
        matrices.popPose();
    }

    public static void drawBeam(
            PoseStack matrices, MultiBufferSource.BufferSource consumer,
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
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
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
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
            @NotNull CameraRenderState camera, @NotNull Entity entity, @NotNull DeltaTracker tickCounter,
            boolean throughWalls, @NotNull RenderColor color, RenderStyle style
    ) {
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
        AABB box = getBox(entity, tickDelta);

        drawStyledBox(matrices, consumer, camera, box, throughWalls, color, style);
    }

    public static void drawTracer(
            @NotNull PoseStack matrices, MultiBufferSource.@NotNull BufferSource consumer,
            @NotNull CameraRenderState camera, @NotNull Vec3 pos, @NotNull RenderColor color
    ) {
        Vec3 camPos = camera.pos;

        matrices.pushPose();
        matrices.translate(-camPos.x(), -camPos.y(), -camPos.z());

        PoseStack.Pose entry = matrices.last();
        VertexConsumer buffer = consumer.getBuffer(Layers.GuiLine);
        Vec3 point = camPos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot));

        Vector3f normal = pos.toVector3f()
                .sub((float) point.x(), (float) point.y(), (float) point.z())
                .normalize(new Vector3f(1.0f, 1.0f, 1.0f));

        buffer.addVertex(entry, (float) point.x(), (float) point.y(), (float) point.z()).setColor(color.r, color.g, color.b, color.a).setNormal(entry, normal);
        buffer.addVertex(entry, (float) pos.x(), (float) pos.y(), (float) pos.z()).setColor(color.r, color.g, color.b, color.a).setNormal(entry, normal);
        matrices.popPose();
    }

    public static void drawHitBox(
            @NotNull PoseStack matrices, MultiBufferSource.BufferSource consumer,
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

    private static void drawBox(PoseStack.Pose entry, VertexConsumer consumer, AABB box, int color, float lineWidth) {
        PoseStack matrices = new PoseStack();
        matrices.last().pose().set(entry.pose());
        matrices.last().normal().set(entry.normal());

        ShapeRenderer.renderShape(
                matrices,
                consumer,
                Shapes.create(box),
                0.0,
                0.0,
                0.0,
                color,
                lineWidth
        );
    }

    public static void drawFilledBox(
            PoseStack matrices,
            VertexConsumer consumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float r, float g, float b, float a
    ) {
        PoseStack.Pose entry = matrices.last();

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
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline filledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_filled_cull"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                        .build()
        );

        public static final RenderPipeline outlineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_outline_no_cull"))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build());

        public static final RenderPipeline outlineCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_outline_cull"))
                .build());

        public static final RenderPipeline lineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_line_no_cull"))
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .build());

        public static final RenderPipeline circleFilledNoCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_filled_no_cull"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline circleFilledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_filled_cull"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                        .build()
        );

        public static final RenderPipeline circleOutlineNoCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_outline_no_cull"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        public static final RenderPipeline circleOutlineCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.fromNamespaceAndPath("iqaddons", "pipeline/iqaddons_circle_outline_cull"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .build()
        );
    }

    public static class Parameters {

        public static final RenderSetup FILLED = RenderSetup.builder(Pipelines.filledCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup();

        public static final RenderSetup FILLED_NO_CULL = RenderSetup.builder(Pipelines.filledNoCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup();

        public static final RenderSetup LINES = RenderSetup.builder(Pipelines.outlineCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup();

        public static final RenderSetup LINES_NO_CULL = RenderSetup.builder(Pipelines.outlineNoCull)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .createRenderSetup();
    }

    public static class Layers {

        public static final RenderType BoxFilled = RenderType.create(
                "iqaddons_box_filled",
                RenderSetup.builder(Pipelines.filledCull)
                        .sortOnUpload()
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType BoxFilledNoCull = RenderType.create(
                "iqaddons_box_filled_no_cull",
                RenderSetup.builder(Pipelines.filledNoCull)
                        .sortOnUpload()
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType BoxOutline = RenderType.create(
                "iqaddons_box_outline",
                RenderSetup.builder(Pipelines.outlineCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType BoxOutlineNoCull = RenderType.create(
                "iqaddons_box_outline_no_cull",
                RenderSetup.builder(Pipelines.outlineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType GuiLine = RenderType.create(
                "iqaddons_gui_line",
                RenderSetup.builder(Pipelines.lineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType CircleFilled = RenderType.create(
                "iqaddons_circle_filled",
                RenderSetup.builder(Pipelines.circleFilledCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType CircleFilledNoCull = RenderType.create(
                "iqaddons_circle_filled_no_cull",
                RenderSetup.builder(Pipelines.circleFilledNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType CircleOutline = RenderType.create(
                "iqaddons_circle_outline",
                RenderSetup.builder(Pipelines.circleOutlineCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );

        public static final RenderType CircleOutlineNoCull = RenderType.create(
                "iqaddons_circle_outline_no_cull",
                RenderSetup.builder(Pipelines.circleOutlineNoCull)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                        .createRenderSetup()
        );
    }
}