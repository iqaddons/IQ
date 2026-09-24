package net.iqaddons.mod.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.utils.pearl.PearlImpactProjector;
import net.iqaddons.mod.utils.pearl.PearlTrajectorySolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class PearlFlatWorldTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        PearlFlatTrajectoryTest.main(new String[0]);
        Vec3 origin = new Vec3(0.5, 103.5, 0.5);
        Vec3 target = new Vec3(12, 100, 12);
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                Level level = client.level;
                for (int x = -14; x <= 14; x++) {
                    for (int z = 10; z <= 14; z++) {
                        level.setBlock(new BlockPos(x, 100, z),
                                (x < 0 ? Blocks.STONE_SLAB : Blocks.STONE).defaultBlockState(), 3);
                    }
                }
                assertHit(level, origin, target, new Vec3(12.5, 101, 12.5));
                assertHit(level, origin, new Vec3(-12, 100, 12), new Vec3(-11.5, 100.5, 12.5));
                if (PearlTrajectorySolver.solve(origin, new Vec3(6, 100, -6),
                        PearlTrajectoryType.FLAT, level, null) != null)
                    throw new AssertionError("Missing floor must not produce an unvalidated marker");
                for (int y = 100; y <= 112; y++) {
                    for (int z = -2; z <= 16; z++) {
                        level.setBlock(new BlockPos(6, y, z), Blocks.STONE.defaultBlockState(), 3);
                    }
                }
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT, client.level, null) != null)
                    throw new AssertionError("Blocked throw or previous-tick cache accepted");
                if (PearlTrajectorySolver.solveFastFlat(origin, target, client.level, null) != null)
                    throw new AssertionError("Fast path bypassed collision validation");
                var area = net.iqaddons.mod.config.preset.BuiltInPearlWaypoints.getDefaultAreas().getFirst();
                var feature = new net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature();
                try {
                    var method = feature.getClass().getDeclaredMethod("solveWaypointTrajectory",
                            net.iqaddons.mod.model.pearl.WaypointArea.class,
                            net.iqaddons.mod.model.pearl.PearlWaypoint.class, Vec3.class, Vec3.class);
                    method.setAccessible(true);
                    var displayed = (PearlAimSolution) method.invoke(feature, area, area.waypoints().getFirst(), origin, target);
                    if (displayed == null) throw new AssertionError("FLAT marker disappeared behind wall");
                    var expected = PearlTrajectorySolver.solve(origin, new Vec3(12.5, 101, 12.5), PearlTrajectoryType.FLAT);
                    if (displayed.direction().distanceToSqr(expected.direction()) > 1.0E-12)
                        throw new AssertionError("Wall changed the marker's aiming direction");
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static void assertHit(Level level, Vec3 origin, Vec3 target, Vec3 expected) {
        PearlAimSolution solution = PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT, level, null);
        if (solution == null) {
            var surface = level.clip(new net.minecraft.world.level.ClipContext(
                    new Vec3(expected.x, target.y + 1, expected.z), new Vec3(expected.x, target.y - 6, expected.z),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty()));
            throw new AssertionError("No solution for surface " + expected + "; detected=" + surface.getLocation()
                    + " type=" + surface.getType() + " inside=" + surface.isInside()
                    + " block=" + level.getBlockState(BlockPos.containing(target)));
        }
        if (solution != PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT, level, null))
            throw new AssertionError("Same-frame solution was not cached");
        Vec3 direction = solution.direction();
        double horizontal = Math.hypot(direction.x, direction.z);
        Vec3 start = origin.add(-direction.z / horizontal * 0.1600000023841858, 0,
                direction.x / horizontal * 0.1600000023841858);
        var hit = PearlImpactProjector.projectPearlImpact(level, null, start, direction.scale(1.5), 96);
        if (hit == null || hit.location().distanceToSqr(expected) > 1.0E-8)
            throw new AssertionError("Incorrect block/surface center: " + hit);
    }
}
