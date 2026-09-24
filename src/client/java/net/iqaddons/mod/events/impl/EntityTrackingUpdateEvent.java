package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public record EntityTrackingUpdateEvent(
        @NotNull Entity entity
) implements Event {
}
