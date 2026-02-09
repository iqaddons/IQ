package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class StunWaypointsFeature extends KuudraFeature {

    private static final Vec3d STUN_POS_1 = new Vec3d(-154, 29, -172);
    private static final Vec3d STUN_POS_2 = new Vec3d(-167, 27, -168);

    private static final RenderColor WAYPOINT_COLOR = new RenderColor(0, 245, 255, 200);
    private static final float WAYPOINT_SIZE = 1.0f;

    public StunWaypointsFeature() {
        super(
                "stunWaypoints",
                "Stun Waypoints",
                () -> PhaseThreeConfig.stunWaypoints,
                KuudraPhase.STUN, KuudraPhase.EATEN
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        renderWaypoint(event, STUN_POS_1);
        renderWaypoint(event, STUN_POS_2);
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull Vec3d pos) {
        float half = WAYPOINT_SIZE / 2f;

        Box waypointBox = new Box(
                pos.getX() - half, pos.getY(), pos.getZ() - half,
                pos.getX() + half, pos.getY() + WAYPOINT_SIZE, pos.getZ() + half
        );

        event.drawOutline(waypointBox, true, WAYPOINT_COLOR);
    }
}
