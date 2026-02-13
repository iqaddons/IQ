package net.iqaddons.mod.model.pearl;

import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public record PearlWaypoint(
        @NotNull Vec3d target,
        @NotNull RenderColor color,
        Vec3d standBlock,
        Integer preSupply,
        Integer hideForPre,
        float size,
        @NotNull String label,
        boolean alert
) {

    public static final float DEFAULT_SIZE = 0.4f;

    public boolean shouldShow(int missingPre) {
        if (hideForPre != null && hideForPre == missingPre) {
            return false;
        }

        if (preSupply != null && missingPre > 0) {
            return preSupply == missingPre;
        }

        return true;
    }

    public boolean hasStandBlock() {
        return standBlock != null && !(standBlock.x == 0 && standBlock.y == 0 && standBlock.z == 0);
    }
}
