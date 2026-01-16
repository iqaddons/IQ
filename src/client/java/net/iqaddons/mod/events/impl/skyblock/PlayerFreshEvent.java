package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;

public record PlayerFreshEvent(
        boolean selfFresh,
        String playerName,
        int playerEntityId,
        int buildingProgress,
        long freshAt
) implements Event {
}
