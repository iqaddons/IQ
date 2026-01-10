package net.iqaddons.mod.state.supply;

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
        return position.squaredDistanceTo(pos) <= PILE_RADIUS_SQUARED;
    }

    public double squaredDistanceTo(@NotNull Vec3d pos) {
        return position.squaredDistanceTo(pos);
    }
}