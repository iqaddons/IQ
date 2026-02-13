package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

public record ScreenDrawSlotEvent(
        HandledScreen<?> screen,
        DrawContext drawContext,
        Slot slot,
        int x,
        int y
) implements Event {
}
