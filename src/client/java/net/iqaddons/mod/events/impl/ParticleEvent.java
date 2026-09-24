package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.core.particles.ParticleOptions;

@Getter
@Setter
@RequiredArgsConstructor
public class ParticleEvent implements Event, Cancellable {

    private final ParticleOptions type;
    private final double x;
    private final double y;
    private final double z;
    private final double xSpeed;
    private final double ySpeed;
    private final double zSpeed;

    private boolean cancelled;
}
