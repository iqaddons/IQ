package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.DrawContext;

public record HudRenderEvent(
        DrawContext drawContext,
        float tickDelta,
        int screenWidth,
        int screenHeight
) implements Event {

    public int centerX() {
        return screenWidth / 2;
    }

    public int centerY() {
        return screenHeight / 2;
    }
}