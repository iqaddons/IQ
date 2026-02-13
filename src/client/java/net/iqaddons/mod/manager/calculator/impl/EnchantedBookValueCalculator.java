package net.iqaddons.mod.manager.calculator.impl;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class EnchantedBookValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        if (!"ENCHANTED_BOOK".equals(itemId)) {
            return manager.getItemPrice(itemId) * quantity;
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

            var enchantId = String.format("ENCHANTMENT_%s_%S", enchant.toUpperCase(), level.get());
            return manager.getItemPrice(enchantId) * quantity;
        }

        return 0D;
    }
}