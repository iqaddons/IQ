package net.iqaddons.mod.features.generic;

import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.MessageUtil;

public class LimboAlertFeature extends Feature {

    private static final String LIMBO_MESSAGE = "A kick occurred in your connection, so you were put in the SkyBlock lobby!";

    public LimboAlertFeature() {
        super("limboAlert", "Limbo Alert",
                () -> Configuration.limboAlert
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onChatReceived(ChatReceivedEvent event) {
        if (!isEnabled()) return;

        String message = event.getStrippedMessage();
        if (message.contains(LIMBO_MESSAGE)) {
            MessageUtil.PARTY.sendMessage("[IQ] I was kicked from the game!");

            mc.player.playSound(
                    net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                    2.0f, 1.0f
            );
        }
    }
}
