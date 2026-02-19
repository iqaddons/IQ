package net.iqaddons.mod.mixin;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.BossBarRenderEvent;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Final
    @Shadow
    Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderAll(DrawContext context, CallbackInfo ci) {
        if (!KuudraGeneralConfig.hideKuudraBossBar) {
            return;
        }

        if (!KuudraStateManager.get().isInRun()) {
            return;
        }

        boolean hasKuudraBar = bossBars.values().stream()
                .anyMatch(bar -> bar.getName().getString().contains("Kuudra"));

        if (hasKuudraBar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossBar*", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderBossBar(DrawContext context, int x, int y, BossBar bossBar, CallbackInfo ci) {
        BossBarRenderEvent event = EventBus.post(new BossBarRenderEvent(bossBar));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
