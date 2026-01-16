package net.iqaddons.mod.state;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.state.supply.PileLocation;
import net.iqaddons.mod.state.supply.PreSpot;
import net.iqaddons.mod.state.supply.SupplyPosition;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
public final class SupplyStateManager {

    private static final SupplyStateManager INSTANCE = new SupplyStateManager();

    private final List<SupplyPosition> activeSupplies = new CopyOnWriteArrayList<>();
    private final List<PileLocation> remainingPiles = new CopyOnWriteArrayList<>();

    private volatile PreSpot detectedPreSpot = null;
    private volatile boolean preSpotLocked = false;
    private volatile Instant suppliesPhaseStart = null;
    private volatile int missingPre = 0;
    private volatile int suppliesCollected = 0;

    public void startSuppliesPhase() {
        suppliesPhaseStart = Instant.now();
        log.debug("Supplies phase started");
    }

    public void updateSupplyPositions(@NotNull List<SupplyPosition> positions) {
        activeSupplies.clear();
        activeSupplies.addAll(positions);
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView List<SupplyPosition> getActiveSupplies() {
        return Collections.unmodifiableList(activeSupplies);
    }

    public boolean tryDetectPreSpot(@NotNull Vec3d playerPos) {
        if (preSpotLocked) return false;

        PreSpot detected = PreSpot.fromPlayerPosition(playerPos);
        if (detected != null) {
            detectedPreSpot = detected;
            preSpotLocked = true;
            log.info("Pre spot detected: {}", detected.getDisplayName());
            return true;
        }

        return false;
    }

    public boolean hasPreSupply() {
        if (detectedPreSpot == null) return false;

        Vec3d preLoc = detectedPreSpot.getLocation();
        double radius = 18.0;

        return activeSupplies.stream()
                .anyMatch(supply -> supply.isNear(preLoc, radius));
    }

    public @Nullable Boolean hasSecondarySupply() {
        if (detectedPreSpot == null || !detectedPreSpot.hasSecondaryLocation()) {
            return null;
        }

        Vec3d secondaryLoc = detectedPreSpot.getSecondaryLocation();
        double radius = detectedPreSpot.getSecondaryCheckRadius();

        return activeSupplies.stream()
                .anyMatch(supply -> supply.isNear(secondaryLoc, radius));
    }

    public @Nullable SupplyPosition findSupplyNear(@NotNull Vec3d location, double radius) {
        return activeSupplies.stream()
                .filter(supply -> supply.isNear(location, radius))
                .findFirst()
                .orElse(null);
    }

    public void markPileCompleted(@NotNull Vec3d armorStandPos) {
        remainingPiles.removeIf(pile -> pile.isNearby(armorStandPos));
    }

    public void setMissingPre(int value) {
        if (value != missingPre) {
            missingPre = value;
            log.debug("Missing pre set to: {}", value);
        }
    }

    public void reset() {
        activeSupplies.clear();
        remainingPiles.clear();
        remainingPiles.addAll(PileLocation.DEFAULT_PILES);

        detectedPreSpot = null;
        preSpotLocked = false;
        suppliesPhaseStart = null;

        missingPre = 0;
        suppliesCollected = 0;
        log.debug("Supply state reset");
    }

    public long getElapsedTimeMillis() {
        if (suppliesPhaseStart == null) return 0;
        return System.currentTimeMillis() - Objects.requireNonNull(suppliesPhaseStart).toEpochMilli();
    }

    public long getElapsedTimeSeconds() {
        return (long) (getElapsedTimeMillis() / 1000.0);
    }

    @Contract(pure = true)
    public @NotNull String getTimeColor() {
        return switch (getTimeTier()) {
            case 0 -> "§d§l";
            case 1 -> "§9§l";
            case 2 -> "§a§l";
            case 3 -> "§2§l";
            case 4 -> "§e§l";
            default -> "§c§l";
        };
    }

    private int getTimeTier() {
        long time = getElapsedTimeMillis();
        if (time < 20000) return 0;
        if (time < 24000) return 1;
        if (time < 26000) return 2;
        if (time < 27500) return 3;
        if (time < 29000) return 4;
        return 5;
    }

    public static SupplyStateManager get() {
        return INSTANCE;
    }
}