package net.iqaddons.mod.manager;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class KuudraDebugManager {

    private static final KuudraDebugManager INSTANCE = new KuudraDebugManager();

    private Vec3 simulatedKuudraPosition = null;

    private KuudraDebugManager() {}

    public static @NotNull KuudraDebugManager get() {
        return INSTANCE;
    }

    public void start(@NotNull Vec3 kuudraPosition) {
        simulatedKuudraPosition = kuudraPosition;
    }

    public void stop() {
        simulatedKuudraPosition = null;
    }

    public boolean isActive() {
        return simulatedKuudraPosition != null;
    }

    public @NotNull Optional<Vec3> kuudraPosition() {
        return Optional.ofNullable(simulatedKuudraPosition);
    }
}
