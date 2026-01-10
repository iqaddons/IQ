package net.iqaddons.mod.utils;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@UtilityClass
public class KuudraDirectionUtil {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static final float KUUDRA_WIDTH = 15.3f;
    private static final float KUUDRA_WIDTH_TOLERANCE = 0.5f;
    private static final float KUUDRA_MAX_HEALTH = 100_000f;

    public static final float BOSS_HEALTH_THRESHOLD = 25_000f;
    public static final float BOSS_SPAWN_HEALTH_MIN = 24_900f;

    public static @NotNull Optional<MagmaCubeEntity> findKuudra() {
        ClientWorld world = MC.world;
        if (world == null) return Optional.empty();
        return findKuudraInternal(world);
    }

    private static @NotNull Optional<MagmaCubeEntity> findKuudraInternal(@NotNull ClientWorld world) {
        for (var entity : world.getEntities()) {
            if (entity instanceof MagmaCubeEntity magmaCube) {
                if (isKuudra(magmaCube)) {
                    return Optional.of(magmaCube);
                }
            }
        }

        return Optional.empty();
    }

    public static boolean isKuudra(@Nullable MagmaCubeEntity entity) {
        if (entity == null) return false;

        float width = entity.getWidth();
        float health = entity.getHealth();

        return Math.abs(width - KUUDRA_WIDTH) < KUUDRA_WIDTH_TOLERANCE
                && health <= KUUDRA_MAX_HEALTH
                && health > 0;
    }

    public static boolean isInPhase4(@NotNull MagmaCubeEntity kuudra) {
        return kuudra.getHealth() <= BOSS_HEALTH_THRESHOLD;
    }

    public static boolean justBossSpawned(@NotNull MagmaCubeEntity kuudra) {
        float health = kuudra.getHealth();
        return health <= BOSS_HEALTH_THRESHOLD && health > BOSS_SPAWN_HEALTH_MIN;
    }

    public static float getHealth(@NotNull MagmaCubeEntity kuudra) {
        return kuudra.getHealth();
    }

    public static @NotNull SpawnDirection getSpawnDirection(@NotNull MagmaCubeEntity kuudra) {
        double x = kuudra.getX();
        double z = kuudra.getZ();

        if (x < -128) return SpawnDirection.RIGHT;
        if (z > -84) return SpawnDirection.FRONT;
        if (x > -72) return SpawnDirection.LEFT;
        if (z < -132) return SpawnDirection.BACK;

        return SpawnDirection.UNKNOWN;
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


