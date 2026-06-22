package net.iqaddons.mod.mixin;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Slf4j
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private Component title;

    @Shadow
    @Nullable
    private Component subtitle;

    @Unique
    private String iq$lastTitleMessage = "";
    @Unique
    private String iq$lastSubtitleMessage = "";
    @Unique
    private boolean iq$lastTitleCancelled;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void iq$onRenderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (minecraft.player == null) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        EventBus.post(new HudRenderEvent(
                context,
                tickCounter.getGameTimeDeltaPartialTick(true),
                width,
                height
        ));
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void iq$onRenderTitleAndSubtitle(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
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