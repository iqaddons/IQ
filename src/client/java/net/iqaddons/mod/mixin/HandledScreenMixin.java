package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

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
}
