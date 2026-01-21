package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.iqaddons.mod.utils.hud.element.HudAnchor;
import net.iqaddons.mod.utils.hud.element.HudWidget;
import net.iqaddons.mod.utils.render.HudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class KuudraHealthWidget extends HudWidget {

    private static final float KUUDRA_MAX_HEALTH = 100_000f;
    private static final float BOSS_MULTIPLIER = 4f;
    private static final float DAMAGE_MULTIPLIER = 2400f;
    private static final float BOSS_HEALTH_THRESHOLD = 25_000f;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private float currentHP = 0;
    private float percentage = 0;
    private boolean kuudraFound = false;

    private EventBus.Subscription<ClientTickEvent> tickSubscription;

    private final HudLine titleLine;
    private final HudLine healthLine;
    private final HudLine damageLine;

    public KuudraHealthWidget() {
        super(
                "kuudraHealth",
                "Kuudra Health",
                10.0f, 280.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§4§lKuudra Health");
        healthLine = HudLine.of("§c❤ 100,000 §8(§c100%§8)");
        damageLine = HudLine.of("§fDamage: §e0.0M")
                .showWhen(() -> stateManager.phase() == KuudraPhase.BOSS && currentHP <= BOSS_HEALTH_THRESHOLD);

        setEnabledSupplier(() -> PhaseThreeConfig.kuudraHPBossbar);
        setVisibilityCondition(this::isInCombatPhase);

        setExampleLines(List.of(
                HudLine.of("§4§lKuudra Health"),
                HudLine.of("§c❤ 52,340 §8(§c52.34%§8)"),
                HudLine.of("§fDamage: §e125.6M")
        ));
    }

    @Override
    protected void onActivate() {
        currentHP = 0;
        percentage = 0;
        kuudraFound = false;

        clearLines();
        addLines(titleLine, healthLine, damageLine);

        tickSubscription = EventBus.subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }

        kuudraFound = false;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(2)) return;

        var optionalKuudra = KuudraLocationUtil.findKuudra();
        if (optionalKuudra.isEmpty()) {
            kuudraFound = false;
            return;
        }

        MagmaCubeEntity kuudra = optionalKuudra.get();
        kuudraFound = true;

        currentHP = kuudra.getHealth();
        percentage = (currentHP / KUUDRA_MAX_HEALTH) * 100f;

        updateDisplay();
    }

    private void updateDisplay() {
        healthLine.text(String.format("§c❤ %.0f §8(§c%.2f%%§8)", currentHP, percentage));

        if (stateManager.phase() == KuudraPhase.BOSS && currentHP <= BOSS_HEALTH_THRESHOLD) {
            float actualHP = currentHP * BOSS_MULTIPLIER;
            float damageInMillions = (actualHP * DAMAGE_MULTIPLIER) / 1_000_000f;
            damageLine.text(String.format("§fDamage: §e%.1fM", damageInMillions));
        }

        markDimensionsDirty();
    }

    private boolean isInCombatPhase() {
        KuudraPhase phase = stateManager.phase();
        return phase == KuudraPhase.STUN
                || phase == KuudraPhase.DPS
                || phase == KuudraPhase.BOSS;
    }

    @Override
    public void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (shouldRender() && isActive() && kuudraFound) {
            float absX = getAbsoluteX();
            float absY = getAbsoluteY();
            int height = getScaledHeight();

            int barY = (int) (absY + height + 2);
            int barWidth = Math.max(getScaledWidth(), 120);

            float healthPercent = currentHP / KUUDRA_MAX_HEALTH;

            HudRenderer.drawProgressBar(
                    context,
                    (int) absX, barY,
                    barWidth, 6,
                    healthPercent,
                    getHealthBarColor(currentHP),
                    0xFF282828
            );
        }
    }

    private int getHealthBarColor(float hp) {
        if (hp > 99_000) return 0xFF55FF55;
        if (hp > 75_000) return 0xFF00AA00;
        if (hp > 50_000) return 0xFFFFFF55;
        if (hp > 25_000) return 0xFFFFAA00;
        if (hp > 10_000) return 0xFFFF5555;
        return 0xFFAA0000;
    }
}