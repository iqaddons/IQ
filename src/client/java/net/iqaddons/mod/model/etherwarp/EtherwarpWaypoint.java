package net.iqaddons.mod.model.etherwarp;

import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Representa um waypoint individual do Etherwarp Helper.
 * Cada waypoint pode aparecer em múltiplas fases e possui configurações de renderização customizáveis.
 */
public record EtherwarpWaypoint(
        @NotNull String name,
        @NotNull List<Vec3> positions,
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

    public record BoxSpec(@NotNull Vec3 min, @NotNull Vec3 max) {
        public static final BoxSpec DEFAULT = new BoxSpec(new Vec3(-0.5, 0.0, -0.5), new Vec3(0.5, 1.0, 0.5));

        public AABB toWorldBox(@NotNull Vec3 center) {
            return new AABB(
                    center.x() + min.x(), center.y() + min.y(), center.z() + min.z(),
                    center.x() + max.x(), center.y() + max.y(), center.z() + max.z()
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

    public AABB getRenderBox(@NotNull Vec3 center) {
        return switch (shape) {
            case TOP -> new AABB(center.x() - 0.5, center.y() + 0.875, center.z() - 0.5,
                    center.x() + 0.5, center.y() + 1.0, center.z() + 0.5);
            case BOTTOM -> new AABB(center.x() - 0.5, center.y(), center.z() - 0.5,
                    center.x() + 0.5, center.y() + 0.125, center.z() + 0.5);
            case SLAB_LOWER, HALF_LOWER -> new AABB(center.x() - 0.5, center.y(), center.z() - 0.5,
                    center.x() + 0.5, center.y() + 0.5, center.z() + 0.5);
            case SLAB_UPPER, HALF_UPPER -> new AABB(center.x() - 0.5, center.y() + 0.5, center.z() - 0.5,
                    center.x() + 0.5, center.y() + 1.0, center.z() + 0.5);
            case CENTER_PLATE -> new AABB(center.x() - 0.25, center.y() + 0.45, center.z() - 0.25,
                    center.x() + 0.25, center.y() + 0.55, center.z() + 0.25);
            case EDGE_TOP -> new AABB(center.x() - 0.5, center.y() + 0.95, center.z() - 0.5,
                    center.x() + 0.5, center.y() + 1.0, center.z() + 0.5);
            case EDGE_BOTTOM -> new AABB(center.x() - 0.5, center.y(), center.z() - 0.5,
                    center.x() + 0.5, center.y() + 0.05, center.z() + 0.5);
            case PILLAR -> new AABB(center.x() - 0.2, center.y(), center.z() - 0.2,
                    center.x() + 0.2, center.y() + 1.0, center.z() + 0.2);
            case CUSTOM -> boxSpec.toWorldBox(center);
            case FULL -> new AABB(center.x() - 0.5, center.y(), center.z() - 0.5,
                    center.x() + 0.5, center.y() + 1.0, center.z() + 0.5);
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
    public String getUniqueId(@NotNull Vec3 pos) {
        return String.format("%s_%.3f_%.3f_%.3f", name, pos.x, pos.y, pos.z);
    }

    // Compatibilidade com configs/uso legados que assumiam apenas uma posição.
    public @NotNull Vec3 firstPosition() {
        return positions.get(0);
    }
}



