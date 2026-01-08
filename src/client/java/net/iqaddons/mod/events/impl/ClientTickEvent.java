package net.iqaddons.mod.events.impl;

import lombok.Getter;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.MinecraftClient;

@Getter
public class ClientTickEvent extends Event {

    private static long tickCount = 0;

    private final MinecraftClient client;

    public ClientTickEvent(MinecraftClient client) {
        this.client = client;
        tickCount++;
    }

    public boolean isNthTick(int n) {
        return tickCount % n == 0;
    }

    public boolean isInGame() {
        return client.player != null && client.world != null;
    }
}
