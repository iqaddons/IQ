package net.iqaddons.mod.model.profit.chest;

import java.util.List;

public record ChestValueBreakdown(
        double totalValue,
        double keyCost,
        double profit,
        List<ChestItemValue> items
) {
}