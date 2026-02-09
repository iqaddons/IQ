package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RendDamageAlertFeature extends KuudraFeature {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static final float MIN_REND_DAMAGE = 1666f;
    private static final float DAMAGE_MULTIPLIER = 9600f;
    private static final double ARENA_Y_THRESHOLD = 10.0;
    private static final double IGNORE_Y_THRESHOLD = 30.0;
    private static final float MIN_VALID_HEALTH = 100f;

    private volatile float lastKuudraHealth = 25000f;
    private volatile long bossStartTime = 0;
    private volatile boolean inBoss = false;
    private volatile boolean kuudraDead = false;

    public RendDamageAlertFeature() {
        super(
                "rendDamageAlert",
                "Rend Damage Alert",
                () -> PhaseFourConfig.rendDamageAlert,
                KuudraPhase.DPS, KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        resetState();

        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        resetState();
        log.info("Rend Damage Alert deactivated");
    }

    private void resetState() {
        lastKuudraHealth = 25000f;
        bossStartTime = 0;
        inBoss = false;
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.currentPhase().isCombatPhase()) {
            if (!inBoss) resetState();
        } else {
            resetState();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (MC.player == null) return;

        if (MC.player.getY() < ARENA_Y_THRESHOLD && !inBoss) {
            inBoss = true;
            bossStartTime = System.currentTimeMillis();
        }

        if (MC.player.getY() > IGNORE_Y_THRESHOLD) {
            return;
        }

        checkRendDamage();
    }

    private void checkRendDamage() {
        var optionalKuudra = KuudraLocationUtil.findKuudra();
        if (optionalKuudra.isEmpty()) return;

        MagmaCubeEntity kuudra = optionalKuudra.get();
        if (!kuudra.isAlive()) {
            KuudraLocationUtil.invalidateCache();
            return;
        }

        float currentHealth = kuudra.getHealth();
        if (currentHealth > KuudraLocationUtil.BOSS_HEALTH_THRESHOLD) {
            return;
        }

        if (currentHealth <= MIN_VALID_HEALTH) {
            if (!kuudraDead && currentHealth <= 0) {
                kuudraDead = true;
                KuudraLocationUtil.invalidateCache();
            }

            return;
        }

        float damage = lastKuudraHealth - currentHealth;
        if (damage >= MIN_REND_DAMAGE) {
            float actualDamage = damage * DAMAGE_MULTIPLIER;
            String formattedDamage = formatHealth(actualDamage);
            String damageColor = getDamageColor(damage);
            double timeSinceStart = (System.currentTimeMillis() - bossStartTime) / 1000.0;

            MessageUtil.INFO.sendMessage("Someone pulled for %s%s §7damage at §a%.2fs§7."
                    .formatted(damageColor, formattedDamage, timeSinceStart)
            );
            log.debug("Rend damage: {} ({})", formattedDamage, damage);
        }

        lastKuudraHealth = currentHealth;
    }

    private @NotNull String formatHealth(float number) {
        if (number >= 1_000_000) {
            return String.format("%.2fm", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.2fk", number / 1_000);
        } else {
            return String.format("%.2f", number);
        }
    }

    @Contract(pure = true)
    private @NotNull String getDamageColor(float damage) {
        if (damage > 7291) return "§a";
        else if (damage >= 4166) return "§e";
        else if (damage >= 1666) return "§c";
        return "§f";
    }
}