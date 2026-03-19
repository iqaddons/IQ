package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CountdownLagCompensationUtil {

    private static final long LAG_COMPENSATION_THRESHOLD_MS = 120L;
    private static final long MAX_COMPENSATION_PER_TICK_MS = 1500L;

    public static long applyLagCompensation(long countdownEndMillis, long previousTickMillis, long currentTickMillis, long expectedTickIntervalMs) {
        if (countdownEndMillis <= 0L || previousTickMillis <= 0L || currentTickMillis <= previousTickMillis) {
            return countdownEndMillis;
        }

        long elapsedMs = currentTickMillis - previousTickMillis;
        long lagMs = elapsedMs - Math.max(1L, expectedTickIntervalMs);
        if (lagMs <= LAG_COMPENSATION_THRESHOLD_MS) {
            return countdownEndMillis;
        }

        // Cap one-step compensation so a single freeze does not overcorrect indefinitely.
        return countdownEndMillis + Math.min(lagMs, MAX_COMPENSATION_PER_TICK_MS);
    }
}
