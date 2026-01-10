package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

@Getter
@RequiredArgsConstructor
public class ScreenClickEvent implements Event, Cancellable {

    private final HandledScreen<?> screen;
    private final DrawContext context;
    private final Slot slot;

    @Setter
    private boolean cancelled;
}
