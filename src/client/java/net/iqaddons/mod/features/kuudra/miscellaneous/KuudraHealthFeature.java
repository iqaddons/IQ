package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
public class KuudraHealthFeature extends KuudraFeature {

    public KuudraHealthFeature() {
        super(
                "kuudraHealthDisplay",
                "Kuudra Health Display",
                () -> PhaseThreeConfig.kuudraHealth,
                Stream.concat(
                        KuudraPhase.PRE_COMBAT_PHASES.stream(),
                        KuudraPhase.COMBAT_PHASES.stream()
                ).toArray(KuudraPhase[]::new)
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive()) return;

        var kuudraPos = bossInfo.bossEntity().getEntityPos();
        event.drawText(
                kuudraPos.add(0, 10, 0),
                Text.literal(formatHealth(bossInfo.currentHealth())),
                0.25f,
                true,
                RenderColor.fromArgb(getHealthColor(bossInfo.currentHealth()))
        );
    }

    private @NotNull String formatHealth(float currentHealth) {
        if (currentPhase() == KuudraPhase.BOSS) {
            float millions = currentHealth / 100f;
            return String.format(Locale.ROOT, "%.1fM/240M", millions);
        }
        
        return String.format(Locale.ROOT, "%,.0f/100.000", currentHealth);
    }

    private int getHealthColor(float currentHealth) {
        if (currentHealth > 75_000) return PhaseThreeConfig.KuudraHealthColorConfig.high;
        if (currentHealth > 50_000) return PhaseThreeConfig.KuudraHealthColorConfig.mid;
        if (currentHealth > 25_000) return PhaseThreeConfig.KuudraHealthColorConfig.low;
        return PhaseThreeConfig.KuudraHealthColorConfig.critical;
    }
}