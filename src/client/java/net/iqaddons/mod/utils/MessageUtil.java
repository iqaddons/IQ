package net.iqaddons.mod.utils;

import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudNotificationEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public enum MessageUtil {

    SUCCESS("§a"),
    INFO("§7"),
    WARNING("§e"),
    ERROR("§c"),
    PARTY();

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String PREFIX = "§d§l[IQ] §r";

    private final String color;

    MessageUtil() {
        this.color = "";
    }

    public void sendMessage(String message) {
        mc.execute(() -> {
            ClientPlayerEntity player = mc.player;
            if (player != null) {
                if (this == PARTY) {
                    player.networkHandler.sendChatCommand("pc " + message);
                    return;
                }

                player.sendMessage(Text.literal(PREFIX + color + message), false);
            }
        });
    }

    public static void sendFormattedMessage(@NotNull String message) {
        mc.execute(() -> {
            ClientPlayerEntity player = mc.player;
            if (player != null) {
                player.sendMessage(Text.literal(PREFIX + message.replace('&', '§')), false);
            }
        });
    }

    public static void showTitle(
            @NotNull String title,
            String subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        showTitle(
                Text.literal(title.replace('&', '§')),
                subtitle != null && !subtitle.isEmpty()
                        ? Text.literal(subtitle.replace('&', '§'))
                        : Text.empty(),
                fadeIn, stay, fadeOut
        );
    }

    public static void showTitle(Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        mc.execute(() -> {
            if (mc.inGameHud == null) return;

            mc.inGameHud.setTitle(title);
            mc.inGameHud.setSubtitle(subtitle);
            mc.inGameHud.setTitleTicks(fadeIn, stay, fadeOut);
        });
    }

    public static void showAlert(String message, int durationTicks) {
        mc.execute(() -> EventBus.post(new HudNotificationEvent(message, durationTicks)));
    }

    public static void showAlert(String message, int durationTicks, SoundEvent soundEvent) {
        mc.execute(() -> EventBus.post(new HudNotificationEvent(message, durationTicks, soundEvent)));
    }
}
