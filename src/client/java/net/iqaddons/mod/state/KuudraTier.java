package net.iqaddons.mod.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public static KuudraTier fromName(String name) {
        if (name == null) return NONE;
        String lower = name.toLowerCase();
        for (KuudraTier tier : values()) {
            if (lower.contains(tier.displayName.toLowerCase())) {
                return tier;
            }
        }
        return NONE;
    }
}
