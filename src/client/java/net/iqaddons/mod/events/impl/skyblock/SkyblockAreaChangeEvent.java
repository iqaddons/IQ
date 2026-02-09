package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import org.jetbrains.annotations.NotNull;

public record SkyblockAreaChangeEvent(
        boolean onSkyBlock,
        String previousArea,
        @NotNull String newArea
) implements Event {
}
