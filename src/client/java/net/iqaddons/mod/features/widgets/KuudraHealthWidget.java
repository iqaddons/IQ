package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraBossInfo;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@Slf4j
public class KuudraHealthWidget extends HudWidget {

    private static final float BOSS_PHASE_MAX_HEALTH = 24_000f;
    private static final float PRE_BOSS_MIN_HEALTH = 24_000f;
    private static final float PRE_BOSS_MAX_HEALTH = 100_000f;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final HudLine titleLine = HudLine.of("§4§lKuudra Health");
    private final HudLine healthLine = HudLine.of("§c❤ 100,000 §8(§c100%§8)");

    public KuudraHealthWidget() {
        super(
                "kuudraHealth",
                "Kuudra Health",
                10.0f, 280.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseThreeConfig.kuudraHealthDisplay);

        setVisibilityCondition(() -> {
            KuudraPhase phase = stateManager.phase();
            return KuudraPhase.COMBAT_PHASES.contains(phase);
        });

        setExampleLines(List.of(
                titleLine,
                healthLine
        ));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLines(titleLine, healthLine);

        subscribe(ClientTickEvent.class, this::onTick);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(2)) return;

        var bossInfo = stateManager.context().bossInfo();
        if (!bossInfo.isAlive()) return;

        updateDisplay(bossInfo);
    }

    private void updateDisplay(@NotNull KuudraBossInfo bossInfo) {
        KuudraPhase phase = stateManager.phase();
        float currentHealth = bossInfo.currentHealth();
        double healthPercentage = getHealthPercentageByPhase(phase, currentHealth);

        if (stateManager.phase() == KuudraPhase.BOSS) {
            healthLine.text(String.format(Locale.ROOT, "§c❤ %.1fM/240M §8(§c%.1f%%§8)",
                    currentHealth / 100f,
                    healthPercentage)
            );
        } else {
            healthLine.text(String.format(Locale.ROOT, "§c❤ %,.0f §8(§c%.1f%%§8)",
                    currentHealth,
                    healthPercentage)
            );
        }

        markDimensionsDirty();
    }

    private double getHealthPercentageByPhase(@NotNull KuudraPhase phase, float currentHealth) {
        if (phase == KuudraPhase.BOSS) {
            return clampPercentage((currentHealth / BOSS_PHASE_MAX_HEALTH) * 100.0);
        }

        if (phase == KuudraPhase.STUN || phase == KuudraPhase.DPS || phase == KuudraPhase.SKIP) {
            float normalizedHealth = currentHealth - PRE_BOSS_MIN_HEALTH;
            float preBossRange = PRE_BOSS_MAX_HEALTH - PRE_BOSS_MIN_HEALTH;
            return clampPercentage((normalizedHealth / preBossRange) * 100.0);
        }

        return bossInfoFallbackPercentage(currentHealth);
    }

    private double bossInfoFallbackPercentage(float currentHealth) {
        var bossInfo = stateManager.context().bossInfo();
        return bossInfo.maxHealth() <= 0f ? 0.0 : clampPercentage((currentHealth / bossInfo.maxHealth()) * 100.0);
    }

    private double clampPercentage(double value) {
        return Math.min(100.0, Math.max(0.0, value));
    }
}