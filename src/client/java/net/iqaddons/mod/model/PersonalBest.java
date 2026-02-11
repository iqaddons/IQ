package net.iqaddons.mod.model;

import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public record PersonalBest(
        long bestTimeMillis,
        Map<KuudraPhase, Long> phaseSplitsMillis
) {

    @Contract(" -> new")
    public static @NotNull PersonalBest empty() {
        return new PersonalBest(-1L, new EnumMap<>(KuudraPhase.class));
    }
}