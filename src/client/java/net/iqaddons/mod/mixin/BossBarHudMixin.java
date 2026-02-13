package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.BossBarRenderEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(method = "renderBossBar*", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderBossBar(DrawContext context, int x, int y, BossBar bossBar, CallbackInfo ci) {
        BossBarRenderEvent event = EventBus.post(new BossBarRenderEvent(bossBar));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
