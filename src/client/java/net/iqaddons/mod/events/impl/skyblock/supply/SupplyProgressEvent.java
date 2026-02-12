package net.iqaddons.mod.events.impl.skyblock.supply;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.model.spot.SupplyPosition;

@Data
@RequiredArgsConstructor
public class SupplyProgressEvent implements Event, Cancellable {

    private final SupplyPosition position;
    private final PreSpot spot;
    private final String progressText;
    private final int currentProgress;

    private boolean cancelled;
}
