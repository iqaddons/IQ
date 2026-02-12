package net.iqaddons.mod.events.impl.skyblock.supply;

import net.iqaddons.mod.events.Event;

public record SupplyPlaceEvent(
        String originalMessage,
        String playerName,
        int currentSupply,
        double placedAt
) implements Event {}
