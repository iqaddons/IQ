package net.iqaddons.mod.utils.pearl;

import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Low arc aimed at the center of the landing surface, using discrete projectile physics. */
final class FlatPearlSolver {
    private static final double SPEED = 1.5;
    private static final double DRAG = 0.9900000095367432;
    private static final double GRAVITY = 0.029999999329447746;
    private static final double SIDE_OFFSET = 0.1600000023841858;
    private static final int MAX_TICKS = 96;
    private static final double CENTER_TOLERANCE_SQR = 1.0E-8;
    private static final Map<Key, PearlAimSolution> CACHE = new HashMap<>();
    private static WeakReference<Level> cachedLevel = new WeakReference<>(null);
    private static long cachedTick = Long.MIN_VALUE;

    private FlatPearlSolver() { }

    static @Nullable PearlAimSolution solve(Vec3 origin, Vec3 target,
                                                        @Nullable Level level, @Nullable Entity owner) {
        return solve(origin, target, level, owner, false, true);
    }

    static synchronized @Nullable PearlAimSolution solve(Vec3 origin, Vec3 target,
            @Nullable Level level, @Nullable Entity owner, boolean lowestNearby, boolean validatePath) {
        // Exact positions: quantizing the launch position reuses a wrong direction after small movement.
        long tick = level == null ? 0 : level.getGameTime();
        if (cachedLevel.get() != level || cachedTick != tick || CACHE.size() >= 192) {
            CACHE.clear();
            cachedLevel = new WeakReference<>(level);
            cachedTick = tick;
        }
        Key key = new Key(origin, target, owner == null ? -1 : owner.getId(), lowestNearby, validatePath);
        if (CACHE.containsKey(key)) return CACHE.get(key);
        PearlAimSolution solution = solveUncached(origin, target, level, owner, lowestNearby, validatePath);
        CACHE.put(key, solution);
        return solution;
    }

