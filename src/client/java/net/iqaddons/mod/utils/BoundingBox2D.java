package net.iqaddons.mod.utils;

public record BoundingBox2D(
        double minX,
        double minZ,
        double maxX,
        double maxZ
) {
    public static BoundingBox2D fromCorners(double x1, double z1, double x2, double z2) {
        return new BoundingBox2D(
                Math.min(x1, x2),
                Math.min(z1, z2),
                Math.max(x1, x2),
                Math.max(z1, z2)
        );
    }

    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}