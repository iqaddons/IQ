package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;

public record SupplyPickupEvent(
        String originalMessage,
        String playerName,
        int currentSupply,
        double pickupAt
) implements Event {
}
