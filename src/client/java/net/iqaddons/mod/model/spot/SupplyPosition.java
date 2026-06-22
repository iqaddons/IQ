package net.iqaddons.mod.model.spot;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record SupplyPosition(
        @NotNull Vec3 position,
        float carrierYaw,
        int entityId
) {

    private static final double CRATE_OFFSET = 3.7;
    private static final double ANGLE_OFFSET = 130.0;

    public static final double CRATE_Y = 75.0;

    public static @NotNull SupplyPosition fromGiant(
            double giantX, double giantZ,
            float yaw, int entityId
    ) {
        double angleRad = Math.toRadians(yaw + ANGLE_OFFSET);
        double crateX = giantX + (CRATE_OFFSET * Math.cos(angleRad));
        double crateZ = giantZ + (CRATE_OFFSET * Math.sin(angleRad));

        return new SupplyPosition(
                new Vec3(crateX, CRATE_Y, crateZ),
                yaw,
                entityId
        );
    }

    public boolean isNear(@NotNull Vec3 target, double radius) {
        return position.distanceToSqr(target) < radius * radius;
    }

    public Vec3 getBeaconPosition() {
        return position;
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull Vec3 getBoxMin() {
        return new Vec3(position.x, position.y - 1, position.z);
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull Vec3 getBoxMax() {
        return new Vec3(position.x + 1, position.y, position.z + 1);
    }
}