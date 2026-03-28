package net.iqaddons.mod.model.etherwarp;

import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Representa um waypoint individual do Etherwarp Helper.
 * Cada waypoint pode aparecer em múltiplas fases e possui configurações de renderização customizáveis.
 */
public record EtherwarpWaypoint(
        @NotNull String name,
        @NotNull List<Vec3d> positions,
        int colorRgb,                     // Cor default em formato RGB
        @NotNull List<Integer> colorsRgb, // Cores por posicao (opcional)
        float alpha,                      // 0.0 - 1.0
        @NotNull WorldRenderUtils.RenderStyle renderStyle,
        float lineWidth,                  // Espessura da linha (para OUTLINE)
        @NotNull Set<KuudraPhase> showInPhases,
        @NotNull Set<KuudraPhase> hideInPhases,
        float maxRenderDistance,          // -1 = sem limite
        @NotNull HighlightShape shape,
        @NotNull BoxSpec boxSpec
) {

    public EtherwarpWaypoint {
        colorsRgb = colorsRgb == null ? List.of() : List.copyOf(colorsRgb);
    }

    public int getColorForIndex(int index) {
        if (colorsRgb.isEmpty()) {
            return colorRgb;
        }

        if (index < colorsRgb.size()) {
            return colorsRgb.get(index);
        }

        // Lista menor que positions: reutiliza a ultima cor da lista.
        return colorsRgb.get(colorsRgb.size() - 1);
    }

    public record BoxSpec(@NotNull Vec3d min, @NotNull Vec3d max) {
        public static final BoxSpec DEFAULT = new BoxSpec(new Vec3d(-0.5, 0.0, -0.5), new Vec3d(0.5, 1.0, 0.5));

        public Box toWorldBox(@NotNull Vec3d center) {
            return new Box(
                    center.getX() + min.getX(), center.getY() + min.getY(), center.getZ() + min.getZ(),
                    center.getX() + max.getX(), center.getY() + max.getY(), center.getZ() + max.getZ()
            );
        }
    }

    public enum HighlightShape {
        FULL,
        TOP,
        BOTTOM,
        SLAB_LOWER,
        SLAB_UPPER,
        HALF_LOWER,
        HALF_UPPER,
        CENTER_PLATE,
        EDGE_TOP,
        EDGE_BOTTOM,
        PILLAR,
        CUSTOM
    }

    public Box getRenderBox(@NotNull Vec3d center) {
        return switch (shape) {
            case TOP -> new Box(center.getX() - 0.5, center.getY() + 0.875, center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            case BOTTOM -> new Box(center.getX() - 0.5, center.getY(), center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 0.125, center.getZ() + 0.5);
            case SLAB_LOWER, HALF_LOWER -> new Box(center.getX() - 0.5, center.getY(), center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            case SLAB_UPPER, HALF_UPPER -> new Box(center.getX() - 0.5, center.getY() + 0.5, center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            case CENTER_PLATE -> new Box(center.getX() - 0.25, center.getY() + 0.45, center.getZ() - 0.25,
                    center.getX() + 0.25, center.getY() + 0.55, center.getZ() + 0.25);
            case EDGE_TOP -> new Box(center.getX() - 0.5, center.getY() + 0.95, center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            case EDGE_BOTTOM -> new Box(center.getX() - 0.5, center.getY(), center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 0.05, center.getZ() + 0.5);
            case PILLAR -> new Box(center.getX() - 0.2, center.getY(), center.getZ() - 0.2,
                    center.getX() + 0.2, center.getY() + 1.0, center.getZ() + 0.2);
            case CUSTOM -> boxSpec.toWorldBox(center);
            case FULL -> new Box(center.getX() - 0.5, center.getY(), center.getZ() - 0.5,
                    center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        };
    }

    /**
     * Verifica se o waypoint deve ser visível em uma determinada fase.
     */
    public boolean shouldShowInPhase(@NotNull KuudraPhase phase) {
        return !hideInPhases.contains(phase) && showInPhases.contains(phase);
    }

    /**
     * Verifica se o waypoint é válido (tem pelo menos uma fase de exibição).
     */
    public boolean isValid() {
        return !name.isBlank() && !positions.isEmpty() && !showInPhases.isEmpty();
    }

    /**
     * Gera um identificador único baseado na posição e nome.
     */
    public String getUniqueId(@NotNull Vec3d pos) {
        return String.format("%s_%.3f_%.3f_%.3f", name, pos.x, pos.y, pos.z);
    }

    // Compatibilidade com configs/uso legados que assumiam apenas uma posição.
    public @NotNull Vec3d firstPosition() {
        return positions.get(0);
    }
}



