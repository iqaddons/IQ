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

@Slf4j
public class KuudraHealthWidget extends HudWidget {

    private static final float BOSS_MULTIPLIER = 4f;
    private static final float DAMAGE_MULTIPLIER = 2400f;
    private static final float BOSS_HEALTH_THRESHOLD = 25_000f;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final HudLine titleLine = HudLine.of("§4§lKuudra Health");
    private final HudLine healthLine = HudLine.of("§c❤ 100,000 §8(§c100%§8)");
    private final HudLine damageLine = HudLine.of("§fDamage: §e0.0M")
            .showWhen(() -> stateManager.phase() == KuudraPhase.BOSS
                    && stateManager.context().bossInfo().currentHealth() <= BOSS_HEALTH_THRESHOLD
            );

    public KuudraHealthWidget() {
        super(
                "kuudraHealth",
                "Kuudra Health",
                10.0f, 280.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseThreeConfig.kuudraHealthDisplay);

        KuudraPhase phase = stateManager.phase();
        setVisibilityCondition(() -> phase == KuudraPhase.STUN
                || phase == KuudraPhase.DPS
                || phase == KuudraPhase.BOSS);

        setExampleLines(List.of(
                titleLine,
                healthLine,
                damageLine
        ));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLines(titleLine, healthLine, damageLine);

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
        float currentHealth = bossInfo.currentHealth();
        healthLine.text(String.format("§c❤ %.0f §8(§c%.2f%%§8)",
                currentHealth, bossInfo.getHealthPercentage())
        );

        float actualHP = currentHealth * BOSS_MULTIPLIER;
        float damageInMillions = (actualHP * DAMAGE_MULTIPLIER) / 1_000_000f;
        damageLine.text(String.format("§fDamage: §e%.1fM", damageInMillions));

        markDimensionsDirty();
    }
}