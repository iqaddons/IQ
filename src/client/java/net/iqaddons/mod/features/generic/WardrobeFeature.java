package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class WardrobeFeature extends Feature {

    private static final Minecraft mc = Minecraft.getInstance();

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
        if (!title.contains(WARDROBE_TITLE)) return;

        int slotIndex = keyCodeToWardrobeSlot(event.getKeyCode(), event.getScanCode());
        if (slotIndex < 0) return;

        event.setCancelled(true);
        clickWardrobeSlot(slotIndex);

        if (Configuration.wardrobeSound) {
            mc.level.playSound(
                    mc.player, mc.player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 2.0f, 1.0f
            );
        }

        log.debug("Wardrobe slot {} selected (container index {})", slotIndex - WARDROBE_SLOT_OFFSET + 1, slotIndex);
    }

    private int keyCodeToWardrobeSlot(int keyCode, int scanCode) {
        List<KeyMapping> wardrobeSlotKeys = IQKeyBindings.getWardrobeSlotKeys();
        for (int slot = 0; slot < wardrobeSlotKeys.size(); slot++) {
            if (wardrobeSlotKeys.get(slot).matches(new KeyEvent(keyCode, scanCode, 0))) {
                return WARDROBE_SLOT_OFFSET + slot;
            }
        }

        return -1;
    }

    private void clickWardrobeSlot(int slotIndex) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> handledScreen)) return;

        AbstractContainerMenu handler = handledScreen.getMenu();
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) return;

        mc.gameMode.handleContainerInput(
                handler.containerId,
                slotIndex,
                0,
                ContainerInput.PICKUP,
                player
        );
    }
}