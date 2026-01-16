package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;

public record SupplyPickupEvent(
        String originalMessage,
        String playerName,
        int currentSupply,
        long pickupAt
) implements Event {
}
