package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@UtilityClass
public class WorldRenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawFilled(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Box box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();

        Vec3d camPos = camera.getCameraPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls ? consumer.getBuffer(Layers.BoxFilledNoCull) : consumer.getBuffer(Layers.BoxFilled);
        drawFilledBox(
                matrices, buffer,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                color.r, color.g, color.b, color.a
        );
        matrices.pop();
    }

    public static void drawOutline(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, Box box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();
        Vec3d camPos = camera.getCameraPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls
                ? consumer.getBuffer(Layers.BoxOutlineNoCull)
                : consumer.getBuffer(Layers.BoxOutline);

        drawBox(
                matrices.peek(), buffer, box,
                color.argb, 1
        );
        matrices.pop();
    }

    public static void drawFilledCircle(
            @NotNull MatrixStack matrices,
            VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera,
            @NotNull Vec3d center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        if (segments < 3) segments = 3;

        matrices.push();

        Vec3d camPos = camera.getCameraPos();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        MatrixStack.Entry entry = matrices.peek();

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

            buffer.vertex(entry, (float) center.x, (float) center.y, (float) center.z)
                    .color(color.r, color.g, color.b, color.a);
            buffer.vertex(entry, x1, (float) center.y, z1)
                    .color(color.r, color.g, color.b, color.a);
            buffer.vertex(entry, x2, (float) center.y, z2)
                    .color(color.r, color.g, color.b, color.a);
        }

        matrices.pop();
    }

    public static void drawCircleOutline(
            @NotNull MatrixStack matrices,
            VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera,
            @NotNull Vec3d center,
            float radius,
            int segments,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        if (segments < 3) segments = 3;

        matrices.push();

        Vec3d camPos = camera.getCameraPos();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        MatrixStack.Entry entry = matrices.peek();

        VertexConsumer buffer = consumer.getBuffer(
                throughWalls ? Layers.CircleOutlineNoCull : Layers.CircleOutline
        );

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0) * i / segments;
            float x = (float) (center.x + Math.cos(angle) * radius);
            float y = (float) center.y;
            float z = (float) (center.z + Math.sin(angle) * radius);

            buffer.vertex(entry, x, y, z)
                    .color(color.r, color.g, color.b, color.a);
        }

        matrices.pop();
    }

    public static void drawText(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Vec3d pos, Text text,
            float scale, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();
        Vec3d camPos = camera.getCameraPos();

        matrices.translate(
                pos.x - camPos.x,
                pos.y - camPos.y,
                pos.z - camPos.z
        );

        matrices.multiply(camera.getRotation());
        matrices.scale(scale, -scale, scale);

        mc.textRenderer.draw(
                text,
                -mc.textRenderer.getWidth(text) / 2f,
                0,
                color.argb,
                true,
                matrices.peek().getPositionMatrix(),
                consumer,
                throughWalls
                        ? TextRenderer.TextLayerType.SEE_THROUGH
                        : TextRenderer.TextLayerType.NORMAL,
                0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        consumer.draw();
        matrices.pop();
    }

    public static void drawBeam(
            MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            Camera camera, Vec3d pos, int height,
            boolean throughWalls, RenderColor color
    ) {
        drawFilled(
                matrices, consumer, camera,
                Box.of(pos, 0.5, 0, 0.5).stretch(0, height, 0),
                throughWalls, color
        );
    }

    public static void drawStyledBox(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Box box, boolean throughWalls,
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
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Entity entity, @NotNull RenderTickCounter tickCounter,
            boolean throughWalls, @NotNull RenderColor color, RenderStyle style
    ) {
        float tickDelta = tickCounter.getTickProgress(true);
        Box box = getBox(entity, tickDelta);

        drawStyledBox(matrices, consumer, camera, box, throughWalls, color, style);
    }

    public static void drawTracer(
            @NotNull MatrixStack matrices, VertexConsumerProvider.@NotNull Immediate consumer,
            @NotNull Camera camera, @NotNull Vec3d pos, @NotNull RenderColor color
    ) {
        Vec3d camPos = camera.getCameraPos();

        matrices.push();
        matrices.translate(-camPos.getX(), -camPos.getY(), -camPos.getZ());

        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer buffer = consumer.getBuffer(Layers.GuiLine);
        Vec3d point = camPos.add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()));

        Vector3f normal = pos.toVector3f()
                .sub((float) point.getX(), (float) point.getY(), (float) point.getZ())
                .normalize(new Vector3f(1.0f, 1.0f, 1.0f));

        buffer.vertex(entry, (float) point.getX(), (float) point.getY(), (float) point.getZ()).color(color.r, color.g, color.b, color.a).normal(entry, normal);
        buffer.vertex(entry, (float) pos.getX(), (float) pos.getY(), (float) pos.getZ()).color(color.r, color.g, color.b, color.a).normal(entry, normal);
        matrices.pop();
    }

    public static void drawHitBox(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, Entity entity, @NotNull RenderTickCounter tickCounter,
            boolean troughWalls, RenderColor color
    ) {
        float tickDelta = tickCounter.getTickProgress(true);
        drawOutline(matrices, consumer, camera, getBox(entity, tickDelta), troughWalls, color);
    }

    @Contract("_, _ -> new")
    private static @NotNull Box getBox(@NotNull Entity entity, float tickDelta) {
        double x = entity.lastX + (entity.getX() - entity.lastX) * tickDelta;
        double y = entity.lastY + (entity.getY() - entity.lastY) * tickDelta;
        double z = entity.lastZ + (entity.getZ() - entity.lastZ) * tickDelta;

        float width = entity.getWidth();
        float height = entity.getHeight();
        float halfWidth = width / 2.0f;

        return new Box(
                x - halfWidth,
                y,
                z - halfWidth,
                x + halfWidth,
                y + height,
                z + halfWidth
        );
    }

    private static void drawBox(MatrixStack.Entry entry, VertexConsumer consumer, Box box, int color, float lineWidth) {
        MatrixStack matrices = new MatrixStack();
        matrices.peek().getPositionMatrix().set(entry.getPositionMatrix());
        matrices.peek().getNormalMatrix().set(entry.getNormalMatrix());

        VertexRendering.drawOutline(
                matrices,
                consumer,
                VoxelShapes.cuboid(box),
                0.0,
                0.0,
                0.0,
                color,
                lineWidth
        );
    }

    public static void drawFilledBox(
            MatrixStack matrices,
            VertexConsumer consumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float r, float g, float b, float a
    ) {
        MatrixStack.Entry entry = matrices.peek();

        quad(consumer, entry, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        quad(consumer, entry, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        quad(consumer, entry, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
    }

    private static void quad(
            VertexConsumer consumer,
            MatrixStack.Entry entry,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float r, float g, float b, float a
    ) {
        consumer.vertex(entry, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(entry, x2, y2, z2).color(r, g, b, a);
        consumer.vertex(entry, x3, y3, z3).color(r, g, b, a);
        consumer.vertex(entry, x4, y4, z4).color(r, g, b, a);
    }

    public enum RenderStyle {
        SOLID, OUTLINE, BOTH;
    }

    public static class Pipelines {

        public static final RenderPipeline.Snippet filledSnippet = RenderPipelines.POSITION_COLOR_SNIPPET;
        public static final RenderPipeline.Snippet outlineSnippet = RenderPipelines.RENDERTYPE_LINES_SNIPPET;

        public static final RenderPipeline filledNoCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_filled_no_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                        .withDepthWrite(false)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        public static final RenderPipeline filledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_filled_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                        .withDepthWrite(false)
                        .build()
        );

        public static final RenderPipeline outlineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_outline_no_cull"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());

        public static final RenderPipeline outlineCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_outline_cull"))
                .build());

        public static final RenderPipeline lineNoCull = RenderPipelines.register(RenderPipeline.builder(outlineSnippet)
                .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_line_no_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());

        public static final RenderPipeline circleFilledNoCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_circle_filled_no_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        public static final RenderPipeline circleFilledCull = RenderPipelines.register(
                RenderPipeline.builder(filledSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_circle_filled_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                        .build()
        );

        public static final RenderPipeline circleOutlineNoCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_circle_outline_no_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        public static final RenderPipeline circleOutlineCull = RenderPipelines.register(
                RenderPipeline.builder(outlineSnippet)
                        .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_circle_outline_cull"))
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .build()
        );
    }

    public static class Parameters {

        public static final RenderSetup FILLED = RenderSetup.builder(Pipelines.filledCull)
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .expectedBufferSize(RenderLayer.field_64010)
                .build();

        public static final RenderSetup FILLED_NO_CULL = RenderSetup.builder(Pipelines.filledNoCull)
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .expectedBufferSize(RenderLayer.field_64010)
                .build();

        public static final RenderSetup LINES = RenderSetup.builder(Pipelines.outlineCull)
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .expectedBufferSize(RenderLayer.field_64010)
                .build();

        public static final RenderSetup LINES_NO_CULL = RenderSetup.builder(Pipelines.outlineNoCull)
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .expectedBufferSize(RenderLayer.field_64010)
                .build();
    }

    public static class Layers {

        public static final RenderLayer BoxFilled = RenderLayer.of(
                "iqaddons_box_filled",
                RenderSetup.builder(Pipelines.filledCull)
                        .translucent()
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer BoxFilledNoCull = RenderLayer.of(
                "iqaddons_box_filled_no_cull",
                RenderSetup.builder(Pipelines.filledNoCull)
                        .translucent()
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer BoxOutline = RenderLayer.of(
                "iqaddons_box_outline",
                RenderSetup.builder(Pipelines.outlineCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer BoxOutlineNoCull = RenderLayer.of(
                "iqaddons_box_outline_no_cull",
                RenderSetup.builder(Pipelines.outlineNoCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer GuiLine = RenderLayer.of(
                "iqaddons_gui_line",
                RenderSetup.builder(Pipelines.lineNoCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer CircleFilled = RenderLayer.of(
                "iqaddons_circle_filled",
                RenderSetup.builder(Pipelines.circleFilledCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer CircleFilledNoCull = RenderLayer.of(
                "iqaddons_circle_filled_no_cull",
                RenderSetup.builder(Pipelines.circleFilledNoCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer CircleOutline = RenderLayer.of(
                "iqaddons_circle_outline",
                RenderSetup.builder(Pipelines.circleOutlineCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );

        public static final RenderLayer CircleOutlineNoCull = RenderLayer.of(
                "iqaddons_circle_outline_no_cull",
                RenderSetup.builder(Pipelines.circleOutlineNoCull)
                        .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .expectedBufferSize(RenderLayer.field_64010)
                        .build()
        );
    }
}