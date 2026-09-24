package net.iqaddons.mod.manager.calculator.impl;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EssenceValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        double totalBonus = (KuudraGeneralConfig.ProfitTrackerConfig.kuudraPetBonus
                + KuudraGeneralConfig.ProfitTrackerConfig.attributeBonus) / 100.0;
        int finalAmount = (int) Math.round(quantity * (1 + totalBonus));
        return manager.getItemPrice(itemId) * finalAmount;
    }
}
