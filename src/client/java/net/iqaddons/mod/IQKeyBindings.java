package net.iqaddons.mod;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.*;
import net.iqaddons.mod.screen.IQConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IQKeyBindings {

    private static final KeyMapping.Category IQ_CATEGORY = KeyMapping.Category.register(Identifier.parse("iq"));

    private static KeyMapping openConfigKey;
    private static KeyMapping openWardrobeKey;
    private static KeyMapping openLoadoutsKey;

    @Getter
    private static KeyMapping advanceCroesusPageKey;

    @Getter
    private static KeyMapping goBackCroesusPageKey;

    @Getter
    private static KeyMapping castIchorPoolKey;

    @Getter
    private static KeyMapping pearlAimAssistKey;

    @Getter
    private static KeyMapping toggleLeftClickKey;

    @Getter
    private static KeyMapping toggleRightClickKey;

    @Getter
    private static List<KeyMapping> wardrobeSlotKeys = List.of();

    @Getter
    private static List<KeyMapping> loadoutsSlotKeys = List.of();

    @Getter
    private static KeyMapping loadoutsNextPageKey;

    @Getter
    private static KeyMapping loadoutsPreviousPageKey;

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

        openLoadoutsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.open-loadouts",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                IQ_CATEGORY
        ));

        castIchorPoolKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.cast-ichor-pool",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                IQ_CATEGORY
        ));

        pearlAimAssistKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.pearl-aim-assist",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                IQ_CATEGORY
        ));

        toggleLeftClickKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.toggle-left-click",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                IQ_CATEGORY
        ));

        toggleRightClickKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.toggle-right-click",
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

        loadoutsSlotKeys = List.of(
                registerLoadoutsSlotKey(1, GLFW.GLFW_KEY_1),
                registerLoadoutsSlotKey(2, GLFW.GLFW_KEY_2),
                registerLoadoutsSlotKey(3, GLFW.GLFW_KEY_3),
                registerLoadoutsSlotKey(4, GLFW.GLFW_KEY_4),
                registerLoadoutsSlotKey(5, GLFW.GLFW_KEY_5),
                registerLoadoutsSlotKey(6, GLFW.GLFW_KEY_6),
                registerLoadoutsSlotKey(7, GLFW.GLFW_KEY_7),
                registerLoadoutsSlotKey(8, GLFW.GLFW_KEY_8),
                registerLoadoutsSlotKey(9, GLFW.GLFW_KEY_9),
                registerLoadoutsSlotKey(10, GLFW.GLFW_KEY_0),
                registerLoadoutsSlotKey(11, GLFW.GLFW_KEY_MINUS),
                registerLoadoutsSlotKey(12, GLFW.GLFW_KEY_EQUAL)
        );

        loadoutsNextPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-next-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                IQ_CATEGORY
        ));

        loadoutsPreviousPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-previous-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                IQ_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                openConfigScreen(client);
            }

            while (openWardrobeKey.consumeClick()) {
                if (client.player != null) {
                    client.player.connection.sendCommand("wd");
                }
            }

            while (openLoadoutsKey.consumeClick()) {
                if (client.player != null) {
                    client.player.connection.sendCommand("loadouts");
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

    private static @NotNull KeyMapping registerLoadoutsSlotKey(int slotNumber, int defaultKeyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-slot-" + slotNumber,
                InputConstants.Type.KEYSYM,
                defaultKeyCode,
                IQ_CATEGORY
        ));
    }
}
