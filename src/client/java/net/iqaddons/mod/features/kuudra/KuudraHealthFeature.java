package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.HudRenderer;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@Slf4j
public class KuudraHealthFeature extends KuudraFeature {

    private static final float MAX_HEALTH = 100_000f;
    
    public KuudraHealthFeature() {
        super(
                "kuudraHealthDisplay",
                "Kuudra Health Display",
                () -> PhaseThreeConfig.kuudraHPBossbar,
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
                getHealthColor(bossInfo.currentHealth())
        );
    }

    private @NotNull String formatHealth(float currentHealth) {
        if (currentPhase() == KuudraPhase.BOSS) {
            float effectiveHealth = currentHealth * 4;
            float millions = (effectiveHealth * 2.4f) / 1_000_000f;
            return String.format("%.1fM/2.4M", millions);
        }
        
        return String.format("%.0fk/100k", currentHealth / 1000f);
    }

    private @NotNull RenderColor getHealthColor(float currentHealth) {
        if (currentHealth > 75_000) return RenderColor.fromHex(0x55FF55);
        if (currentHealth > 50_000) return RenderColor.fromHex(0xFFFF55);
        if (currentHealth > 25_000) return RenderColor.fromHex(0xFFAA00);
        return RenderColor.fromHex(0xFF5555);
    }
}