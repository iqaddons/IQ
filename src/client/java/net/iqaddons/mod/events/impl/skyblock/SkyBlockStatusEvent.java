package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import org.jetbrains.annotations.NotNull;

public record SkyBlockStatusEvent(
        boolean onSkyBlock,
        @NotNull String currentArea
) implements Event {
}
