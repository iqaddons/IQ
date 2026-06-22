package net.iqaddons.mod.mixin;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.BossBarRenderEvent;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Final
    @Shadow
    Map<UUID, LerpingBossEvent> events;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderAll(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!KuudraGeneralConfig.hideKuudraBossBar) {
            return;
        }

        if (!KuudraStateManager.get().isInRun()) {
            return;
        }

        boolean hasKuudraBar = events.values().stream()
                .anyMatch(bar -> bar.getName().getString().contains("Kuudra"));

        if (hasKuudraBar) {
            ci.cancel();
        }
    }

    @Inject(method = "extractBar*", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderBossBar(GuiGraphicsExtractor context, int x, int y, BossEvent bossBar, CallbackInfo ci) {
        BossBarRenderEvent event = EventBus.post(new BossBarRenderEvent(bossBar));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
