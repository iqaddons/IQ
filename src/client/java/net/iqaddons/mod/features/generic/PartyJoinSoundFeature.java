package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@Slf4j
public class PartyJoinSoundFeature extends Feature {

    private static final Minecraft mc = Minecraft.getInstance();
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
        subscribe(ChatReceivedEvent.class, this::onChat);
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        if (isPartyJoinMessage(message)) {
            if (mc.player == null || mc.level == null) {
                return;
            }

            mc.level.playSound(
                    mc.player, mc.player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 2.0f, 1.0f
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