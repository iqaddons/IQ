package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.iqaddons.mod.config.Configuration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IQKeyBindings {

    private static final KeyBinding.Category IQ_CATEGORY = KeyBinding.Category.create(Identifier.of("iq"));

    private static KeyBinding openConfigKey;
    private static KeyBinding openWardrobeKey;
    private static List<KeyBinding> wardrobeSlotKeys = List.of();

    public static void register() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iq.open-config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                IQ_CATEGORY
        ));

        openWardrobeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iq.open-wardrobe",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                IQ_CATEGORY
        ));

        wardrobeSlotKeys = List.of(
                registerWardrobeSlotKey(1, GLFW.GLFW_KEY_1),
                registerWardrobeSlotKey(2, GLFW.GLFW_KEY_2),
                registerWardrobeSlotKey(3, GLFW.GLFW_KEY_3),
                registerWardrobeSlotKey(4, GLFW.GLFW_KEY_4),
                registerWardrobeSlotKey(5, GLFW.GLFW_KEY_5),
                registerWardrobeSlotKey(6, GLFW.GLFW_KEY_6),
                registerWardrobeSlotKey(7, GLFW.GLFW_KEY_7),
                registerWardrobeSlotKey(8, GLFW.GLFW_KEY_8),
                registerWardrobeSlotKey(9, GLFW.GLFW_KEY_9)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }

            while (openWardrobeKey.wasPressed()) {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand("wd");
                }
            }
        });
    }

    public static void openConfigScreen(@NotNull MinecraftClient client) {
        client.setScreen(
                ResourcefulConfigScreen.make(IQModClient.get().getConfigurator(), Configuration.class)
                        .withParent(null)
                        .build()
        );
    }

    public static @NotNull List<KeyBinding> getWardrobeSlotKeys() {
        return wardrobeSlotKeys;
    }

    private static @NotNull KeyBinding registerWardrobeSlotKey(int slotNumber, int defaultKeyCode) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iq.wardrobe-slot-" + slotNumber,
                InputUtil.Type.KEYSYM,
                defaultKeyCode,
                IQ_CATEGORY
        ));
    }
}
