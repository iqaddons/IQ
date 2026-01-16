package net.iqaddons.mod.utils;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            if (this == PARTY) {
                player.networkHandler.sendChatCommand("pc [IQ] " + message);
                return;
            }

            player.sendMessage(Text.literal(PREFIX + color + message), false);
        }
    }

    public static void sendFormattedMessage(@NotNull String message) {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            player.sendMessage(Text.literal(PREFIX + message.replace('&', '§')), false);
        }
    }

    public static void showTitle(
            @NotNull String title,
            String subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        if (mc.inGameHud == null) return;

        Text titleText = Text.literal(title.replace('&', '§'));
        Text subtitleText = subtitle != null && !subtitle.isEmpty()
                ? Text.literal(subtitle.replace('&', '§'))
                : Text.empty();

        mc.inGameHud.setTitle(titleText);
        mc.inGameHud.setSubtitle(subtitleText);
        mc.inGameHud.setTitleTicks(fadeIn, stay, fadeOut);
    }
}
