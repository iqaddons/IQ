package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ScreenDrawSlotEvent;
import net.iqaddons.mod.events.impl.ScreenKeyPressEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public class CroesusHelperFeature extends Feature {

    private static final String OPENED_CHEST_LORE = "No more chests to open!";

    public CroesusHelperFeature() {
        super("croesusHelper", "Croesus Helper",
                () -> KuudraGeneralConfig.croesusHelper
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ScreenDrawSlotEvent.class, this::onScreenDrawSlot);
        subscribe(ScreenKeyPressEvent.class, this::onScreenKeyPress);
    }

    private void onScreenDrawSlot(@NotNull ScreenDrawSlotEvent event) {
        var title = event.screen().getTitle().getString().toLowerCase();
        if (!title.contains("croesus") && !title.contains("vesuvius")) return;

        var slot = event.slot();
        var hasOpenedChest = ChestProfitUtil.getLoreLines(slot.getItem()).stream()
                .map(String::toLowerCase)
                .anyMatch(line -> line.contains(OPENED_CHEST_LORE.toLowerCase()));
        if (hasOpenedChest) {
            int left = slot.x;
            int top = slot.y;

            event.drawContext().fill(left, top, left + 16, top + 16, 0x66FF0000);
        }
    }

    private void onScreenKeyPress(@NotNull ScreenKeyPressEvent event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> handledScreen)) return;
        if (!isCroesusScreen(event.getScreenTitle())) return;

        Optional<Slot> navigationSlot;
        if (IQKeyBindings.getAdvanceCroesusPageKey().matches(new KeyEvent(event.getKeyCode(), event.getScanCode(), 0))) {
            navigationSlot = findNavigationSlot(handledScreen, "next");
        } else if (IQKeyBindings.getGoBackCroesusPageKey().matches(new KeyEvent(event.getKeyCode(), event.getScanCode(), 0))) {
            navigationSlot = findNavigationSlot(handledScreen, "previous", "back");
        } else {
            return;
        }

        if (navigationSlot.isEmpty() || mc.player == null || mc.gameMode == null) return;

        event.setCancelled(true);
        mc.gameMode.handleContainerInput(
                handledScreen.getMenu().containerId,
                navigationSlot.get().index,
                0,
                ContainerInput.CLONE,
                mc.player
        );
    }

    private boolean isCroesusScreen(@NotNull String title) {
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        return normalizedTitle.contains("croesus") || normalizedTitle.contains("vesuvius");
    }

    private @NotNull Optional<Slot> findNavigationSlot(@NotNull AbstractContainerScreen<?> screen, @NotNull String @NotNull ... labels) {
        return screen.getMenu().slots.stream()
                .filter(slot -> hasAnyLabel(slot, labels))
                .findFirst();
    }

    private boolean hasAnyLabel(@NotNull Slot slot, @NotNull String @NotNull ... labels) {
        if (!slot.hasItem()) return false;
        String stackName = StringUtils.stripFormatting(slot.getItem().getHoverName().getString()).toLowerCase(Locale.ROOT);
        for (String label : labels) {
            if (stackName.contains(label)) {
                return true;
            }
        }

        return false;
    }
}
