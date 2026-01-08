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

public class IQKeyBindings {

    private static KeyBinding openConfigKey;

    public static void register() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iq.openConfig",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(Identifier.of("category.iq.keybinds"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }
        });
    }

    public static void openConfigScreen(@NotNull MinecraftClient client) {
        if (client.currentScreen == null) {
            client.setScreen(
                    ResourcefulConfigScreen.make(IQModClient.get().getConfigurator(), Configuration.class)
                            .withParent(null)
                            .build()
            );
        }
    }
}
