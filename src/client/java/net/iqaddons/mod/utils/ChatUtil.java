package net.iqaddons.mod.utils;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public enum ChatUtil {

    SUCCESS("§a"),
    INFO("§7"),
    WARNING("§e"),
    ERROR("§c"),
    PARTY();

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String PREFIX = "§d[IQ] §r";

    private final String color;

    ChatUtil() {
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
            player.sendMessage(Text.literal(message.replace('&', '§')), false);
        }
    }
}
