package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.OptionalDouble;

@UtilityClass
public class WorldRenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawFilled(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Box box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();

        Vec3d camPos = camera.getPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls ? consumer.getBuffer(Layers.BoxFilledNoCull) : consumer.getBuffer(Layers.BoxFilled);
        VertexRendering.drawFilledBox(
                matrices, buffer,
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                color.r, color.g, color.b, color.a
        );
        matrices.pop();
    }

    public static void drawOutline(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, Box box, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();
        Vec3d camPos = camera.getPos().negate();
        matrices.translate(camPos.x, camPos.y, camPos.z);

        VertexConsumer buffer = throughWalls
                ? consumer.getBuffer(Layers.BoxOutlineNoCull)
                : consumer.getBuffer(Layers.BoxOutline);

        VertexRendering.drawBox(
                matrices.peek(), buffer, box,
                color.r, color.g, color.b, color.a
        );
        matrices.pop();
    }

    public static void drawText(
            @NotNull MatrixStack matrices, VertexConsumerProvider.Immediate consumer,
            @NotNull Camera camera, @NotNull Vec3d pos, Text text,
            float scale, boolean throughWalls, @NotNull RenderColor color
    ) {
        matrices.push();
        Vec3d camPos = camera.getPos();

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

    public static void drawTracer(
            @NotNull MatrixStack matrices, VertexConsumerProvider.@NotNull Immediate consumer,
            @NotNull Camera camera, @NotNull Vec3d pos, @NotNull RenderColor color
    ) {
        Vec3d camPos = camera.getPos();

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
    

    public static class Pipelines {
        public static final RenderPipeline.Snippet filledSnippet = RenderPipelines.POSITION_COLOR_SNIPPET;
        public static final RenderPipeline.Snippet outlineSnippet = RenderPipelines.RENDERTYPE_LINES_SNIPPET;

        public static final RenderPipeline filledNoCull = RenderPipelines.register(RenderPipeline.builder(filledSnippet)
                .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_filled_no_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build());
        
        public static final RenderPipeline filledCull = RenderPipelines.register(RenderPipeline.builder(filledSnippet)
                .withLocation(Identifier.of("iqaddons", "pipeline/iqaddons_filled_cull"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
                .build());
        
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
    }

    public static class Parameters {
        public static final RenderLayer.MultiPhaseParameters.Builder filled = RenderLayer.MultiPhaseParameters.builder()
                .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING);

        public static final RenderLayer.MultiPhaseParameters.Builder lines = RenderLayer.MultiPhaseParameters.builder()
                .layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
                .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.0)));
    }

    public static class Layers {
        public static final RenderLayer.MultiPhase BoxFilled = RenderLayer.of(
                "iqaddons_box_filled",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                true,
                Pipelines.filledCull,
                Parameters.filled.build(false)
        );

        public static final RenderLayer.MultiPhase BoxFilledNoCull = RenderLayer.of(
                "iqaddons_box_filled_no_cull",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                true,
                Pipelines.filledNoCull,
                Parameters.filled.build(false)
        );

        public static final RenderLayer.MultiPhase BoxOutline = RenderLayer.of(
                "iqaddons_box_outline",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.outlineCull,
                Parameters.lines.build(false)
        );

        public static final RenderLayer.MultiPhase BoxOutlineNoCull = RenderLayer.of(
                "iqaddons_box_outline_no_cull",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.outlineNoCull,
                Parameters.lines.build(false)
        );

        public static final RenderLayer.MultiPhase GuiLine = RenderLayer.of(
                "iqaddons_gui_line",
                RenderLayer.DEFAULT_BUFFER_SIZE,
                false,
                false,
                Pipelines.lineNoCull,
                Parameters.lines.build(false)
        );
    }
}
