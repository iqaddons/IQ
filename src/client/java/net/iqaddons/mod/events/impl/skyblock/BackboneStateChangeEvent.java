package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;

public record BackboneStateChangeEvent(
        int ticksRemaining,
        int startingTicks,
        boolean rendActive,
        boolean rendTriggered
) implements Event {
}
