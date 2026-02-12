package net.iqaddons.mod.manager.calculator.impl;

import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SalvageValueCalculator implements ItemValueCalculator {

    private static final int BASE_CRIMSON_ESSENCE = 108;
    private static final double KUUDRA_STAR_MULTIPLIER = 0.63;

    @Override
    public double calculateValue(@NotNull ItemStack stack, String itemId, int quantity) {
        int stars = countStars(stack.getName().getString());
        int totalStarCost = 0;
        for (int star = 1; star <= stars; star++) {
            totalStarCost += 20 + (star * 5);
        }

        int bonus = (int) Math.floor(totalStarCost * KUUDRA_STAR_MULTIPLIER);
        int salvageValue = BASE_CRIMSON_ESSENCE + bonus;

        return manager
                .getPrice(itemId)
                .orElse(0D) * salvageValue;
    }

    private int countStars(@NotNull String name) {
        return (int) name.chars().filter(ch -> ch == '✪').count();
    }
}

