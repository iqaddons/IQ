package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Getter
@RequiredArgsConstructor
public class ScreenClickEvent implements Event, Cancellable {

    private final HandledScreen<?> screen;
    private final Slot slot;
    private final SlotActionType actionType;

    @Setter
    private boolean cancelled;
}
