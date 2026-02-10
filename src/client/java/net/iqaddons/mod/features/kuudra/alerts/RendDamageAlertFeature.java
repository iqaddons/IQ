package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RendDamageAlertFeature extends KuudraFeature {

    private static final float MIN_REND_DAMAGE = 1666f;
    private static final float DAMAGE_MULTIPLIER = 9600f;
    private static final float BOSS_HEALTH_CAP = 25_000f;
    private static final double IGNORE_Y_THRESHOLD = 30.0;

    private volatile float lastKuudraHealth = BOSS_HEALTH_CAP - 1f;

    public RendDamageAlertFeature() {
        super(
                "rendDamageAlert",
                "Rend Damage Alert",
                () -> PhaseFourConfig.rendDamageAlert,
                KuudraPhase.BOSS
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
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (!event.currentPhase().isCombatPhase()) {
            resetState();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(2) || mc.player == null) {
            return;
        }

        if (mc.player.getY() > IGNORE_Y_THRESHOLD) return;

        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive()) return;

        float currentHealth = bossInfo.currentHealth();
        if (currentHealth > BOSS_HEALTH_CAP) return;

        float diff = Math.max(0f, lastKuudraHealth - currentHealth);
        if (diff > MIN_REND_DAMAGE) {
            float scaledDamage = diff * DAMAGE_MULTIPLIER;
            MessageUtil.INFO.sendMessage("§fSomeone pulled for %s%s §fdamage at §a%.2fs§f."
                    .formatted(getDamageColor(diff), formatDamage(scaledDamage),
                            currentContext().phaseDuration().toMillis() / 1000.0)
            );

            log.debug("Rend pull detected: raw={} scaled={}", diff, scaledDamage);
        }

        lastKuudraHealth = currentHealth;
    }

    private void resetState() {
        lastKuudraHealth = BOSS_HEALTH_CAP - 1f;
    }

    private @NotNull String formatDamage(float number) {
        if (number >= 1_000_000f) return String.format("%.2fM", number / 1_000_000f);
        if (number >= 1_000f) return String.format("%.2fK", number / 1_000f);
        return String.format("%.0f", number);
    }

    @Contract(pure = true)
    private @NotNull String getDamageColor(float damage) {
        if (damage <= 4166f) return "§c";
        if (damage <= 7291f) return "§e";
        return "§a";
    }
}