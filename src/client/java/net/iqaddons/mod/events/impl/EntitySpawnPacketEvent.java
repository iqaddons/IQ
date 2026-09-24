package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record EntitySpawnPacketEvent(
        int entityId,
        @NotNull EntityType<?> entityType,
        @NotNull Vec3 position
) implements Event {
}
