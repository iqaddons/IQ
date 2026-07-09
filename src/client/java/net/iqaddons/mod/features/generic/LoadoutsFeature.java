package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class LoadoutsFeature extends Feature {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final String LOADOUTS_TITLE = "Loadouts";
    private static final String LOADOUTS_PAGE_ONE_TITLE = "(1/3) Loadouts";
    private static final String LOADOUTS_PAGE_TWO_TITLE = "(2/3) Loadouts";
    private static final String LOADOUTS_PAGE_THREE_TITLE = "(3/3) Loadouts";
    private static final int LOADOUTS_NEXT_PAGE_SLOT = 44;
    private static final int LOADOUTS_PREVIOUS_PAGE_SLOT = 17;
    private static final int[] LOADOUTS_SLOT_INDICES = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    public LoadoutsFeature() {
        super(
                "loadoutsKeybinds",
                "Loadouts Keybinds",
                () -> Configuration.loadoutsKeybinds
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ScreenKeyPressEvent.class, this::onScreenKeyPress);
    }

    private void onScreenKeyPress(@NotNull ScreenKeyPressEvent event) {
        String title = event.getScreenTitle();
        if (!title.contains(LOADOUTS_TITLE)) return;

        int slotIndex = keyCodeToLoadoutsSlot(title, event.getKeyCode(), event.getScanCode());
        if (slotIndex < 0) return;

        event.setCancelled(true);
        clickLoadoutsSlot(slotIndex);

        if (Configuration.loadoutsSound) {
            if (mc.level == null || mc.player == null) return;
            mc.level.playSound(
                    mc.player, mc.player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 2.0f, 1.0f
            );
        }

        log.debug("Loadouts action on slot {}", slotIndex);
    }

    private int keyCodeToLoadoutsSlot(@NotNull String title, int keyCode, int scanCode) {
        List<KeyMapping> loadoutsSlotKeys = IQKeyBindings.getLoadoutsSlotKeys();
        for (int slot = 0; slot < loadoutsSlotKeys.size(); slot++) {
            if (loadoutsSlotKeys.get(slot).matches(new KeyEvent(keyCode, scanCode, 0))) {
                return LOADOUTS_SLOT_INDICES[slot];
            }
        }

        KeyMapping nextPageKey = IQKeyBindings.getLoadoutsNextPageKey();
        if (nextPageKey != null
                && nextPageKey.matches(new KeyEvent(keyCode, scanCode, 0))
                && (title.contains(LOADOUTS_PAGE_ONE_TITLE) || title.contains(LOADOUTS_PAGE_TWO_TITLE))) {
            return LOADOUTS_NEXT_PAGE_SLOT;
        }

        KeyMapping previousPageKey = IQKeyBindings.getLoadoutsPreviousPageKey();
        if (previousPageKey != null
                && previousPageKey.matches(new KeyEvent(keyCode, scanCode, 0))
                && (title.contains(LOADOUTS_PAGE_TWO_TITLE) || title.contains(LOADOUTS_PAGE_THREE_TITLE))) {
            return LOADOUTS_PREVIOUS_PAGE_SLOT;
        }

        return -1;
    }

    private void clickLoadoutsSlot(int slotIndex) {
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
