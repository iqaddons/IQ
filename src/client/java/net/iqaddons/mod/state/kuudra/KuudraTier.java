package net.iqaddons.mod.state.kuudra;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum KuudraTier {

    NONE("None", 0),
    BASIC("Basic", 1),
    HOT("Hot", 2),
    BURNING("Burning", 3),
    FIERY("Fiery", 4),
    INFERNAL("Infernal", 5);

    private final String displayName;
    private final int level;

    public static @Nullable KuudraTier fromText(@NotNull String text) {
        String lower = text.toLowerCase();
        for (int i = values().length - 1; i >= 1; i--) {
            KuudraTier tier = values()[i];
            if (lower.contains(tier.displayName.toLowerCase())) {
                return tier;
            }
        }

        return null;
    }
}
