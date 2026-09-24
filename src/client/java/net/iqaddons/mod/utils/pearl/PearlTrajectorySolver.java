package net.iqaddons.mod.utils.pearl;

import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PearlTrajectorySolver {

    private PearlTrajectorySolver() {
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type
    ) {
        return PearlImpactProjector.solve(origin, target, type);
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type,
            boolean debug
    ) {
        return PearlImpactProjector.solve(origin, target, type, debug);
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type,
            @Nullable Level level,
            @Nullable Entity owner
    ) {
        return PearlImpactProjector.solve(origin, target, type, level, owner, false);
    }

    public static @Nullable PearlAimSolution solveFastFlat(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @Nullable Level level,
            @Nullable Entity owner
    ) {
        return FlatPearlSolver.solve(origin, target, level, owner, true, true);
    }

    public static @Nullable PearlAimSolution solveFlatForDisplay(
            @NotNull Vec3 origin, @NotNull Vec3 target, @Nullable Level level, @Nullable Entity owner,
            boolean lowestNearby
    ) {
        return FlatPearlSolver.solve(origin, target, level, owner, lowestNearby, false);
    }
}
