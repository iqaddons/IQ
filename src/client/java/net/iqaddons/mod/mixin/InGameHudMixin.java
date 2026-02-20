package net.iqaddons.mod.mixin;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Slf4j
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Nullable
    private Text title;

    @Shadow
    @Nullable
    private Text subtitle;

    @Unique
    private String iq$lastTitleMessage = "";
    @Unique
    private String iq$lastSubtitleMessage = "";
    @Unique
    private boolean iq$lastTitleCancelled;

    @Inject(method = "render", at = @At("TAIL"))
    private void iq$onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (client.player == null) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        EventBus.post(new HudRenderEvent(
                context,
                tickCounter.getTickProgress(true),
                width,
                height
        ));
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        boolean hasTitle = title != null && !title.getString().isEmpty();
        boolean hasSubtitle = subtitle != null && !subtitle.getString().isEmpty();
        if (!hasTitle && !hasSubtitle) {
            iq$lastTitleMessage = "";
            iq$lastSubtitleMessage = "";
            iq$lastTitleCancelled = false;
            return;
        }

        String currentTitleMessage = title == null ? "" : title.getString();
        String currentSubtitleMessage = subtitle == null ? "" : subtitle.getString();
        boolean isDuplicateTitle = currentTitleMessage.equals(iq$lastTitleMessage)
                && currentSubtitleMessage.equals(iq$lastSubtitleMessage);

        if (isDuplicateTitle) {
            if (iq$lastTitleCancelled) {
                ci.cancel();
            }

            return;
        }

        iq$lastTitleMessage = currentTitleMessage;
        iq$lastSubtitleMessage = currentSubtitleMessage;

        TitleReceivedEvent event = EventBus.post(
                new TitleReceivedEvent(title, subtitle)
        );

        iq$lastTitleCancelled = event.isCancelled();
        if (iq$lastTitleCancelled) {
            ci.cancel();
        }
    }
}