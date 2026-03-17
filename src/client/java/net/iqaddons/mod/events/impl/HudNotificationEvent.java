package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record HudNotificationEvent(
        @NotNull String text,
        int durationTicks,
        @Nullable SoundEvent soundEvent
) implements Event {
    public HudNotificationEvent(@NotNull String text, int durationTicks) {
        this(text, durationTicks, null);
    }
}
