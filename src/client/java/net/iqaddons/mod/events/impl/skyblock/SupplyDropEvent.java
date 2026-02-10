package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import org.jetbrains.annotations.NotNull;

public record SupplyDropEvent(
        @NotNull String playerName,
        int currentSupply
) implements Event {}