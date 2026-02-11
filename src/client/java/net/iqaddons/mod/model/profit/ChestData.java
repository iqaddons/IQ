package net.iqaddons.mod.model.profit;

public record ChestData(
        ChestType type,
        long grossValue, long keyCost,
        long netProfit, int essence,
        int teeth, int pricedItems
) {
}
