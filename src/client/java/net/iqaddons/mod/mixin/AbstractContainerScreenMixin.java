package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.events.impl.ScreenDrawSlotEvent;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.hud.HudManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void iq$renderHudOverHandledScreen(
            GuiGraphicsExtractor context,
            int mouseX, int mouseY,
            float a,
            CallbackInfo ci
    ) {
        HudManager.get().renderOnHandledScreen(context, mouseX, mouseY, a);
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void iq$highlightOpenedCroesusChests(GuiGraphicsExtractor context, Slot slot, int x, int y, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        EventBus.post(new ScreenDrawSlotEvent(
                screen,
                context,
                slot,
                x, y
        ));
    }

    @Inject(
            method = "slotClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSlotClick(Slot slot, int slotId, int button,
                             ContainerInput actionType, CallbackInfo ci
    ) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ScreenClickEvent event = EventBus.post(new ScreenClickEvent(
                screen,
                slot,
                button,
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
    private void iq$onKeyPressed(@NotNull KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        ScreenKeyPressEvent event = EventBus.post(
                new ScreenKeyPressEvent((AbstractContainerScreen<?>) (Object) this, input.key(), input.scancode(), input.modifiers())
        );

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }
}
