package net.iqaddons.mod.utils;

import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudNotificationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public enum MessageUtil {

    SUCCESS("§a"),
    INFO("§f"),
    WARNING("§e"),
    ERROR("§c"),
    PARTY();

    private static final Minecraft mc = Minecraft.getInstance();
    private static final String PREFIX = "§d§l[IQ] §r";

    private final String color;

    MessageUtil() {
        this.color = "";
    }

    public void sendMessage(String message) {
        mc.execute(() -> {
            LocalPlayer player = mc.player;
            if (player != null) {
                if (this == PARTY) {
                    player.connection.sendCommand("pc " + message);
                    return;
                }

                player.sendSystemMessage(Component.literal(PREFIX + color + message));
            }
        });
    }

    public static void sendFormattedMessage(@NotNull String message) {
        mc.execute(() -> {
            LocalPlayer player = mc.player;
            if (player != null) {
                player.sendSystemMessage(Component.literal(PREFIX + message.replace('&', '§')));
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
                Component.literal(title.replace('&', '§')),
                subtitle != null && !subtitle.isEmpty()
                        ? Component.literal(subtitle.replace('&', '§'))
                        : Component.empty(),
                fadeIn, stay, fadeOut
        );
    }

    public static void showTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        mc.execute(() -> {
            if (mc.gui == null) return;

            mc.gui.setTitle(title);
            mc.gui.setSubtitle(subtitle);
            mc.gui.setTimes(fadeIn, stay, fadeOut);
        });
    }

    public static void showAlert(String message, int durationTicks) {
        mc.execute(() -> EventBus.post(new HudNotificationEvent(message, durationTicks)));
    }

    public static void showAlert(String message, int durationTicks, SoundEvent soundEvent) {
        mc.execute(() -> EventBus.post(new HudNotificationEvent(message, durationTicks, soundEvent)));
    }
}
