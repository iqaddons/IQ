package net.iqaddons.mod.events.impl.skyblock.supply;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.model.spot.SupplyPosition;

public record SupplyPickupEvent(
        PreSpot spot,
        SupplyPosition position,
        long pickupAt
) implements Event {}
