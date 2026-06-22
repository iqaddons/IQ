package net.iqaddons.mod.model.profit.chest;

import net.minecraft.network.chat.Component;

public record ChestItemValue(
        Component displayName,
        int count,
        double value
) {
}