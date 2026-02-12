package net.iqaddons.mod.manager.calculator.impl;

import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnchantedBookValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        if (!"ENCHANTED_BOOK".equals(itemId)) {
            return manager.getPrice(itemId).orElse(0D) * quantity;
        }

        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return 0D;

        var tag = customData.copyNbt();
        if (!tag.contains("enchantments")) return 0D;

        var enchants = tag.getCompound("enchantments");
        if (enchants.isEmpty()) return 0D;

        for (String enchant : enchants.get().getKeys()) {
            var level = enchants.get().getInt(enchant);
            if (level.isEmpty()) return 0D;

            String enchantmentId = enchant.toUpperCase() + "_" + level.get();
            return manager.getPrice(enchantmentId)
                    .orElse(0D) * quantity;
        }

        return 0D;
    }
}