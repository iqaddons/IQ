package net.iqaddons.mod.model.supply;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum PreSpot {

    TRIANGLE(
            "Triangle",
            new Vec3d(-67.5, 77, -122.5),
            "Shop",
            new Vec3d(-81, 76, -143),
            15.0,
            6,  // Triangle pile missing
            7   // Shop pile missing (secondary)
    ),
    X(
            "X",
            new Vec3d(-142.5, 77, -151),
            "X Cannon",
            new Vec3d(-143, 76, -125),
            30.0,
            1,  // X pile missing
            2   // X Cannon pile missing (secondary)
    ),
    EQUALS(
            "Equals",
            new Vec3d(-65.5, 76, -87.5),
            null,
            null,
            15.0,
            5,  // Equals pile missing
            -1  // No secondary
    ),
    SLASH(
            "Slash",
            new Vec3d(-113.5, 77, -68.5),
            "Square",
            new Vec3d(-143, 76, -80),
            15.0,
            4,  // Slash pile missing
            3   // Square pile missing (secondary) - Note: This maps to square waypoints
    );

    private final String displayName;
    private final Vec3d location;

    private final String secondaryName;
    private final Vec3d secondaryLocation;
    private final double detectionRadius;

    private final int missingPreValue;
    private final int secondaryMissingValue;

    public static @Nullable PreSpot fromPlayerPosition(@NotNull Vec3d playerPos) {
        for (PreSpot spot : values()) {
            if (spot.isPlayerNearby(playerPos)) {
                return spot;
            }
        }
        return null;
    }

    @Contract(pure = true)
    public static @Nullable PreSpot fromMessage(@NotNull String message) {
        String normalized = message.toLowerCase().trim();

        if (normalized.contains("x cannon") || normalized.equals("xc")) {
            return PreSpot.X;  // X Cannon is secondary for X pre
        }
        if (normalized.contains("square")) {
            return PreSpot.SLASH;  // Square is secondary for Slash pre
        }
        if (normalized.contains("shop")) {
            return PreSpot.TRIANGLE;  // Shop is secondary for Triangle pre
        }

        return switch (normalized) {
            case "triangle", "tri" -> PreSpot.TRIANGLE;
            case "x" -> PreSpot.X;
            case "equals", "eq" -> PreSpot.EQUALS;
            case "slash" -> PreSpot.SLASH;
            default -> null;
        };
    }

    public static int getMissingPreValueFromPileName(@NotNull String pileName) {
        String normalized = pileName.toLowerCase().trim();

        return switch (normalized) {
            case "triangle", "tri" -> 6;
            case "x" -> 1;
            case "x cannon", "xc", "xcannon" -> 2;
            case "equals", "eq" -> 5;
            case "slash" -> 4;
            case "shop" -> 7;
            case "square" -> 3;
            default -> 0;
        };
    }

    public boolean isPlayerNearby(@NotNull Vec3d playerPos) {
        return playerPos.squaredDistanceTo(location) < detectionRadius * detectionRadius;
    }

    public boolean hasSecondaryLocation() {
        return secondaryLocation != null;
    }

    public double getSecondaryCheckRadius() {
        return switch (this) {
            case TRIANGLE -> 18.0;
            case X -> 16.0;
            case SLASH -> 20.0;
            default -> 15.0;
        };
    }
}