package net.iqaddons.mod.model.profit.chest.data;

import net.iqaddons.mod.model.profit.chest.type.ChestType;

public record ChestData(
        ChestType type,
        long grossValue, long keyCost,
        long netProfit, int essence,
        int pricedItems
) {
}
