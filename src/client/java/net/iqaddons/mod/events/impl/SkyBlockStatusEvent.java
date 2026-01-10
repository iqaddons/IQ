package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import org.jetbrains.annotations.NotNull;

public record SkyBlockStatusEvent(
        boolean onSkyBlock,
        @NotNull String currentArea
) implements Event {
}
