package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraDirectionUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Slf4j
public class KuudraHealthDisplayFeature extends KuudraFeature {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static final float KUUDRA_MAX_HEALTH = 100_000f;
    private static final float BOSS_MULTIPLIER = 4f;
    private static final float DAMAGE_MULTIPLIER = 2400f;

    private static final int HP_DISPLAY_Y = 11;
    private static final double WORLD_TEXT_Y_OFFSET = 10.0;

    private volatile @Nullable KuudraHealthDisplayFeature.KuudraHealthDat cachedHPData = null;

    public KuudraHealthDisplayFeature() {
        super(
                "kuudraHealthBossbar",
                "Kuudra HP Bossbar",
                () -> Configuration.PhaseThreeConfig.kuudraHPBossbar,
                KuudraPhase.STUN, KuudraPhase.DPS, KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        cachedHPData = null;
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        subscribe(EventBus.subscribe(HudRenderEvent.class, this::onHudRender));
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onWorldRender));
        log.info("Kuudra HP Feature activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        cachedHPData = null;
        log.info("Kuudra HP Feature deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        Optional<MagmaCubeEntity> optionalKuudra = KuudraDirectionUtil.findKuudra();
        if (optionalKuudra.isEmpty()) {
            cachedHPData = null;
            return;
        }

        MagmaCubeEntity kuudra = optionalKuudra.get();
        float currentHP = kuudra.getHealth();
        float percentage = (currentHP / KUUDRA_MAX_HEALTH) * 100f;

        String displayText = formatHPDisplay(currentHP, percentage);
        String colorCode = getColorCode(currentHP);

        Vec3d worldPos = new Vec3d(
                kuudra.getX(),
                kuudra.getY() + WORLD_TEXT_Y_OFFSET,
                kuudra.getZ()
        );

        cachedHPData = new KuudraHealthDat(currentHP, percentage, displayText, colorCode, worldPos);
    }

    private void onHudRender(@NotNull HudRenderEvent event) {
        KuudraHealthDat data = cachedHPData;
        if (data == null) return;

        TextRenderer textRenderer = MC.textRenderer;
        String text = "§l" + data.percentage + "%";
        int textWidth = textRenderer.getWidth(text);

        int x = event.centerX() - (textWidth / 2);

        event.drawContext().drawTextWithShadow(
                textRenderer,
                data.colorCode + text,
                x,
                HP_DISPLAY_Y,
                0xFFFFFF
        );
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        KuudraHealthDat data = cachedHPData;
        if (data == null || data.worldPosition == null) return;

        String fullText = data.colorCode + data.displayText;
        RenderColor textColor = RenderColor.fromHex(0xA7171A);

        event.drawText(
                data.worldPosition,
                Text.literal(fullText),
                0.25f,
                true,
                textColor
        );
    }

    private @NotNull String formatHPDisplay(float hp, float percentage) {
        KuudraPhase phase = currentPhase();

        if (phase == KuudraPhase.BOSS) {
            float actualHP = hp * BOSS_MULTIPLIER;
            float damageInMillions = (actualHP * DAMAGE_MULTIPLIER) / 1_000_000f;
            return String.format("%.0fM §c❤", damageInMillions);
        } else {
            return String.format("%.2f%%", percentage);
        }
    }

    @Contract(pure = true)
    private @NotNull String getColorCode(float hp) {
        if (hp > 99_000) return "§a";
        if (hp > 75_000) return "§2";
        if (hp > 50_000) return "§e";
        if (hp > 25_000) return "§6";
        if (hp > 10_000) return "§c";
        return "§4";
    }

    private record KuudraHealthDat(
            float currentHP,
            float percentage,
            String displayText,
            String colorCode,
            Vec3d worldPosition
    ) {}
}