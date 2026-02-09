package net.iqaddons.mod.model;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.time.Instant;

public record WaypointData(
        Text playerName,
        Vec3d position,
        Instant expiresAt,
        boolean isUrgent
) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public double distanceFrom(Vec3d other) {
        return position.distanceTo(other);
    }

    public double getDurationMultiplier() {
        return isUrgent ? 0.5 : 1.0;
    }
}
