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
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@Slf4j
public class KuudraHealthWidget extends HudWidget {

    private static final float BOSS_PHASE_MAX_HEALTH = 24_000f;
    private static final float PRE_BOSS_MIN_HEALTH = 25_000f;
    private static final float PRE_BOSS_MAX_HEALTH = 100_000f;
    private static final String RED = "§c";
    private static final String DARK_RED = "§4";
    private static final String DARK_GRAY = "§8";
    private static final String BOLD = "§l";
    private static final String HEART = "\u2764";

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final HudLine titleLine = HudLine.of(DARK_RED + BOLD + "Kuudra Health")
            .showWhen(() -> !PhaseThreeConfig.KuudraHealthWidgetConfig.renderHealthOnly);
    private final HudLine healthLine = HudLine.of(RED + HEART + " 100,000 " + DARK_GRAY + "(" + RED + "100%" + DARK_GRAY + ")");

    public KuudraHealthWidget() {
        super(
                "kuudraHealth",
                "Kuudra Health",
                439.0f, 19.653473f,
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

    @Override
    protected float getLineStartX(@NotNull Font textRenderer, @NotNull HudLine line) {
        return Math.max(0.0f, (getWidth() - line.getWidth(textRenderer)) / 2.0f);
    }

    @Override
    public boolean isNanoLineCentered(@NotNull Font textRenderer, @NotNull HudLine line) {
        return true;
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
        String percentageColor = getPercentageColorCode(healthPercentage);

        if (stateManager.phase() == KuudraPhase.BOSS) {
            healthLine.text(buildHealthComponent(
                    String.format(Locale.ROOT, "%.1fM", currentHealth / 100f),
                    healthPercentage,
                    percentageColor
            )
            );
        } else {
            healthLine.text(buildHealthComponent(
                    String.format(Locale.ROOT, "%,.0f", currentHealth),
                    healthPercentage,
                    percentageColor
            )
            );
        }

        markDimensionsDirty();
    }

    private @NotNull Component buildHealthComponent(
            @NotNull String healthText,
            double healthPercentage,
            @NotNull String percentageColor
    ) {
        return Component.literal(String.format(Locale.ROOT,
                "%s%s %s %s(%s%.1f%%%s)",
                RED, HEART, healthText, DARK_GRAY, percentageColor, healthPercentage, DARK_GRAY
        ));
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

    private @NotNull String getPercentageColorCode(double healthPercentage) {
        if (healthPercentage >= 75.0) {
            return colorCodeFor(PhaseThreeConfig.KuudraHealthWidgetConfig.highPercentageColor, "\u00A7a");
        }

        if (healthPercentage >= 50.0) {
            return colorCodeFor(PhaseThreeConfig.KuudraHealthWidgetConfig.midPercentageColor, "\u00A7e");
        }

        if (healthPercentage >= 25.0) {
            return colorCodeFor(PhaseThreeConfig.KuudraHealthWidgetConfig.lowPercentageColor, "\u00A76");
        }

        return colorCodeFor(PhaseThreeConfig.KuudraHealthWidgetConfig.criticalPercentageColor, "\u00A7c");
    }

    private @NotNull String colorCodeFor(int color, @NotNull String fallback) {
        return switch (color & 0xFFFFFF) {
            case 0x55FF55 -> "\u00A7a";
            case 0xFFFF55 -> "\u00A7e";
            case 0xFFAA00 -> "\u00A76";
            case 0xFF5555 -> "\u00A7c";
            case 0xAA0000 -> "\u00A74";
            case 0xAAAAAA -> "\u00A77";
            case 0x555555 -> "\u00A78";
            default -> fallback;
        };
    }
}
