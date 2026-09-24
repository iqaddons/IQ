package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

public record SoundReceivedEvent(
        ClientboundSoundPacket packet
) implements Event {
}
