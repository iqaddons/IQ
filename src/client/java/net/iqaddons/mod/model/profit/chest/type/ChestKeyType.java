package net.iqaddons.mod.model.profit.chest.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public enum ChestKeyType {

    FREE("", 0, 0),
    BASIC("KUUDRA_KEY", 160_000, 2),
    HOT("HOT_KUUDRA_KEY", 320_000, 4),
    BURNING("BURNING_KUUDRA_KEY", 600_000, 16),
    FIERY("FIERY_KUUDRA_KEY", 1_200_000, 40),
    INFERNAL("INFERNAL_KUUDRA_KEY", 2_400_000, 80),
    UNKNOWN("UNKNOWN", 0, 0);

    private final String itemId;
    private final long baseCoinsCost;
    private final int materialAmount;

    @Contract(pure = true)
    public static ChestKeyType parseKeyType(@NotNull String keyTier) {
        String normalized = keyTier.toUpperCase();
        return switch (normalized) {
            case "HOT" -> ChestKeyType.HOT;
            case "BURNING" -> ChestKeyType.BURNING;
            case "FIERY" -> ChestKeyType.FIERY;
            case "INFERNAL" -> ChestKeyType.INFERNAL;
            case "KUUDRA_KEY", "BASIC" -> ChestKeyType.BASIC;
            case "FREE" -> ChestKeyType.FREE;
            default -> ChestKeyType.UNKNOWN;
        };
    }
}
