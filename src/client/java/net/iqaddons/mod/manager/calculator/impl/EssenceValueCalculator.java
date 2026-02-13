package net.iqaddons.mod.manager.calculator.impl;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EssenceValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        int finalAmount = (int) Math.round(quantity * (1 + KuudraGeneralConfig.kuudraPetBonus / 100.0));
        return manager.getItemPrice(itemId) * finalAmount;
    }
}
