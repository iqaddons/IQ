package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public record ScreenSetEvent(
        @Nullable Screen previousScreen,
        @Nullable Screen currentScreen
) implements Event {
}