    private static @Nullable PearlAimSolution solveUncached(Vec3 origin, Vec3 target,
            @Nullable Level level, @Nullable Entity owner, boolean lowestNearby, boolean validatePath) {
        Vec3 center = landingCenter(target, level, owner);
        if (center == null) return null;
        PearlAimSolution best = solveCenter(origin, center, level, owner, validatePath);
        if (!lowestNearby) return best;

        // Integer pile coordinates lie between equally near block centers. Only break those ties:
        // never expand the landing radius or move a fractional/custom target to a farther block.
        int xChoices = target.x == Math.rint(target.x) ? 2 : 1;
        int zChoices = target.z == Math.rint(target.z) ? 2 : 1;
        for (int x = 0; x < xChoices; x++) {
            for (int z = 0; z < zChoices; z++) {
                if (x == 0 && z == 0) continue;
                Vec3 alternate = landingCenter(new Vec3(center.x - x, target.y, center.z - z), level, owner);
                if (alternate == null || Math.abs(alternate.y - center.y) > 1.0E-4) continue;
                PearlAimSolution candidate = solveCenter(origin, alternate, level, owner, validatePath);
                if (candidate != null && (best == null || candidate.direction().y < best.direction().y - 1.0E-12)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static @Nullable Vec3 landingCenter(Vec3 target, @Nullable Level level, @Nullable Entity owner) {
        Vec3 center = new Vec3(Math.floor(target.x) + 0.5, target.y, Math.floor(target.z) + 0.5);
        if (level != null) {
            // Start above the face, not exactly on it: clip treats a boundary start as an inside hit.
            // The configured pile coordinate need not be the top of its collision shape.
            var surface = level.clip(new ClipContext(center.add(0, 1.5, 0), center.add(0, -6, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    owner == null ? CollisionContext.empty() : CollisionContext.of(owner)));
            if (surface.getType() != HitResult.Type.BLOCK || surface.isInside()) return null;
            center = surface.getLocation();
        }
        return center;
    }

    private static @Nullable PearlAimSolution solveCenter(Vec3 origin, Vec3 center,
            @Nullable Level level, @Nullable Entity owner, boolean validatePath) {
        PearlAimSolution solution = solveSurface(origin, center);
        // Marker visibility is independent of obstructions. Throw helpers still validate the path.
        if (solution == null || level == null || !validatePath) return solution;
        Vec3 direction = solution.direction();
        double horizontal = Math.hypot(direction.x, direction.z);
        Vec3 start = origin.add(-direction.z / horizontal * SIDE_OFFSET, 0,
                direction.x / horizontal * SIDE_OFFSET);
        var hit = PearlImpactProjector.projectPearlImpact(level, owner, start, direction.scale(SPEED),
                Math.min(MAX_TICKS, (int) Math.ceil(solution.flightTicks()) + 1));
        // Do not show an approximate/unvalidated fallback through an obstacle or at a block edge.
        if (hit == null || hit.location().distanceToSqr(center) > CENTER_TOLERANCE_SQR) return null;
        return solution;
    }

    private static @Nullable PearlAimSolution solveSurface(Vec3 origin, Vec3 target) {
        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        double radiusSq = dx * dx + dz * dz;
        if (!Double.isFinite(radiusSq) || radiusSq <= SIDE_OFFSET * SIDE_OFFSET) return null;
        double distance = Math.sqrt(radiusSq - SIDE_OFFSET * SIDE_OFFSET);
        double dy = target.y - origin.y;
        if (!Double.isFinite(dy)) return null;
        double yaw = Math.atan2(dx, dz) + Math.asin(SIDE_OFFSET / Math.sqrt(radiusSq));

        // At n+f ticks, displacement is v0*(sumDrag + f*dragPower) - gravitySum - f*gravitySpeed.
        // Enforcing |v0|=SPEED gives a quadratic in f. The first descending root is the low arc.
        double sumDrag = 0, dragPower = 1, gravitySum = 0, gravitySpeed = 0;
        for (int n = 0; n < MAX_TICKS; n++) {
            double vertical = dy + gravitySum;
            double a = gravitySpeed * gravitySpeed - SPEED * SPEED * dragPower * dragPower;
            double b = 2 * (vertical * gravitySpeed - SPEED * SPEED * sumDrag * dragPower);
            double c = distance * distance + vertical * vertical - SPEED * SPEED * sumDrag * sumDrag;
            double discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                // Stable quadratic formula avoids cancellation for roots near tick boundaries.
                double q = -0.5 * (b + Math.copySign(Math.sqrt(discriminant), b));
                double first = Math.abs(a) < 1.0E-12 ? -c / b : q / a;
                double second = Math.abs(a) < 1.0E-12 || q == 0 ? Double.NaN : c / q;
                if (second < first) { double swap = first; first = second; second = swap; }
                for (int root = 0; root < 2; root++) {
                    double fraction = root == 0 ? first : second;
                    if (!Double.isFinite(fraction) || fraction < -1.0E-9 || fraction > 1 + 1.0E-9) continue;
                    fraction = Math.clamp(fraction, 0, 1);
                    double factor = sumDrag + fraction * dragPower;
                    if (factor <= 0) continue;
                    double vxz = distance / factor;
                    double vy = (vertical + fraction * gravitySpeed) / factor;
                    if (vy * dragPower - gravitySpeed >= 0 || vy > vxz) continue;
                    Vec3 direction = new Vec3(Math.sin(yaw) * vxz / SPEED, vy / SPEED,
                            Math.cos(yaw) * vxz / SPEED);
                    return new PearlAimSolution(direction, n + fraction);
                }
            }
            sumDrag += dragPower;
            gravitySum += gravitySpeed;
            dragPower *= DRAG;
            gravitySpeed = gravitySpeed * DRAG + GRAVITY;
        }
        return null;
    }

    private record Key(Vec3 origin, Vec3 target, int ownerId, boolean lowestNearby, boolean validatePath) { }
}
