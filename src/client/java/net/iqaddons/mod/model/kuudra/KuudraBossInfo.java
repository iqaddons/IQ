package net.iqaddons.mod.model.kuudra;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public record KuudraBossInfo(
        Entity bossEntity,
        float currentHealth, float maxHealth,
        double damageReceived,
        Vec3d position
) {

    public static @NotNull KuudraBossInfo empty() {
        return new KuudraBossInfo(null, 0f, 0f, 0d, Vec3d.ZERO);
    }

    public static @NotNull KuudraBossInfo tracked(@NotNull MagmaCubeEntity bossEntity) {
        float clamped = Math.max(0f, bossEntity.getHealth());
        float damageReceived = Math.max(0f, bossEntity.getMaxHealth() - clamped);

        return new KuudraBossInfo(bossEntity,
                clamped, bossEntity.getMaxHealth(),
                damageReceived, bossEntity.getEntityPos()
        );
    }

    public boolean isAlive() {
        return bossEntity != null && bossEntity.isAlive();
    }

    public double getHealthPercentage() {
        return maxHealth > 0 ? (currentHealth / maxHealth) * 100.0 : 0.0;
    }
}
