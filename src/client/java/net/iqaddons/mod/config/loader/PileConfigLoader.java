package net.iqaddons.mod.config.loader;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.spot.PileLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

@Slf4j
public class PileConfigLoader {

    private static final PileConfigLoader INSTANCE = new PileConfigLoader();

    private static final List<PileLocation> DEFAULT_PILES = List.of(
            new PileLocation("Shop", new Vec3(-98.5, 78.4, -113.5), 7),
            new PileLocation("Triangle", new Vec3(-94.5, 78.4, -106.5), 6),
            new PileLocation("Equals", new Vec3(-106.5, 78.4, -99.5), 5),
            new PileLocation("Slash", new Vec3(-98.5, 78.4, -99.5), 4),
            new PileLocation("X Cannon", new Vec3(-110.5, 78.4, -106.5), 2),
            new PileLocation("X", new Vec3(-106.5, 78.4, -113.5), 1)
    );

    public @NotNull @UnmodifiableView List<PileLocation> load() {
        return DEFAULT_PILES;
    }

    public @NotNull @UnmodifiableView List<PileLocation> reload() {
        log.info("Using built-in pile locations");
        return DEFAULT_PILES;
    }

    public @NotNull @UnmodifiableView List<PileLocation> getCached() {
        return DEFAULT_PILES;
    }

    public static PileConfigLoader get() {
        return INSTANCE;
    }
}

