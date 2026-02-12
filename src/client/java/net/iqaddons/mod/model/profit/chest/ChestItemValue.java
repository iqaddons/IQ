package net.iqaddons.mod.model.profit.chest;

import net.minecraft.text.Text;

public record ChestItemValue(
        Text displayName,
        int count,
        double value
) {
}