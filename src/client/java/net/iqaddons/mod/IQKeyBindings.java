package net.iqaddons.mod;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.*;
import net.iqaddons.mod.screen.IQConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IQKeyBindings {

    private static final KeyMapping.Category IQ_CATEGORY = KeyMapping.Category.register(Identifier.parse("iq"));

    private static KeyMapping openConfigKey;
    private static KeyMapping openWardrobeKey;

    @Getter
    private static KeyMapping advanceCroesusPageKey;

    @Getter
    private static KeyMapping goBackCroesusPageKey;

    @Getter
    private static List<KeyMapping> wardrobeSlotKeys = List.of();

    public static void register() {
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.open-config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                IQ_CATEGORY
        ));

        advanceCroesusPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.advance-croesus-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                IQ_CATEGORY
        ));

        goBackCroesusPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.go-back-croesus-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                IQ_CATEGORY
        ));

        openWardrobeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.open-wardrobe",
                InputConstants.Type.KEYSYM,
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
            while (openConfigKey.consumeClick()) {
                openConfigScreen(client);
            }

            while (openWardrobeKey.consumeClick()) {
                if (client.player != null && client.player.connection != null) {
                    client.player.connection.sendCommand("wd");
                }
            }
        });
    }

    public static void openConfigScreen(@NotNull Minecraft client) {
        client.setScreen(new IQConfigScreen(null,
                Configuration.class,
                KuudraGeneralConfig.class,
                PhaseOneConfig.class, PhaseTwoConfig.class,
                PhaseThreeConfig.class, PhaseFourConfig.class
        ));
    }

    private static @NotNull KeyMapping registerWardrobeSlotKey(int slotNumber, int defaultKeyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.wardrobe-slot-" + slotNumber,
                InputConstants.Type.KEYSYM,
                defaultKeyCode,
                IQ_CATEGORY
        ));
    }
}
