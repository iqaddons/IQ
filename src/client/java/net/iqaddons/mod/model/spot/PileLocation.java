package net.iqaddons.mod.model.spot;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PileLocation(
        @NotNull String name,
        @NotNull Vec3d position,
        int noPreValue
) {

    public static final List<PileLocation> DEFAULT_PILES = List.of(
            new PileLocation("Shop", new Vec3d(-98, 78.125, -112.9375), 7),
            new PileLocation("X Cannon", new Vec3d(-110, 78.125, -106), 2),
            new PileLocation("Slash", new Vec3d(-106, 78.125, -99.0625), 4),
            new PileLocation("Triangle", new Vec3d(-94, 78.125, -106), 6),
            new PileLocation("Equals", new Vec3d(-98, 78.125, -99.0625), 5),
            new PileLocation("X", new Vec3d(-106, 78.125, -112.9375), 1)
    );

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