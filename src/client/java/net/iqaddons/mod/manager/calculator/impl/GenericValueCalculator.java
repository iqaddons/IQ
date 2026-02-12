package net.iqaddons.mod.manager.calculator.impl;

import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GenericValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        int count = Math.max(1, quantity);
        return manager.getPrice(itemId).orElse(0D) * count;
    }
}
