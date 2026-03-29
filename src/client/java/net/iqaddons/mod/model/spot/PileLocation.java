package net.iqaddons.mod.model.spot;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public record PileLocation(
        @NotNull String name,
        @NotNull Vec3d position,
        int noPreValue
) {


    private static final double PILE_RADIUS_SQUARED = 1.5 * 1.5;

    public boolean isNoPrePile(int missingPre) {
        return noPreValue == missingPre;
    }

    public boolean isNearby(@NotNull Vec3d pos) {
        double dx = position.x - pos.x;
        double dz = position.z - pos.z;
        double horizontalDistSq = dx * dx + dz * dz;

        double dy = Math.abs(position.y - pos.y);
        return horizontalDistSq <= PILE_RADIUS_SQUARED && dy <= 5.0;
    }

    public double squaredDistanceTo(@NotNull Vec3d pos) {
        return position.squaredDistanceTo(pos);
    }
}