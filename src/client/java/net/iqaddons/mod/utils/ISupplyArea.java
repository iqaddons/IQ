package net.iqaddons.mod.utils;

import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ISupplyArea {

    static double HALF_WIDTH = 1.0;
    static double HALF_DEPTH = 5.8;
    static double HEIGHT = 4.8;

    static double EXT_FORWARD = 4.8;
    static double EXT_RIGHT = 3.6;
    static double EXT_LEFT = 1.0;

    @Contract("_ -> new")
    public static @NotNull AABB getEnclosingBox(Giant giant) {
        Vec3[] corners = getCorners(giant);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Vec3 c : corners) {
            minX = Math.min(minX, c.x);
            maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y);
            maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z);
            maxZ = Math.max(maxZ, c.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static Vec3 @NotNull [] getCorners(@NotNull Giant giant) {
        double yawRad = Math.toRadians(giant.getYRot());

        Vec3 fwd = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));

        Vec3 origin = giant.getEyePosition().subtract(0, 1.5, 0);

        // Asymmetrical boundaries
        // forward: [-HALF_DEPTH  ..  HALF_DEPTH + EXT_FORWARD]
        // right:   [-HALF_WIDTH  ..  HALF_WIDTH + EXT_RIGHT  ]

        Vec3 bl = scale(fwd, -HALF_DEPTH).add(scale(right, -(HALF_WIDTH + EXT_LEFT)));
        Vec3 br = scale(fwd, -HALF_DEPTH).add(scale(right, HALF_WIDTH + EXT_RIGHT));
        Vec3 fl = scale(fwd, HALF_DEPTH + EXT_FORWARD).add(scale(right, -(HALF_WIDTH + EXT_LEFT)));
        Vec3 fr = scale(fwd, HALF_DEPTH + EXT_FORWARD).add(scale(right, HALF_WIDTH + EXT_RIGHT));

        Vec3 up = new Vec3(0, HEIGHT, 0);

        return new Vec3[]{
                // bottom
                origin.add(bl),
                origin.add(br),
                origin.add(fl),
                origin.add(fr),
                // top
                origin.add(bl).add(up),
                origin.add(br).add(up),
                origin.add(fl).add(up),
                origin.add(fr).add(up),
        };
    }

    static Vec3 scale(@NotNull Vec3 v, double s) {
        return v.scale(s);
    }
}
