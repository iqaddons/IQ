package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class ScreenKeyPressEvent implements Event, Cancellable {

    private final @NotNull Screen screen;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;

    @Setter
    private boolean cancelled;

    public @NotNull String getScreenTitle() {
        return screen.getTitle().getString();
    }
}