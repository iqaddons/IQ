package net.iqaddons.mod.manager.calculator;

import net.iqaddons.mod.manager.pricing.ItemPriceManager;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemValueCalculator {

    ItemPriceManager manager = ItemPriceManager.get();

     double calculateValue(@NotNull ItemStack stack, String itemId, int quantity);
}
