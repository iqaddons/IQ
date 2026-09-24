package net.iqaddons.mod.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Collects world-space overlay primitives and emits one extraction node per render layer. */
public final class WorldOverlayBatch {

    private static final ThreadLocal<WorldOverlayBatch> ACTIVE = new ThreadLocal<>();

    private final Vec3 cameraPos;
    private final @Nullable WorldOverlayPhaseSubmitter phaseSubmitter;
    private final Map<LayerKey, List<GeometryWriter>> writersByLayer = new LinkedHashMap<>();

    public WorldOverlayBatch(@NotNull Vec3 cameraPos) {
        this(cameraPos, null);
    }

    public WorldOverlayBatch(
            @NotNull Vec3 cameraPos,
            @Nullable WorldOverlayPhaseSubmitter phaseSubmitter
    ) {
        this.cameraPos = cameraPos;
        this.phaseSubmitter = phaseSubmitter;
    }

    public static @NotNull WorldOverlayBatch begin(@NotNull Vec3 cameraPos) {
        return begin(cameraPos, null);
    }

    public static @NotNull WorldOverlayBatch begin(
            @NotNull Vec3 cameraPos,
            @Nullable WorldOverlayPhaseSubmitter phaseSubmitter
    ) {
        WorldOverlayBatch batch = new WorldOverlayBatch(cameraPos, phaseSubmitter);
        ACTIVE.set(batch);
        return batch;
    }

    public static WorldOverlayBatch active() {
        return ACTIVE.get();
    }

    public static void end(@NotNull WorldOverlayBatch batch) {
        if (ACTIVE.get() == batch) {
            ACTIVE.remove();
        }
    }

    public void submit(@NotNull RenderType layer, @NotNull GeometryWriter writer) {
        submit(layer, WorldOverlayLayerPolicy.RenderPhase.AFTER_TRANSLUCENT_TERRAIN, writer);
    }

    public void submit(
            @NotNull RenderType layer,
            @NotNull WorldOverlayLayerPolicy.RenderPhase phase,
            @NotNull GeometryWriter writer
    ) {
        writersByLayer.computeIfAbsent(new LayerKey(layer, phase), ignored -> new ArrayList<>()).add(writer);
    }

    public void submitImmediate(
            @NotNull PoseStack matrices,
            @NotNull RenderType layer,
            boolean throughWalls,
            @NotNull SubmitNodeCollector.CustomGeometryRenderer renderer,
            @NotNull OrderedSubmitNodeCollector fallback
    ) {
        if (phaseSubmitter != null) {
            phaseSubmitter.iq$submitCustomGeometry(
                    matrices, layer, renderer, WorldOverlayLayerPolicy.renderPhase(throughWalls)
            );
            return;
        }
        fallback.submitCustomGeometry(matrices, layer, renderer);
    }

    public void submitText(
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
            boolean throughWalls,
            @NotNull OrderedSubmitNodeCollector fallback
    ) {
        if (phaseSubmitter != null) {
            phaseSubmitter.iq$submitText(
                    matrices, x, y, text, dropShadow, displayMode, lightCoords,
                    color, backgroundColor, outlineColor,
                    WorldOverlayLayerPolicy.renderPhase(throughWalls)
            );
            return;
        }
        fallback.submitText(
                matrices, x, y, text, dropShadow, displayMode,
                lightCoords, color, backgroundColor, outlineColor
        );
    }

    public int pendingLayerCount() {
        return writersByLayer.size();
    }

    public int pendingCommandCount() {
        return writersByLayer.values().stream().mapToInt(List::size).sum();
    }

    public void flush(@NotNull OrderedSubmitNodeCollector collector) {
        if (writersByLayer.isEmpty()) return;

        Map<LayerKey, List<GeometryWriter>> pending = new LinkedHashMap<>(writersByLayer);
        writersByLayer.clear();

        PoseStack matrices = new PoseStack();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (Map.Entry<LayerKey, List<GeometryWriter>> entry : pending.entrySet()) {
            LayerKey key = entry.getKey();
            SubmitNodeCollector.CustomGeometryRenderer renderer = (pose, vertices) -> {
                for (GeometryWriter writer : entry.getValue()) {
                    writer.write(pose, vertices);
                }
            };
            if (phaseSubmitter != null) {
                phaseSubmitter.iq$submitCustomGeometry(matrices, key.layer(), renderer, key.phase());
            } else {
                collector.submitCustomGeometry(matrices, key.layer(), renderer);
            }
        }
    }

    private record LayerKey(
            @NotNull RenderType layer,
            @NotNull WorldOverlayLayerPolicy.RenderPhase phase
    ) {
    }

    @FunctionalInterface
    public interface GeometryWriter {
        void write(PoseStack.Pose pose, VertexConsumer vertices);
    }
}
