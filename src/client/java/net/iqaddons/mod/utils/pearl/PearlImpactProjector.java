package net.iqaddons.mod.utils.pearl;

import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PearlImpactProjector {

    private static final double VANILLA_SPEED = 1.5;
    private static final double VANILLA_DRAG = 0.9900000095367432;
    private static final double VANILLA_GRAVITY = 0.029999999329447746;
    private static final double LEGACY_GRAVITY_PER_TICK = -0.03;
    private static final double LEGACY_DRAG_PER_TICK = 0.01;
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double MIN_HORIZONTAL_DISTANCE = 1.0E-5;
    private static final double THROW_SPEED_SQR = VANILLA_SPEED * VANILLA_SPEED;
    private static final double FLAT_DIRECT_SPEED_ERROR_TOLERANCE = 0.015;
    private static final double STATIONARY_SPEED_SQR = 1.0E-5;
    private static final double[] FLAT_SAFE_YAW_OFFSETS = {
            0.0,
            Math.toRadians(0.18), Math.toRadians(-0.18),
            Math.toRadians(0.36), Math.toRadians(-0.36),
            Math.toRadians(0.72), Math.toRadians(-0.72)
    };
    private static final double[] FLAT_SAFE_PITCH_OFFSETS = {
            0.0,
            0.20, -0.20,
            0.45, -0.45,
            0.80, -0.80,
            1.20, -1.20
    };
    private static final double FLAT_STATIONARY_YAW_SCAN_STEP = Math.toRadians(0.12);
    private static final double FLAT_STATIONARY_YAW_SCAN_RADIUS = Math.toRadians(1.2);
    private static final double FLAT_STATIONARY_PITCH_SCAN_STEP = 0.18;
    private static final double FLAT_STATIONARY_PITCH_SCAN_RADIUS = 3.6;
    private static final int FLAT_STATIONARY_REFINE_PASSES = 2;
    private static final double FAST_FLAT_SPEED_ERROR_TOLERANCE = 0.035;
    private static final double FAST_FLAT_TICK_STEP = 0.12;
    private static final double FAST_FLAT_MAX_TICKS = 46.0;
    private static final int LEGACY_ANGLE_SEARCH_STEPS = 54;
    private static final int LEGACY_TIME_SEARCH_STEPS = 56;
    private static final double GOLDEN_RATIO_PART = 0.6180339887498949;
    private static final double PEARL_SPAWN_SIDE_OFFSET = 0.1600000023841858;
    private static final double VALIDATION_TARGET_CENTER_Y_OFFSET = 0.5;
    private static final double CACHE_GRID = 16.0;
    private static final int CACHE_LIMIT = 192;
    private static final int VALIDATION_SCAN_RADIUS = 4;
    private static final int VALIDATION_REFINE_PASSES = 2;
    private static final double ROBUST_YAW_STEP = Math.toRadians(0.035);
    private static final double ROBUST_PITCH_STEP_DEGREES = 0.045;
    private static final double ROBUST_MISS_PENALTY = 9.0;
    private static final double ROBUST_DIAGONAL_MISS_PENALTY = 32.0;
    private static final double ROBUST_EDGE_MARGIN = 0.22;
    private static final double ROBUST_EDGE_WEIGHT = 7.0;
    private static final double ROBUST_CORNER_MARGIN = 0.34;
    private static final double ROBUST_CORNER_WEIGHT = 18.0;
    private static final double ROBUST_UNDERSHOOT_WEIGHT = 4.5;
    private static final double ROBUST_CENTER_WEIGHT = 0.85;
    private static final Map<CacheKey, CachedResult> CACHE = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, CachedResult> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    private PearlImpactProjector() {
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type
    ) {
        return solve(origin, target, type, false);
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type,
            boolean debug
    ) {
        return solve(origin, target, type, null, null, debug);
    }

    public static @Nullable PearlAimSolution solve(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type,
            @Nullable Level level,
            @Nullable Entity owner,
            boolean debug
    ) {
        if (type == PearlTrajectoryType.FLAT) {
            return FlatPearlSolver.solve(origin, target, level, owner);
        }
        boolean stationaryFlat = false;
        CacheKey key = CacheKey.of(origin, target, type, stationaryFlat, false);
        synchronized (CACHE) {
            CachedResult cached = CACHE.get(key);
            if (cached != null) {
                return cached.solution();
            }
        }

        Vec3 delta = target.subtract(origin);
        double horizontalDistance = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (horizontalDistance < MIN_HORIZONTAL_DISTANCE) {
            putCached(key, null);
            return null;
        }

        if (type == PearlTrajectoryType.FLAT) {
            PearlAimSolution directFlat = solveDirectFlat(origin, target, level, owner);
            if (directFlat != null) {
                putCached(key, directFlat);
                return directFlat;
            }
            putCached(key, null);
            return null;
        }

        Profile profile = Profile.forType(type);
        double baseYaw = Math.atan2(delta.x, delta.z);
        Vec3 horizontalAxis = horizontalAxis(delta);
        Candidate best = scan(origin, target, horizontalAxis, profile, baseYaw, profile.pitchStep(), profile.yawOffsets());
        if (best == null) {
            putCached(key, null);
            return null;
        }

        double refinedPitchStep = profile.pitchStep() * 0.22;
        double refinedYawStep = profile.yawStep() * 0.32;
        for (int pass = 0; pass < 3; pass++) {
            best = refine(origin, target, horizontalAxis, profile, best, refinedPitchStep, refinedYawStep);
            refinedPitchStep *= 0.45;
            refinedYawStep *= 0.45;
        }

        Candidate validated = level != null
                ? findWorldValidatedCandidate(origin, target, profile, best, level, owner)
                : null;
        Candidate selected = validated != null ? validated : best;
        double timerFlightTicks = timerFlightTicks(origin, target, type, selected);

        PearlAimSolution solution = selected.acceptable(profile) || validated != null
                ? new PearlAimSolution(selected.direction().normalize(), timerFlightTicks)
                : null;
        putCached(key, solution);
        return solution;
    }

    public static @Nullable PearlAimSolution solveFastFlat(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @Nullable Level level,
            @Nullable Entity owner
    ) {
        boolean stationaryFlat = isStationary(owner);
        CacheKey key = CacheKey.of(origin, target, PearlTrajectoryType.FLAT, stationaryFlat, true);
        synchronized (CACHE) {
            CachedResult cached = CACHE.get(key);
            if (cached != null) {
                return cached.solution();
            }
        }

        if (level == null) {
            PearlAimSolution fallback = solve(origin, target, PearlTrajectoryType.FLAT, null, owner, false);
            putCached(key, fallback);
            return fallback;
        }

        Candidate candidate = findFastFlatCandidate(origin, target, Profile.forType(PearlTrajectoryType.FLAT), level, owner);
        PearlAimSolution solution = candidate != null
                ? new PearlAimSolution(candidate.direction().normalize(), candidate.flightTicks())
                : null;
        putCached(key, solution);
        return solution;
    }

    private static @Nullable PearlAimSolution solveDirectFlat(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @Nullable Level level,
            @Nullable Entity owner
    ) {
        Double flightTicks = solveLegacyFlightTicks(origin, target, PearlTrajectoryType.FLAT);
        if (flightTicks == null || !Double.isFinite(flightTicks) || flightTicks <= 0.0) {
            return null;
        }

        Vec3 requiredVelocity = legacyVelocityForTime(target.subtract(origin), flightTicks);
        double speedError = Math.abs(requiredVelocity.lengthSqr() - THROW_SPEED_SQR);
        if (speedError > FLAT_DIRECT_SPEED_ERROR_TOLERANCE || requiredVelocity.lengthSqr() < 1.0E-7) {
            return null;
        }

        Vec3 direction = requiredVelocity.normalize();
        if (level == null) {
            return new PearlAimSolution(direction, flightTicks);
        }

        Profile profile = Profile.forType(PearlTrajectoryType.FLAT);
        Candidate direct = candidateFromDirection(origin, target, profile, direction);
        Candidate validated = isStationary(owner)
                ? findStationarySafeFlatCandidate(origin, target, profile, direct, level, owner)
                : findSafeFlatCandidate(origin, target, profile, direct, level, owner);
        return validated != null
                ? new PearlAimSolution(validated.direction().normalize(), validated.flightTicks())
                : new PearlAimSolution(direction, flightTicks);
    }

    private static boolean isStationary(@Nullable Entity owner) {
        if (owner == null) {
            return false;
        }

        Vec3 motion = owner.getDeltaMovement();
        return motion.x * motion.x + motion.y * motion.y + motion.z * motion.z <= STATIONARY_SPEED_SQR;
    }

    private static @Nullable Candidate findStationarySafeFlatCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Profile profile,
            @NotNull Candidate seed,
            @NotNull Level level,
            @Nullable Entity owner
    ) {
        BlockPos targetBlock = BlockPos.containing(target);
        Vec3 targetCenter = new Vec3(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + VALIDATION_TARGET_CENTER_Y_OFFSET,
                targetBlock.getZ() + 0.5
        );
        Candidate best = validateCandidate(
                origin,
                targetCenter,
                horizontalAxisFromYaw(seed.yawRadians()),
                profile,
                seed,
                level,
                owner,
                targetBlock
        );

        double yawStep = FLAT_STATIONARY_YAW_SCAN_STEP;
        double pitchStep = FLAT_STATIONARY_PITCH_SCAN_STEP;
        double yawRadius = FLAT_STATIONARY_YAW_SCAN_RADIUS;
        double pitchRadius = FLAT_STATIONARY_PITCH_SCAN_RADIUS;
        Candidate center = seed;

        for (int pass = 0; pass <= FLAT_STATIONARY_REFINE_PASSES; pass++) {
            Candidate passBest = best != null ? best : center;

            for (double yawOffset = -yawRadius; yawOffset <= yawRadius + 1.0E-9; yawOffset += yawStep) {
                double yaw = center.yawRadians() + yawOffset;
                Vec3 horizontalAxis = horizontalAxisFromYaw(yaw);
                for (double pitchOffset = -pitchRadius; pitchOffset <= pitchRadius + 1.0E-9; pitchOffset += pitchStep) {
                    double pitch = Math.clamp(center.pitchDegrees() + pitchOffset, profile.minPitch(), profile.maxPitch());
                    Candidate candidate = simulate(origin, target, horizontalAxis, profile, yaw, pitch);
                    Candidate validated = validateCandidate(origin, targetCenter, horizontalAxis, profile, candidate, level, owner, targetBlock);
                    if (validated != null && (best == null || validated.score() < best.score())) {
                        best = validated;
                    }
                    if (validated != null && validated.score() < passBest.score()) {
                        passBest = validated;
                    }
                }
            }

            center = passBest;
            yawRadius *= 0.32;
            pitchRadius *= 0.32;
            yawStep *= 0.35;
            pitchStep *= 0.35;
        }

        return best;
    }

    private static @Nullable Candidate findSafeFlatCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Profile profile,
            @NotNull Candidate seed,
            @NotNull Level level,
            @Nullable Entity owner
    ) {
        BlockPos targetBlock = BlockPos.containing(target);
        Vec3 targetCenter = new Vec3(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + VALIDATION_TARGET_CENTER_Y_OFFSET,
                targetBlock.getZ() + 0.5
        );
        Candidate best = validateFlatCandidate(origin, targetCenter, profile, seed, level, owner, targetBlock);

        for (double yawOffset : FLAT_SAFE_YAW_OFFSETS) {
            double yaw = seed.yawRadians() + yawOffset;
            Vec3 horizontalAxis = horizontalAxisFromYaw(yaw);
            for (double pitchOffset : FLAT_SAFE_PITCH_OFFSETS) {
                if (yawOffset == 0.0 && pitchOffset == 0.0) continue;

                double pitch = Math.clamp(seed.pitchDegrees() + pitchOffset, profile.minPitch(), profile.maxPitch());
                Candidate candidate = simulate(origin, target, horizontalAxis, profile, yaw, pitch);
                Candidate validated = validateFlatCandidate(origin, targetCenter, profile, candidate, level, owner, targetBlock);
                if (validated != null && (best == null || validated.score() < best.score())) {
                    best = validated;
                }
            }
        }

        return best;
    }

    private static @Nullable Candidate findFastFlatCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Profile profile,
            @NotNull Level level,
            @Nullable Entity owner
    ) {
        Vec3 delta = target.subtract(origin);
        double horizontalDistance = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (horizontalDistance < MIN_HORIZONTAL_DISTANCE) {
            return null;
        }

        BlockPos targetBlock = BlockPos.containing(target);
        Vec3 targetCenter = new Vec3(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + VALIDATION_TARGET_CENTER_Y_OFFSET,
                targetBlock.getZ() + 0.5
        );
        double minTicks = Math.max(1.0, horizontalDistance / VANILLA_SPEED);
        double maxTicks = FAST_FLAT_MAX_TICKS;
        Double legacyTicks = solveLegacyFlightTicks(origin, target, PearlTrajectoryType.FLAT);
        Candidate fallback = null;
        if (legacyTicks != null && Double.isFinite(legacyTicks)) {
            maxTicks = Math.min(maxTicks, Math.max(minTicks + FAST_FLAT_TICK_STEP, legacyTicks));
            Vec3 fallbackVelocity = legacyVelocityForTime(delta, legacyTicks);
            if (fallbackVelocity.lengthSqr() >= 1.0E-7) {
                fallback = candidateFromDirection(origin, target, profile, fallbackVelocity.normalize());
            }
        }

        Candidate best = null;
        for (double ticks = minTicks; ticks <= maxTicks + 1.0E-9; ticks += FAST_FLAT_TICK_STEP) {
            Vec3 requiredVelocity = legacyVelocityForTime(delta, ticks);
            if (requiredVelocity.lengthSqr() < 1.0E-7) {
                continue;
            }

            double speedError = Math.abs(requiredVelocity.lengthSqr() - THROW_SPEED_SQR);
            Candidate candidate = candidateFromDirection(origin, target, profile, requiredVelocity.normalize());
            if (speedError > FAST_FLAT_SPEED_ERROR_TOLERANCE) {
                continue;
            }

            Candidate validated = isStationary(owner)
                    ? validateCandidate(origin, targetCenter, horizontalAxisFromYaw(candidate.yawRadians()), profile, candidate, level, owner, targetBlock)
                    : validateFlatCandidate(origin, targetCenter, profile, candidate, level, owner, targetBlock);
            if (validated == null) {
                continue;
            }

            if (best == null || validated.flightTicks() < best.flightTicks()
                    || (validated.flightTicks() == best.flightTicks() && validated.score() < best.score())) {
                best = validated;
            }
        }

        return best != null ? best : fallback;
    }

    private static @Nullable Candidate validateFlatCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 targetCenter,
            @NotNull Profile profile,
            @NotNull Candidate candidate,
            @NotNull Level level,
            @Nullable Entity owner,
            @NotNull BlockPos targetBlock
    ) {
        PearlTrajectoryHit hit = tracePearl(origin, candidate, profile, level, owner);
        if (hit == null || !hit.blockPos().equals(targetBlock)) {
            return null;
        }

        double distanceSq = hit.location().distanceToSqr(targetCenter);
        Vec3 horizontalAxis = horizontalAxisFromYaw(candidate.yawRadians());
        double score = robustTrajectoryScore(origin, targetCenter, horizontalAxis, profile, candidate, level, owner, targetBlock, hit)
                + Math.abs(candidate.pitchDegrees() - profile.idealPitch()) * 0.002
                + Math.abs(hit.tick() - profile.idealFlightTicks()) * 0.001;
        return new Candidate(
                candidate.yawRadians(),
                candidate.pitchDegrees(),
                candidate.direction(),
                hit.tick(),
                distanceSq,
                score,
                candidate.landing(),
                distanceSq
        );
    }

    private static @NotNull Candidate candidateFromDirection(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Profile profile,
            @NotNull Vec3 direction
    ) {
        Vec3 normalized = direction.normalize();
        double yaw = Math.atan2(normalized.x, normalized.z);
        double pitch = Math.toDegrees(Math.asin(Math.clamp(normalized.y, -1.0, 1.0)));
        return simulate(origin, target, horizontalAxisFromYaw(yaw), profile, yaw, pitch);
    }

    private static double timerFlightTicks(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type,
            @NotNull Candidate selected
    ) {
        if (type == PearlTrajectoryType.DOUBLE_HIGH) {
            Double legacyTicks = solveLegacyFlightTicks(origin, target, type);
            if (legacyTicks != null) {
                return legacyTicks;
            }
        }
        return selected.flightTicks();
    }

    private static @Nullable Double solveLegacyFlightTicks(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType type
    ) {
        Vec3 delta = target.subtract(origin);
        double horizontalDistance = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (horizontalDistance < 1.0E-6) {
            return null;
        }

        Double angleTicks = solveLegacyByPitchWindow(delta, horizontalDistance, type);
        if (angleTicks != null) {
            return angleTicks;
        }

        return minimizeLegacySpeedError(delta, type == PearlTrajectoryType.FLAT ? 1.0E-6 : 35.0, type == PearlTrajectoryType.FLAT ? 60.0 : 120.0);
    }

    private static @Nullable Double solveLegacyByPitchWindow(
            @NotNull Vec3 delta,
            double horizontalDistance,
            @NotNull PearlTrajectoryType type
    ) {
        double minPitch = type == PearlTrajectoryType.FLAT ? -89.0 : 45.0;
        double maxPitch = type == PearlTrajectoryType.FLAT ? 45.0 : 89.0;
        double bestTicks = 0.0;
        boolean found = false;

        for (int i = 0; i < LEGACY_ANGLE_SEARCH_STEPS; i++) {
            double midPitch = (minPitch + maxPitch) * 0.5;
            double pitchRadians = Math.toRadians(midPitch);
            double horizontalVelocity = VANILLA_SPEED * Math.cos(pitchRadians);
            if (horizontalVelocity <= 1.0E-6) {
                minPitch = midPitch;
                continue;
            }

            double flightTicks = horizontalDistance / horizontalVelocity;
            double solvedY = estimateLegacyHeightAtTime(VANILLA_SPEED * Math.sin(pitchRadians), flightTicks);
            if (solvedY < delta.y) {
                minPitch = midPitch;
            } else {
                maxPitch = midPitch;
                bestTicks = flightTicks;
                found = true;
            }
        }

        return found ? bestTicks : null;
    }

    private static double estimateLegacyHeightAtTime(double verticalVelocity, double ticks) {
        double retained = 1.0 - Math.exp(-LEGACY_DRAG_PER_TICK * ticks);
        double launchLift = retained * verticalVelocity / LEGACY_DRAG_PER_TICK;
        double gravityLoss = LEGACY_GRAVITY_PER_TICK * ticks / LEGACY_DRAG_PER_TICK;
        return launchLift + gravityLoss;
    }

    private static double minimizeLegacySpeedError(@NotNull Vec3 delta, double minTicks, double maxTicks) {
        double low = minTicks;
        double high = maxTicks;
        double left = high - ((high - low) * GOLDEN_RATIO_PART);
        double right = low + ((high - low) * GOLDEN_RATIO_PART);
        double leftError = legacySpeedError(delta, left);
        double rightError = legacySpeedError(delta, right);

        for (int i = 0; i < LEGACY_TIME_SEARCH_STEPS; i++) {
            if (leftError < rightError) {
                high = right;
                right = left;
                rightError = leftError;
                left = high - ((high - low) * GOLDEN_RATIO_PART);
                leftError = legacySpeedError(delta, left);
            } else {
                low = left;
                left = right;
                leftError = rightError;
                right = low + ((high - low) * GOLDEN_RATIO_PART);
                rightError = legacySpeedError(delta, right);
            }
        }

        return (low + high) * 0.5;
    }

    private static double legacySpeedError(@NotNull Vec3 delta, double ticks) {
        return Math.abs(legacyVelocityForTime(delta, ticks).lengthSqr() - THROW_SPEED_SQR);
    }

    private static @NotNull Vec3 legacyVelocityForTime(@NotNull Vec3 delta, double ticks) {
        double retained = -Math.expm1(-LEGACY_DRAG_PER_TICK * ticks);
        Vec3 dragScaledDelta = delta.scale(LEGACY_DRAG_PER_TICK);
        Vec3 gravityTerm = new Vec3(0.0, LEGACY_GRAVITY_PER_TICK * ticks, 0.0);
        Vec3 numerator = dragScaledDelta.subtract(gravityTerm);
        Vec3 compensatedGravity = new Vec3(0.0, LEGACY_GRAVITY_PER_TICK / LEGACY_DRAG_PER_TICK, 0.0);
        return numerator.scale(1.0 / retained).add(compensatedGravity);
    }

    private static void putCached(@NotNull CacheKey key, @Nullable PearlAimSolution solution) {
        synchronized (CACHE) {
            CACHE.put(key, new CachedResult(solution));
        }
    }

    private static @Nullable Candidate scan(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Vec3 horizontalAxis,
            @NotNull Profile profile,
            double baseYaw,
            double pitchStep,
            double[] yawOffsets
    ) {
        Candidate best = null;
        for (double yawOffset : yawOffsets) {
            double yaw = baseYaw + Math.toRadians(yawOffset);
            for (double pitch = profile.minPitch(); pitch <= profile.maxPitch() + 1.0E-6; pitch += pitchStep) {
                Candidate candidate = simulate(origin, target, horizontalAxis, profile, yaw, pitch);
                if (best == null || candidate.score() < best.score()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static @Nullable Candidate findWorldValidatedCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Profile profile,
            @NotNull Candidate seed,
            @NotNull Level level,
            @Nullable Entity owner
    ) {
        BlockPos targetBlock = BlockPos.containing(target);
        Vec3 targetCenter = new Vec3(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + VALIDATION_TARGET_CENTER_Y_OFFSET,
                targetBlock.getZ() + 0.5
        );

        Candidate bestExact = validateCandidate(origin, targetCenter, horizontalAxisFromYaw(seed.yawRadians()), profile, seed, level, owner, targetBlock);
        double pitchStep = profile.pitchStep() * 0.20;
        double yawStep = profile.yawStep() * 0.55;

        for (int pass = 0; pass < VALIDATION_REFINE_PASSES; pass++) {
            Candidate center = bestExact != null ? bestExact : seed;
            for (int yawIndex = -VALIDATION_SCAN_RADIUS; yawIndex <= VALIDATION_SCAN_RADIUS; yawIndex++) {
                double yaw = center.yawRadians() + (yawIndex * yawStep);
                for (int pitchIndex = -VALIDATION_SCAN_RADIUS; pitchIndex <= VALIDATION_SCAN_RADIUS; pitchIndex++) {
                    double pitch = Math.clamp(
                            center.pitchDegrees() + (pitchIndex * pitchStep),
                            profile.minPitch(),
                            profile.maxPitch()
                    );
                    Candidate candidate = simulate(origin, target, horizontalAxisFromYaw(yaw), profile, yaw, pitch);
                    Candidate validated = validateCandidate(origin, targetCenter, horizontalAxisFromYaw(yaw), profile, candidate, level, owner, targetBlock);
                    if (validated != null && (bestExact == null || validated.score() < bestExact.score())) {
                        bestExact = validated;
                    }
                }
            }
            pitchStep *= 0.42;
            yawStep *= 0.42;
        }

        return bestExact;
    }

    private static @Nullable Candidate validateCandidate(
            @NotNull Vec3 origin,
            @NotNull Vec3 targetCenter,
            @NotNull Vec3 horizontalAxis,
            @NotNull Profile profile,
            @NotNull Candidate candidate,
            @NotNull Level level,
            @Nullable Entity owner,
            @NotNull BlockPos targetBlock
    ) {
        PearlTrajectoryHit hit = tracePearl(origin, candidate, profile, level, owner);
        if (hit == null || !hit.blockPos().equals(targetBlock)) {
            return null;
        }

        double distanceSq = hit.location().distanceToSqr(targetCenter);
        double score = robustTrajectoryScore(origin, targetCenter, horizontalAxis, profile, candidate, level, owner, targetBlock, hit)
                + Math.abs(candidate.pitchDegrees() - profile.idealPitch()) * 0.004
                + Math.abs(hit.tick() - profile.idealFlightTicks()) * 0.003;
        return new Candidate(
                candidate.yawRadians(),
                candidate.pitchDegrees(),
                candidate.direction(),
                hit.tick(),
                distanceSq,
                score,
                candidate.landing(),
                distanceSq
        );
    }

    private static double robustTrajectoryScore(
            @NotNull Vec3 origin,
            @NotNull Vec3 targetCenter,
            @NotNull Vec3 horizontalAxis,
            @NotNull Profile profile,
            @NotNull Candidate center,
            @NotNull Level level,
            @Nullable Entity owner,
            @NotNull BlockPos targetBlock,
            @NotNull PearlTrajectoryHit exactHit
    ) {
        double score = targetHitScore(exactHit.location(), targetCenter, horizontalAxis);
        int targetHits = 1;

        for (RobustOffset offset : RobustOffset.OFFSETS) {
            double pitch = Math.clamp(
                    center.pitchDegrees() + offset.pitchDegrees(),
                    profile.minPitch(),
                    profile.maxPitch()
            );
            Candidate jittered = new Candidate(
                    center.yawRadians() + offset.yawRadians(),
                    pitch,
                    directionFromYawPitch(center.yawRadians() + offset.yawRadians(), pitch * DEG_TO_RAD),
                    center.flightTicks(),
                    center.distanceSq(),
                    center.score(),
                    center.landing(),
                    center.closestDistanceSq()
            );
            PearlTrajectoryHit hit = tracePearl(origin, jittered, profile, level, owner);
            if (hit == null || !hit.blockPos().equals(targetBlock)) {
                score += missPenalty(hit, targetBlock);
                continue;
            }

            targetHits++;
            score += targetHitScore(hit.location(), targetCenter, horizontalAxis);
        }

        return (score / (double) (RobustOffset.OFFSETS.length + 1))
                + ((RobustOffset.OFFSETS.length + 1 - targetHits) * ROBUST_MISS_PENALTY);
    }

    private static double missPenalty(@Nullable PearlTrajectoryHit hit, @NotNull BlockPos targetBlock) {
        if (hit == null) {
            return ROBUST_MISS_PENALTY;
        }

        int dx = hit.blockPos().getX() - targetBlock.getX();
        int dz = hit.blockPos().getZ() - targetBlock.getZ();
        if (dx != 0 && dz != 0) {
            return ROBUST_DIAGONAL_MISS_PENALTY;
        }

        return ROBUST_MISS_PENALTY;
    }

    private static double targetHitScore(
            @NotNull Vec3 hit,
            @NotNull Vec3 targetCenter,
            @NotNull Vec3 horizontalAxis
    ) {
        double dx = hit.x - targetCenter.x;
        double dz = hit.z - targetCenter.z;
        double planarDistanceSq = (dx * dx) + (dz * dz);
        double localX = hit.x - Math.floor(hit.x);
        double localZ = hit.z - Math.floor(hit.z);
        double edgeMargin = Math.min(Math.min(localX, 1.0 - localX), Math.min(localZ, 1.0 - localZ));
        double edgeDeficit = Math.max(0.0, ROBUST_EDGE_MARGIN - edgeMargin);
        double cornerX = Math.max(0.0, Math.abs(localX - 0.5) - ROBUST_CORNER_MARGIN);
        double cornerZ = Math.max(0.0, Math.abs(localZ - 0.5) - ROBUST_CORNER_MARGIN);
        double cornerRisk = cornerX * cornerZ;
        double undershoot = Math.max(0.0, -((dx * horizontalAxis.x) + (dz * horizontalAxis.z)));

        return (planarDistanceSq * ROBUST_CENTER_WEIGHT)
                + (edgeDeficit * edgeDeficit * ROBUST_EDGE_WEIGHT)
                + (cornerRisk * ROBUST_CORNER_WEIGHT)
                + (undershoot * undershoot * ROBUST_UNDERSHOOT_WEIGHT);
    }

    public static @Nullable PearlTrajectoryHit projectPearlImpact(
            @NotNull Level level,
            @Nullable Entity owner,
            @NotNull Vec3 start,
            @NotNull Vec3 velocity,
            int maxTicks
    ) {
        if (maxTicks <= 0 || velocity.lengthSqr() < 1.0E-7) {
            return null;
        }

        Vec3 position = start;
        Vec3 motion = velocity;
        for (int tick = 1; tick <= maxTicks; tick++) {
            Vec3 next = position.add(motion);
            BlockHitResult hit = level.clip(new ClipContext(
                    position,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    owner
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return new PearlTrajectoryHit(hit.getBlockPos(), hit.getLocation(), tick);
            }

            position = next;
            motion = motion.scale(VANILLA_DRAG).add(0.0, -VANILLA_GRAVITY, 0.0);
        }
        return null;
    }

    private static @Nullable PearlTrajectoryHit tracePearl(
            @NotNull Vec3 origin,
            @NotNull Candidate candidate,
            @NotNull Profile profile,
            @NotNull Level level,
            @Nullable Entity owner
    ) {
        Vec3 position = launchPositionForYaw(origin, candidate.yawRadians());
        Vec3 motion = candidate.direction().normalize().scale(VANILLA_SPEED);

        for (int tick = 1; tick <= profile.maxTicks(); tick++) {
            Vec3 next = position.add(motion);
            BlockHitResult hit = level.clip(new ClipContext(
                    position,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    owner
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return new PearlTrajectoryHit(hit.getBlockPos(), hit.getLocation(), tick);
            }

            position = next;
            motion = motion.scale(VANILLA_DRAG).add(0.0, -VANILLA_GRAVITY, 0.0);
        }
        return null;
    }

    private static @NotNull Candidate refine(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Vec3 horizontalAxis,
            @NotNull Profile profile,
            @NotNull Candidate center,
            double pitchStep,
            double yawStep
    ) {
        Candidate best = center;
        for (int yawIndex = -3; yawIndex <= 3; yawIndex++) {
            double yaw = center.yawRadians() + (yawIndex * yawStep);
            for (int pitchIndex = -4; pitchIndex <= 4; pitchIndex++) {
                double pitch = Math.clamp(center.pitchDegrees() + (pitchIndex * pitchStep), profile.minPitch(), profile.maxPitch());
                Candidate candidate = simulate(origin, target, horizontalAxis, profile, yaw, pitch);
                if (candidate.score() < best.score()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static @NotNull Candidate simulate(
            @NotNull Vec3 origin,
            @NotNull Vec3 target,
            @NotNull Vec3 horizontalAxis,
            @NotNull Profile profile,
            double yawRadians,
            double pitchDegrees
    ) {
        Vec3 direction = directionFromYawPitch(yawRadians, pitchDegrees * DEG_TO_RAD);
        Vec3 position = origin;
        Vec3 motion = direction.scale(VANILLA_SPEED);
        LandingSample bestLanding = null;
        double closestDistanceSq = planarDistanceSq(position, target);
        int closestTick = 0;

        for (int tick = 1; tick <= profile.maxTicks(); tick++) {
            Vec3 next = position.add(motion);
            Vec3 landingPoint = sampleLandingPoint(position, next, target.y, tick, profile.minLandingTicks());
            if (landingPoint != null) {
                LandingSample sample = LandingSample.of(landingPoint, target, horizontalAxis, tick);
                if (bestLanding == null || profile.landingScore(sample) < profile.landingScore(bestLanding)) {
                    bestLanding = sample;
                }
            }

            double distanceSq = planarDistanceSq(next, target);
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestTick = tick;
            }

            position = next;
            motion = motion.scale(VANILLA_DRAG).add(0.0, -VANILLA_GRAVITY, 0.0);
        }

        double acceptableDistanceSq = bestLanding != null ? bestLanding.acceptableDistanceSq() : Double.POSITIVE_INFINITY;
        double flightTicks = bestLanding != null ? bestLanding.tick() : closestTick;
        double score = profile.score(bestLanding, closestDistanceSq, pitchDegrees, (int) flightTicks);
        return new Candidate(yawRadians, pitchDegrees, direction, flightTicks, acceptableDistanceSq, score, bestLanding, closestDistanceSq);
    }

    private static @Nullable Vec3 sampleLandingPoint(
            @NotNull Vec3 from,
            @NotNull Vec3 to,
            double landingY,
            int tick,
            int minLandingTicks
    ) {
        if (tick < minLandingTicks || from.y < landingY || to.y > landingY) {
            return null;
        }

        double yDelta = from.y - to.y;
        if (yDelta <= 1.0E-7) {
            return null;
        }

        double progress = Math.clamp((from.y - landingY) / yDelta, 0.0, 1.0);
        return from.lerp(to, progress);
    }

    private static double planarDistanceSq(@NotNull Vec3 landingPoint, @NotNull Vec3 target) {
        double dx = landingPoint.x - target.x;
        double dz = landingPoint.z - target.z;
        return (dx * dx) + (dz * dz);
    }

    private static @NotNull Vec3 horizontalAxis(@NotNull Vec3 delta) {
        double horizontal = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (horizontal < MIN_HORIZONTAL_DISTANCE) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(delta.x / horizontal, 0.0, delta.z / horizontal);
    }

    private static @NotNull Vec3 horizontalAxisFromYaw(double yawRadians) {
        return new Vec3(Math.sin(yawRadians), 0.0, Math.cos(yawRadians)).normalize();
    }

    private static @NotNull Vec3 directionFromYawPitch(double yawRadians, double pitchRadians) {
        double horizontal = Math.cos(pitchRadians);
        return new Vec3(
                Math.sin(yawRadians) * horizontal,
                Math.sin(pitchRadians),
                Math.cos(yawRadians) * horizontal
        ).normalize();
    }

    private static @NotNull Vec3 launchPositionForYaw(@NotNull Vec3 origin, double solverYawRadians) {
        double playerYawRadians = Math.atan2(Math.cos(solverYawRadians), Math.sin(solverYawRadians)) - (Math.PI / 2.0);
        return origin.add(
                -Math.cos(playerYawRadians) * PEARL_SPAWN_SIDE_OFFSET,
                0.0,
                -Math.sin(playerYawRadians) * PEARL_SPAWN_SIDE_OFFSET
        );
    }

    private record Profile(
            double minPitch,
            double maxPitch,
            double pitchStep,
            double yawStep,
            int maxTicks,
            int minLandingTicks,
            double targetRadius,
            double idealPitch,
            double idealFlightTicks,
            double blockSnapWeight,
            double undershootWeight,
            double[] yawOffsets
    ) {
        private static @NotNull Profile forType(@NotNull PearlTrajectoryType type) {
            return switch (type) {
                case FLAT -> new Profile(-72.0, 42.0, 1.65, Math.toRadians(0.26), 96, 2, 1.85, 7.0, 18.0, 0.32, 0.72,
                        new double[]{0.0, -0.42, 0.42, -0.9, 0.9});
                case SKY -> new Profile(39.0, 82.0, 2.15, Math.toRadians(0.28), 132, 18, 1.22, 63.0, 58.0, 0.12, 0.18,
                        new double[]{0.0, -0.3, 0.3, -0.72, 0.72});
                case DOUBLE_HIGH -> new Profile(33.0, 84.0, 2.25, Math.toRadians(0.3), 142, 22, 1.28, 58.0, 66.0, 0.16, 0.22,
                        new double[]{0.0, -0.36, 0.36, -0.84, 0.84});
            };
        }

        private double score(@Nullable LandingSample landing, double fallbackDistanceSq, double pitch, int flightTicks) {
            double landingScore = landing != null ? landingScore(landing) : fallbackDistanceSq + 256.0;
            double pitchPenalty = Math.abs(pitch - idealPitch) * 0.008;
            double timePenalty = Math.abs(flightTicks - idealFlightTicks) * 0.006;
            return landingScore + pitchPenalty + timePenalty;
        }

        private double landingScore(@NotNull LandingSample landing) {
            double undershoot = Math.max(0.0, -landing.alongTargetOffset());
            double undershootPenalty = undershoot * undershoot * undershootWeight;
            return landing.pointDistanceSq()
                    + (landing.blockCenterDistanceSq() * blockSnapWeight)
                    + undershootPenalty;
        }
    }

    private record Candidate(
            double yawRadians,
            double pitchDegrees,
            @NotNull Vec3 direction,
            double flightTicks,
            double distanceSq,
            double score,
            @Nullable LandingSample landing,
            double closestDistanceSq
    ) {
        private boolean acceptable(@NotNull Profile profile) {
            return distanceSq <= profile.targetRadius() * profile.targetRadius();
        }
    }

    private record LandingSample(
            @NotNull Vec3 point,
            @NotNull Vec3 snappedBlockCenter,
            double pointDistanceSq,
            double blockCenterDistanceSq,
            double alongTargetOffset,
            int tick
    ) {
        private static @NotNull LandingSample of(
                @NotNull Vec3 landingPoint,
                @NotNull Vec3 target,
                @NotNull Vec3 horizontalAxis,
                int tick
        ) {
            Vec3 snappedBlockCenter = new Vec3(
                    Math.floor(landingPoint.x) + 0.5,
                    target.y,
                    Math.floor(landingPoint.z) + 0.5
            );
            double alongOffset = ((landingPoint.x - target.x) * horizontalAxis.x)
                    + ((landingPoint.z - target.z) * horizontalAxis.z);
            return new LandingSample(
                    landingPoint,
                    snappedBlockCenter,
                    planarDistanceSq(landingPoint, target),
                    planarDistanceSq(snappedBlockCenter, target),
                    alongOffset,
                    tick
            );
        }

        private double qualityDistanceSq() {
            return Math.min(pointDistanceSq, blockCenterDistanceSq);
        }

        private double acceptableDistanceSq() {
            return qualityDistanceSq();
        }
    }

    private record CachedResult(@Nullable PearlAimSolution solution) {
    }

    private record RobustOffset(double yawRadians, double pitchDegrees) {
        private static final RobustOffset[] OFFSETS = {
                new RobustOffset(ROBUST_YAW_STEP, 0.0),
                new RobustOffset(-ROBUST_YAW_STEP, 0.0),
                new RobustOffset(0.0, ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(0.0, -ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(ROBUST_YAW_STEP, ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(ROBUST_YAW_STEP, -ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(-ROBUST_YAW_STEP, ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(-ROBUST_YAW_STEP, -ROBUST_PITCH_STEP_DEGREES),
                new RobustOffset(ROBUST_YAW_STEP * 2.0, 0.0),
                new RobustOffset(ROBUST_YAW_STEP * -2.0, 0.0),
                new RobustOffset(0.0, ROBUST_PITCH_STEP_DEGREES * 2.0),
                new RobustOffset(0.0, ROBUST_PITCH_STEP_DEGREES * -2.0)
        };
    }

    public record PearlTrajectoryHit(
            @NotNull BlockPos blockPos,
            @NotNull Vec3 location,
            int tick
    ) {
    }

    private record CacheKey(
            long originX,
            long originY,
            long originZ,
            long targetX,
            long targetY,
            long targetZ,
            @NotNull PearlTrajectoryType type,
            boolean stationaryFlat,
            boolean fastFlat
    ) {
        private static @NotNull CacheKey of(
                @NotNull Vec3 origin,
                @NotNull Vec3 target,
                @NotNull PearlTrajectoryType type,
                boolean stationaryFlat,
                boolean fastFlat
        ) {
            return new CacheKey(
                    quantize(origin.x),
                    quantize(origin.y),
                    quantize(origin.z),
                    quantize(target.x),
                    quantize(target.y),
                    quantize(target.z),
                    type,
                    stationaryFlat,
                    fastFlat
            );
        }

        private static long quantize(double value) {
            return Math.round(value * CACHE_GRID);
        }
    }
}
