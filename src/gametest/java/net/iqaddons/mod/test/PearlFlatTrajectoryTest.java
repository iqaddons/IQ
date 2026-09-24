package net.iqaddons.mod.gametest;

import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.utils.pearl.PearlTrajectorySolver;
import net.minecraft.world.phys.Vec3;

/** Independent tick-by-tick landing checks; no server or graphics required. */
public final class PearlFlatTrajectoryTest {
    public static void main(String[] args) {
        int cases = 0;
        for (double range : new double[]{8, 20, 35, 50}) {
            for (int bearing = 0; bearing < 360; bearing += 15) {
                for (double height : new double[]{1.5, 0.5, 3.5}) {
                    for (double shift : new double[]{0, 0.02}) {
                        Vec3 target = new Vec3(-98.5, 79, -112.5);
                        double angle = Math.toRadians(bearing);
                        Vec3 origin = target.add(Math.sin(angle) * range + shift, height,
                                Math.cos(angle) * range);
                        checkLanding(origin, target, false);
                        checkLanding(origin, target, true);
                        cases += 2;
                    }
                }
            }
        }
        if (PearlTrajectorySolver.solve(new Vec3(0, 80, 0), new Vec3(500.5, 79, 0.5),
                PearlTrajectoryType.FLAT) != null) throw new AssertionError("Unreachable target accepted");
        for (Vec3 origin : new Vec3[]{new Vec3(-141.5, 78.5, -90.5), new Vec3(-141.5, 77.5, -88.5)}) {
            for (Vec3 target : new Vec3[]{new Vec3(-94, 76, -106), new Vec3(-98, 76, -113)}) {
                var ordinary = PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT);
                var lowest = PearlTrajectorySolver.solveFastFlat(origin, target, null, null);
                Vec3 expectedCenter = target.add(-0.5, 0, 0.5);
                var expected = PearlTrajectorySolver.solve(origin, expectedCenter, PearlTrajectoryType.FLAT);
                if (lowest == null || expected == null || lowest.direction().distanceToSqr(expected.direction()) > 1.0E-12)
                    throw new AssertionError("Square long FLAT did not choose the nearest equally centered block: " + origin + " -> " + target
                            + " expected=" + expected + " actual=" + lowest + " original=" + ordinary);
                if (ordinary != null && lowest.direction().y >= ordinary.direction().y)
                    throw new AssertionError("Square long FLAT was not lowered");
            }
        }
        System.out.println("PASS: " + cases + " centered FLAT landings, including fast path and sub-cache-grid movement");
    }

    private static void checkLanding(Vec3 origin, Vec3 target, boolean fast) {
        PearlAimSolution solution = fast ? PearlTrajectorySolver.solveFastFlat(origin, target, null, null)
                : PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT);
        if (solution == null) throw new AssertionError("Missing reachable solution: " + origin);
        Vec3 direction = solution.direction();
        double horizontal = Math.hypot(direction.x, direction.z);
        // Launch offset in world coordinates, independent of the solver's yaw convention.
        Vec3 position = origin.add(-direction.z / horizontal * 0.1600000023841858, 0,
                direction.x / horizontal * 0.1600000023841858);
        Vec3 velocity = direction.scale(1.5);
        for (int tick = 1; tick <= 96; tick++) {
            Vec3 next = position.add(velocity);
            if (position.y >= target.y && next.y <= target.y && velocity.y < 0) {
                double fraction = (target.y - position.y) / velocity.y;
                Vec3 hit = position.add(velocity.scale(fraction));
                double miss = Math.hypot(hit.x - target.x, hit.z - target.z);
                if (miss > 0.0001) throw new AssertionError("Center miss=" + miss + " origin=" + origin + " fast=" + fast);
                if (Math.abs(solution.flightTicks() - (tick - 1 + fraction)) > 0.0001)
                    throw new AssertionError("Wrong flight time");
                return;
            }
            position = next;
            velocity = velocity.scale(0.9900000095367432).add(0, -0.029999999329447746, 0);
        }
        throw new AssertionError("No descending landing");
    }
}

