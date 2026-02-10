package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

@Slf4j
public class WardrobeFeature extends Feature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String WARDROBE_TITLE = "Wardrobe";
    private static final int WARDROBE_SLOT_OFFSET = 36;

    public WardrobeFeature() {
        super(
                "wardrobeKeybinds",
                "Wardrobe Keybinds",
                () -> Configuration.wardrobeKeybinds
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ScreenKeyPressEvent.class, this::onScreenKeyPress);
    }

    private void onScreenKeyPress(@NotNull ScreenKeyPressEvent event) {
        String title = event.getScreenTitle();
        if (!title.contains(WARDROBE_TITLE) || title.equals("container")) return;

        int slotIndex = keyCodeToWardrobeSlot(event.getKeyCode());
        if (slotIndex < 0) return;

        event.setCancelled(true);
        clickWardrobeSlot(slotIndex);

        if (Configuration.wardrobeSound) {
            mc.world.playSound(
                    mc.player, mc.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    SoundCategory.PLAYERS, 2.0f, 1.0f
            );
        }

        log.debug("Wardrobe slot {} selected (container index {})", slotIndex - WARDROBE_SLOT_OFFSET + 1, slotIndex);
    }

    private int keyCodeToWardrobeSlot(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int slot = keyCode - GLFW.GLFW_KEY_1;
            return WARDROBE_SLOT_OFFSET + slot;
        }

        if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            int slot = keyCode - GLFW.GLFW_KEY_KP_1;
            return WARDROBE_SLOT_OFFSET + slot;
        }

        return -1;
    }

    private void clickWardrobeSlot(int slotIndex) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!(mc.currentScreen instanceof HandledScreen<?> handledScreen)) return;

        ScreenHandler handler = handledScreen.getScreenHandler();
        mc.interactionManager.clickSlot(
                handler.syncId,
                slotIndex,
                0,
                SlotActionType.PICKUP,
                player
        );
    }
}