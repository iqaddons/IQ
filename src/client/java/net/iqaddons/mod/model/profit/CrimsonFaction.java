package net.iqaddons.mod.model.profit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrimsonFaction {
    BARBARIAN("ENCHANTED_RED_SAND"),
    MAGE("ENCHANTED_MYCELIUM");

    private final String materialId;
}