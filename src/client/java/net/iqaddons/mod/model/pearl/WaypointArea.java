package net.iqaddons.mod.model.pearl;

import net.iqaddons.mod.utils.BoundingBox2D;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record WaypointArea(
        @NotNull String name,
        @NotNull BoundingBox2D bounds,
        @NotNull List<PearlWaypoint> waypoints
) {
    public WaypointArea {
        waypoints = List.copyOf(waypoints);
    }

    public boolean containsPlayer(double x, double z) {
        return bounds.contains(x, z);
    }
}