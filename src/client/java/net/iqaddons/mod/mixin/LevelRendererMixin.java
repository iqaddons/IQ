package net.iqaddons.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.utils.render.WorldOverlayBatch;
import net.iqaddons.mod.utils.render.WorldOverlayPhaseSubmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void iq$submitWorldOverlays(LevelRenderState state, SubmitNodeCollector collector,
                                       boolean renderBlockOutline, CallbackInfo ci) {
        WorldOverlayBatch batch = WorldOverlayBatch.begin(
                state.cameraRenderState.pos,
                (WorldOverlayPhaseSubmitter) collector.order(0)
        );
        try {
            EventBus.post(new WorldRenderEvent(collector, collector, new PoseStack(),
                    state.cameraRenderState, Minecraft.getInstance().getDeltaTracker()));
            batch.flush(collector);
        } finally {
            WorldOverlayBatch.end(batch);
        }
    }
}
