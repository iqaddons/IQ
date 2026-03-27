package net.iqaddons.mod.mixin.feature.transparency;

import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.render.entity.MagmaCubeEntityRenderer;
import net.minecraft.client.render.entity.state.SlimeEntityRenderState;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(MagmaCubeEntityRenderer.class)
public abstract class MagmaCubeRendererStateMixin {

    @Unique
    private static final Predicate<KuudraPhase> TRANSPARENCY_PHASES = KuudraPhase.isOneOf(
            KuudraPhase.STUN, KuudraPhase.DPS
    );

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/mob/MagmaCubeEntity;Lnet/minecraft/client/render/entity/state/SlimeEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void iq$updateKuudraOpacityState(
            MagmaCubeEntity entity,
            SlimeEntityRenderState renderState,
            float tickProgress,
            CallbackInfo ci
    ) {
        float opacity = 1.0f;

        if (PhaseThreeConfig.kuudraTransparency && TRANSPARENCY_PHASES.test(KuudraStateManager.get().phase())) {
            opacity = Math.clamp(PhaseThreeConfig.kuudraOpacity, 0.05f, 1.0f);
        }

        ((KuudraTransparencyState) renderState).iq$setKuudraOpacity(opacity);
    }
}