package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.atomic.AtomicLong;

public record ClientTickEvent(
        MinecraftClient client,
        long tickCount
) implements Event {

    private static final AtomicLong TICK_COUNTER = new AtomicLong(0);

    public static ClientTickEvent create(MinecraftClient client) {
        return new ClientTickEvent(client, TICK_COUNTER.incrementAndGet());
    }

    public boolean isNthTick(int n) {
        return n > 0 &&  tickCount % n == 0;
    }

    public boolean isInGame() {
        return client.player != null && client.world != null;
    }
}
