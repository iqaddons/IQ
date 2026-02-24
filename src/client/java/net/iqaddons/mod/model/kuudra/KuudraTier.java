package net.iqaddons.mod.model.kuudra;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum KuudraTier {

    UNKNOWN(0, "Unknown"),
    BASIC(1, "Basic"),
    HOT(2, "Hot"),
    BURNING(3, "Burning"),
    FIERY(4, "Fiery"),
    INFERNAL(5, "Infernal");

    private final int level;
    private final String displayName;

    @Contract(pure = true)
    public @NotNull String getAssetCode() {
        return displayName.toLowerCase(Locale.ROOT);
    }

    public static @NotNull Optional<KuudraTier> fromLevel(int level) {
        for (KuudraTier tier : values()) {
            if (tier.level == level) {
                return Optional.of(tier);
            }
        }

        return Optional.empty();
    }
}
