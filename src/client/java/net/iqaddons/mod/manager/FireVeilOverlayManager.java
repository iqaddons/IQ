package net.iqaddons.mod.manager;

public final class FireVeilOverlayManager {

    public static final long ABILITY_DURATION_MS = 5_000L;
    public static final long HIDE_PARTICLES_DURATION_MS = 9_000L;
    public static final long READY_VISIBLE_MS = 2_000L;

    private static final FireVeilOverlayManager INSTANCE = new FireVeilOverlayManager();

    private long lastCastMs = 0L;
    private boolean readySoundPlayed = false;

    private FireVeilOverlayManager() {
    }

    public static FireVeilOverlayManager get() {
        return INSTANCE;
    }

    public void recordCast(long now) {
        lastCastMs = now;
        readySoundPlayed = false;
    }

    public void reset() {
        lastCastMs = 0L;
        readySoundPlayed = false;
    }

    public boolean isAbilityActive(long now) {
        return lastCastMs > 0L && now - lastCastMs <= ABILITY_DURATION_MS;
    }

    public boolean shouldHideDefaultParticles(long now) {
        return lastCastMs > 0L && now - lastCastMs <= HIDE_PARTICLES_DURATION_MS;
    }

    public boolean isReadyVisible(long now) {
        long elapsed = now - lastCastMs;
        return lastCastMs > 0L && elapsed > ABILITY_DURATION_MS && elapsed <= ABILITY_DURATION_MS + READY_VISIBLE_MS;
    }

    public boolean shouldDisplayCountdown(long now) {
        return isAbilityActive(now) || isReadyVisible(now);
    }

    public long getRemainingMs(long now) {
        if (lastCastMs <= 0L) return 0L;
        return Math.max(0L, ABILITY_DURATION_MS - (now - lastCastMs));
    }

    public boolean consumeReadySound(long now) {
        if (readySoundPlayed || !isReadyVisible(now)) return false;

        readySoundPlayed = true;
        return true;
    }
}
