package net.iqaddons.mod.model.profit.chest.data;

import net.iqaddons.mod.model.profit.chest.type.ChestKeyType;
import net.minecraft.item.ItemStack;

import java.util.List;

public record ChestContents(
        List<ItemStack> items,
        ChestKeyType keyType
) {
}
