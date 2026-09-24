package net.iqaddons.mod.events.impl;

import lombok.Getter;
import net.iqaddons.mod.events.Event;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

@Getter
public class MobEffectReceivedEvent implements Event {

    private final int entityId;
    private final Holder<MobEffect> effect;
    private final int durationTicks;

    public MobEffectReceivedEvent(int entityId, @NotNull Holder<MobEffect> effect, int durationTicks) {
        this.entityId = entityId;
        this.effect = effect;
        this.durationTicks = durationTicks;
    }
}
