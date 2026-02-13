package net.iqaddons.mod.manager.calculator.impl;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Slf4j
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

        if (KuudraGeneralConfig.armorValueType == KuudraGeneralConfig.ArmorValueType.SALVAGE) {
            return (manager.getItemPrice("ESSENCE_CRIMSON") * salvageValue) * quantity;
        } else {
            return manager.getItemPrice(itemId) * quantity;
        }
    }

    private int countStars(@NotNull String name) {
        return (int) name.chars().filter(ch -> ch == '✪').count();
    }
}

