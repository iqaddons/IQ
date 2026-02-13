package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.hud.HudManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void iq$renderHudOverHandledScreen(
            DrawContext context,
            int mouseX, int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        HudManager.get().renderOnHandledScreen(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSlotClick(Slot slot, int slotId, int button,
            SlotActionType actionType, CallbackInfo ci
    ) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenClickEvent event = EventBus.post(new ScreenClickEvent(
                screen,
                slot,
                actionType
        ));

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$onKeyPressed(@NotNull KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        ScreenKeyPressEvent event = EventBus.post(
                new ScreenKeyPressEvent((HandledScreen<?>) (Object) this, input.key(), input.scancode(), input.modifiers())
        );

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }
}
