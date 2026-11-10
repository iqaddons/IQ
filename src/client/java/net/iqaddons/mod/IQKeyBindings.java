package net.iqaddons.mod;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.*;
import net.iqaddons.mod.features.generic.LoadoutsFeature;
import net.iqaddons.mod.screen.IQConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;

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
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                IQ_CATEGORY
        ));

        advanceCroesusPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.advance-croesus-page",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RIGHT,
                IQ_CATEGORY
        ));

        goBackCroesusPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.go-back-croesus-page",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LEFT,
                IQ_CATEGORY
        ));

        openWardrobeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.open-wardrobe",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                IQ_CATEGORY
        ));

        openLoadoutsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.open-loadouts",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                IQ_CATEGORY
        ));

        wardrobeSlotKeys = List.of(
                registerWardrobeSlotKey(1, InputConstants.KEY_1),
                registerWardrobeSlotKey(2, InputConstants.KEY_2),
                registerWardrobeSlotKey(3, InputConstants.KEY_3),
                registerWardrobeSlotKey(4, InputConstants.KEY_4),
                registerWardrobeSlotKey(5, InputConstants.KEY_5),
                registerWardrobeSlotKey(6, InputConstants.KEY_6),
                registerWardrobeSlotKey(7, InputConstants.KEY_7),
                registerWardrobeSlotKey(8, InputConstants.KEY_8),
                registerWardrobeSlotKey(9, InputConstants.KEY_9)
        );

        loadoutsSlotKeys = List.of(
                registerLoadoutsSlotKey(1, InputConstants.KEY_1),
                registerLoadoutsSlotKey(2, InputConstants.KEY_2),
                registerLoadoutsSlotKey(3, InputConstants.KEY_3),
                registerLoadoutsSlotKey(4, InputConstants.KEY_4),
                registerLoadoutsSlotKey(5, InputConstants.KEY_5),
                registerLoadoutsSlotKey(6, InputConstants.KEY_6),
                registerLoadoutsSlotKey(7, InputConstants.KEY_7),
                registerLoadoutsSlotKey(8, InputConstants.KEY_8),
                registerLoadoutsSlotKey(9, InputConstants.KEY_9),
                registerLoadoutsSlotKey(10, InputConstants.KEY_0),
                registerLoadoutsSlotKey(11, InputConstants.KEY_MINUS),
                registerLoadoutsSlotKey(12, InputConstants.KEY_EQUALS)
        );

        loadoutsNextPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-next-page",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_D,
                IQ_CATEGORY
        ));

        loadoutsPreviousPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-previous-page",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_A,
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
                    drainLoadoutsSlotClicks();
                    LoadoutsFeature.markManualOpen();
                    client.player.connection.sendCommand("loadouts");
                }
            }

            if (client.gui.screen() == null && LoadoutsFeature.isAwaitingManualOpenSlot()) {
                for (int slot = 0; slot < loadoutsSlotKeys.size(); slot++) {
                    while (loadoutsSlotKeys.get(slot).consumeClick()) {
                        LoadoutsFeature.queueLoadoutsSlot(slot + 1);
                    }
                }
            }
        });
    }

    private static void drainLoadoutsSlotClicks() {
        for (KeyMapping loadoutsSlotKey : loadoutsSlotKeys) {
            while (loadoutsSlotKey.consumeClick()) {
                // Drain stale slot clicks before opening Loadouts so queued input only uses fresh presses.
            }
        }
    }

    public static void openConfigScreen(@NotNull Minecraft client) {
        client.gui.setScreen(new IQConfigScreen(null,
                Configuration.class,
                KuudraGeneralConfig.class,
                PhaseOneConfig.class, PhaseTwoConfig.class,
                PhaseThreeConfig.class, PhaseFourConfig.class
        ));
    }

    private static @NotNull KeyMapping registerWardrobeSlotKey(int slotNumber, int defaultKeyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.wardrobe-slot-" + slotNumber,
                InputConstants.Type.KEYBOARD,
                defaultKeyCode,
                IQ_CATEGORY
        ));
    }

    private static @NotNull KeyMapping registerLoadoutsSlotKey(int slotNumber, int defaultKeyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.iq.loadouts-slot-" + slotNumber,
                InputConstants.Type.KEYBOARD,
                defaultKeyCode,
                IQ_CATEGORY
        ));
    }
}
