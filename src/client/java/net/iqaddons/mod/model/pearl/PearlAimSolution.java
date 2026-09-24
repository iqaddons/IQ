package net.iqaddons.mod.model.pearl;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record PearlAimSolution(
        @NotNull Vec3 direction,
        double flightTicks
) {
}
