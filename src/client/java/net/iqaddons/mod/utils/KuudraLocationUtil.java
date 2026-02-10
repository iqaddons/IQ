package net.iqaddons.mod.utils;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@UtilityClass
public class KuudraLocationUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int KUUDRA_SIZE = 30;

    private static MagmaCubeEntity cachedKuudra = null;

    public static @NotNull Optional<MagmaCubeEntity> findKuudra() {
        ClientWorld world = mc.world;
        if (world == null) {
            cachedKuudra = null;
            return Optional.empty();
        }

        if (cachedKuudra != null && cachedKuudra.isAlive() && isKuudra(cachedKuudra)) {
            return Optional.of(cachedKuudra);
        }

        cachedKuudra = findKuudraBoss(world);
        return Optional.ofNullable(cachedKuudra);
    }

    private static @Nullable MagmaCubeEntity findKuudraBoss(@NotNull ClientWorld world) {
        MagmaCubeEntity kuudra = null;
        double maxY = 0;
        int cubesFound = 0;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof MagmaCubeEntity cube)) continue;
            if (cube.getSize() != KUUDRA_SIZE) continue;

            double y = cube.getY();
            cubesFound++;
            if (y > maxY) {
                kuudra = cube;
                maxY = y;
            }
        }

        if (kuudra == null || cubesFound == 0) return null;
        if (kuudra.getHealth() <= 0) return null;
        if (cubesFound == 2) {
            cachedKuudra = kuudra;
            return kuudra;
        }

        return kuudra;
    }

    public static boolean isKuudra(@Nullable MagmaCubeEntity entity) {
        if (entity == null) return false;

        int size = entity.getSize();
        float health = entity.getHealth();

        return size == KUUDRA_SIZE && health > 0;
    }

    public static @NotNull SpawnDirection getDirection(@NotNull Entity entity) {
        double x = entity.getX();
        double z = entity.getZ();

        if (x < -128) return SpawnDirection.RIGHT;
        if (z > -84) return SpawnDirection.FRONT;
        if (x > -72) return SpawnDirection.LEFT;
        if (z < -132) return SpawnDirection.BACK;

        return SpawnDirection.UNKNOWN;
    }

    public static void invalidateCache() {
        cachedKuudra = null;
    }

    @Getter
    public enum SpawnDirection {
        RIGHT("RIGHT", "§c§l"),
        FRONT("FRONT", "§2§l"),
        LEFT("LEFT", "§a§l"),
        BACK("BACK", "§4§l"),
        UNKNOWN("UNKNOWN", "§f§l");

        private final String name;
        private final String colorCode;

        SpawnDirection(String name, String colorCode) {
            this.name = name;
            this.colorCode = colorCode;
        }

        @Contract(pure = true)
        public @NotNull String getFormattedName() {
            return colorCode + name + "!";
        }
    }
}


