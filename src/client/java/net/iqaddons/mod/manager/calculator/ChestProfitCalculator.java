package net.iqaddons.mod.manager.calculator;

import net.iqaddons.mod.manager.ItemPriceManager;
import net.iqaddons.mod.manager.calculator.impl.GenericValueCalculator;
import net.iqaddons.mod.model.profit.chest.data.ChestContents;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class ChestProfitCalculator {

    private final ItemPriceManager priceManager = ItemPriceManager.get();

    private final GenericValueCalculator defaultCalculator;
    private final Map<String, ItemValueCalculator> calculators;
    private final Function<ItemStack, String> itemIdResolver;

    public ChestProfitCalculator(
            GenericValueCalculator defaultCalculator,
            Map<String, ItemValueCalculator> calculators,
            Function<ItemStack, String> itemIdResolver
    ) {
        this.defaultCalculator = defaultCalculator;
        this.calculators = calculators;
        this.itemIdResolver = itemIdResolver;
    }

    public double calculateProfit(@NotNull ChestContents contents) {
        double totalValue = contents.items().stream()
                .mapToDouble(this::calculateItemValue)
                .sum();

        double keyCost = priceManager.calculateKeyPrice(contents.keyType());
        return totalValue - keyCost;
    }

    public double calculateTotalValue(@NotNull ChestContents contents) {
        return contents.items().stream().mapToDouble(this::calculateItemValue).sum();
    }

    public double calculateItemValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0D;
        }

        String itemId = itemIdResolver.apply(stack);
        if (itemId == null || itemId.isBlank()) {
            return 0D;
        }

        int quantity = ChestProfitUtil.resolveItemQuantity(stack);
        if (quantity <= 0) quantity = stack.getCount();

        ItemValueCalculator calculator = calculators.getOrDefault(itemId, defaultCalculator);
        return calculator.calculateValue(stack, itemId, quantity);
    }
}