package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.model.profit.ChestType;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record KuudraChestOpenEvent(
        int windowId,
        @NotNull String title,
        List<Slot> slots,
        ChestType chestType
) implements Event {
}
