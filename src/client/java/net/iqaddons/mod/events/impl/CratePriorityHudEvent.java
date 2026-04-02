package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import org.jetbrains.annotations.NotNull;

public record CratePriorityHudEvent(
        @NotNull String text,
        int durationTicks
) implements Event {
}

