package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

final class KuudraWaypointRenderSupport {

    private static final float MIN_OUTLINE_WIDTH = 2.5f;
    private static final float TARGET_RADIUS = 0.42f;
    private static final float TARGET_THICKNESS = 0.045f;
    private static final int TARGET_SEGMENTS = 48;
    private static final double TEXT_GAP = 0.32;

    private KuudraWaypointRenderSupport() {
    }

    static boolean shouldRenderInPhase(@NotNull EtherwarpWaypoint waypoint, @NotNull KuudraPhase phase) {
        return waypoint.shouldShowInPhase(phase);
    }

    static void render(@NotNull WorldRenderEvent event, @NotNull EtherwarpWaypoint waypoint, @NotNull Vec3 pos, int colorIndex) {
        RenderColor color = RenderColor.fromHex(waypoint.getColorForIndex(colorIndex)).withOpacity(waypoint.alpha());
        render(event, waypoint, pos, color);
    }

    static void render(@NotNull WorldRenderEvent event, @NotNull EtherwarpWaypoint waypoint, @NotNull Vec3 pos, @NotNull RenderColor color) {
        render(event, waypoint, pos, color, true, waypoint.textScale());
    }

    static void render(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 pos,
            @NotNull RenderColor color,
            boolean showText,
            float textScale
    ) {
        AABB box = waypoint.getRenderBox(pos);
        Vec3 center = box.getCenter();

        if (waypoint.markerStyle() == EtherwarpWaypoint.WaypointMarkerStyle.TARGET) {
            renderTarget(event, center, color);
        } else {
            renderBox(event, waypoint, box, color);
        }

        if (showText) {
            renderText(event, waypoint, center, box, color, textScale);
        }
    }

    private static void renderBox(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull AABB box,
            @NotNull RenderColor color
    ) {
        WorldRenderUtils.RenderStyle style = waypoint.renderStyle();
        if (style == WorldRenderUtils.RenderStyle.SOLID) {
            event.drawFilled(box, true, color);
            return;
        }

        if (style == WorldRenderUtils.RenderStyle.BOTH) {
            event.drawFilled(box, true, color.withOpacity(color.a * 0.45f));
        }

        event.drawOutline(box, true, color, Math.max(MIN_OUTLINE_WIDTH, waypoint.lineWidth()));
    }

    private static void renderTarget(@NotNull WorldRenderEvent event, @NotNull Vec3 center, @NotNull RenderColor color) {
        event.drawThickBillboardCircleOutline(center, TARGET_RADIUS, TARGET_THICKNESS, TARGET_SEGMENTS, true, color);
        event.drawBillboardSquareOutline(center, TARGET_RADIUS * 1.45f, true, color.withOpacity(Math.min(1.0f, color.a * 1.2f)));
    }

    private static void renderText(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 center,
            @NotNull AABB box,
            @NotNull RenderColor color,
            float textScale
    ) {
        if (waypoint.text().isBlank() || waypoint.textPosition() == EtherwarpWaypoint.TextPosition.HIDDEN) {
            return;
        }

        double yOffset = (box.maxY - box.minY) * 0.5 + TEXT_GAP;
        Vec3 textPos = waypoint.textPosition() == EtherwarpWaypoint.TextPosition.ABOVE
                ? center.add(0.0, yOffset, 0.0)
                : center.add(0.0, -yOffset, 0.0);

        event.drawText(textPos, Component.literal(waypoint.text()), textScale, true, color.withOpacity(1.0f));
    }
}
