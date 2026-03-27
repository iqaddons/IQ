package net.iqaddons.mod.mixin.feature.transparency;

import net.minecraft.client.render.entity.state.SlimeEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SlimeEntityRenderState.class)
public class SlimeEntityRenderStateMixin implements KuudraTransparencyState {

    @Unique
    private float iq$kuudraOpacity = 1.0f;

    @Override
    public float iq$getKuudraOpacity() {
        return this.iq$kuudraOpacity;
    }

    @Override
    public void iq$setKuudraOpacity(float opacity) {
        this.iq$kuudraOpacity = opacity;
    }
}