package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraDirectionUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraHitboxFeature extends KuudraFeature {

    public KuudraHitboxFeature() {
        super(
                "kuudraHitbox",
                "Kuudra Hitbox",
                () -> PhaseThreeConfig.kuudraHitbox,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onRender));
        log.info("Kuudra Hitbox activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Kuudra Hitbox deactivated");
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        var optionalKuudra = KuudraDirectionUtil.findKuudra();
        if (optionalKuudra.isEmpty()) return;
        
        MagmaCubeEntity kuudra = optionalKuudra.get();
        float tickDelta = event.tickCounter().getTickProgress(true);
        Box hitbox = getBox(kuudra, tickDelta);

        event.drawOutline(hitbox, true, RenderColor.fromArgb(PhaseThreeConfig.kuudraHitboxColor));
    }

    @Contract("_, _ -> new")
    private static @NotNull Box getBox(@NotNull MagmaCubeEntity kuudra, float tickDelta) {
        double x = kuudra.lastX + (kuudra.getX() - kuudra.lastX) * tickDelta;
        double y = kuudra.lastY + (kuudra.getY() - kuudra.lastY) * tickDelta;
        double z = kuudra.lastZ + (kuudra.getZ() - kuudra.lastZ) * tickDelta;

        float width = kuudra.getWidth();
        float height = kuudra.getHeight();
        float halfWidth = width / 2.0f;

        return new Box(
                x - halfWidth,
                y,
                z - halfWidth,
                x + halfWidth,
                y + height,
                z + halfWidth
        );
    }
}


