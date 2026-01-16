package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@Slf4j
public class PartyJoinSoundFeature extends Feature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern PARTY_JOIN_PATTERN = Pattern.compile("^\\w+ joined the party[.!]?$");

    public PartyJoinSoundFeature() {
        super(
                "partyJoinSound",
                "Party Join Sound",
                () -> Configuration.partyJoinSound
        );
    }

    @Override
    protected void onActivate() {
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        if (isPartyJoinMessage(message)) {
            if (mc.player == null || mc.world == null) {
                return;
            }

            mc.world.playSound(
                    mc.player, mc.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    SoundCategory.PLAYERS, 2.0f, 1.0f
            );
        }
    }

    private boolean isPartyJoinMessage(@NotNull String message) {
        if (message.contains("joined the party")) {
            return true;
        }

        return PARTY_JOIN_PATTERN.matcher(message).matches();
    }
}