package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.HudRenderer;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@Slf4j
public class KuudraHealthFeature extends KuudraFeature {

    private static final float MAX_HEALTH = 100_000f;
    
    private float currentHP = 0f;
    private Vec3d kuudraPos = null;

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
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onWorldRender);
        subscribe(HudRenderEvent.class, this::onHudRender);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isNthTick(2)) return;

        var kuudraOpt = KuudraLocationUtil.findKuudra();
        if (kuudraOpt.isEmpty()) {
            currentHP = 0;
            kuudraPos = null;
            return;
        }

        MagmaCubeEntity kuudra = kuudraOpt.get();
        currentHP = kuudra.getHealth();
        kuudraPos = new Vec3d(kuudra.getX(), kuudra.getY(), kuudra.getZ());
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (kuudraPos == null || currentHP <= 0) return;

        event.drawText(
                kuudraPos.add(0, 10, 0),
                Text.literal(formatWorldHealth()),
                0.25f,
                true,
                getHealthColor()
        );
    }

    private void onHudRender(@NotNull HudRenderEvent event) {
        if (currentHP <= 0) return;

        float percentage = (currentHP / MAX_HEALTH) * 100f;
        String percentText = String.format("§l%.2f%%", percentage);
        
        int textWidth = mc.textRenderer.getWidth(percentText);
        int x = (event.screenWidth() / 2) - (textWidth / 2);
        int y = 11;
        
        HudRenderer.drawText(event.drawContext(), percentText, x, y, 0xFFFFFF);
    }

    private @NotNull String formatWorldHealth() {
        if (currentPhase() == KuudraPhase.BOSS) {
            float effectiveHP = currentHP * 4;
            float millions = (effectiveHP * 2.4f) / 1_000_000f;
            return String.format("%.1fM/2.4M", millions);
        }
        
        return String.format("%.0fk/100k", currentHP / 1000f);
    }

    private @NotNull RenderColor getHealthColor() {
        if (currentHP > 75_000) return RenderColor.fromHex(0x55FF55);
        if (currentHP > 50_000) return RenderColor.fromHex(0xFFFF55);
        if (currentHP > 25_000) return RenderColor.fromHex(0xFFAA00);
        return RenderColor.fromHex(0xFF5555);
    }
}