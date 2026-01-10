package net.iqaddons.mod.features.kuudra.waypoints;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public record PearlWaypoint(
        @NotNull Vec3d target,
        @NotNull Color color,
        Vec3d standBlock,
        int preSupply,
        int hideForPre,
        float size,
        @NotNull String label
) {

    public static final float DEFAULT_SIZE = 0.4f;

    public boolean shouldShow(int missingPre) {
        if (hideForPre != 0 && hideForPre == missingPre) {
            return false;
        }

        if (preSupply != 0 && missingPre > 0) {
            return preSupply == missingPre;
        }

        return true;
    }

    public boolean hasStandBlock() {
        return standBlock != null && !(standBlock.x == 0 && standBlock.y == 0 && standBlock.z == 0);
    }
}
