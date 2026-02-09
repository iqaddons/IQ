package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.mob.MagmaCubeEntity;
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
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        var optionalKuudra = KuudraLocationUtil.findKuudra();
        if (optionalKuudra.isEmpty()) return;
        
        MagmaCubeEntity kuudra = optionalKuudra.get();
        event.drawHitbox(kuudra, true, RenderColor.fromArgb(PhaseThreeConfig.kuudraHitboxColor));
    }
}


