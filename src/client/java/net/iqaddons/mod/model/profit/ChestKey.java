package net.iqaddons.mod.model.profit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChestKey {

    BASIC(160_000, 2),
    HOT(320_000, 4),
    BURNING(600_000, 16),
    FIERY(1_200_000, 40),
    INFERNAL(2_400_000, 80);

    private final long baseCoinsCost;
    private final int materialAmount;

}
