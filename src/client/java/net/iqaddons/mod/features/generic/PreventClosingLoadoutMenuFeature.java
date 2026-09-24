package net.iqaddons.mod.features.generic;

import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.features.Feature;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public class PreventClosingLoadoutMenuFeature extends Feature {

    private static final String LOADOUTS_PAGE_ONE_TITLE = "(1/3) Loadouts";
    private static final String LOADOUTS_PAGE_TWO_TITLE = "(2/3) Loadouts";
    private static final String LOADOUTS_PAGE_THREE_TITLE = "(3/3) Loadouts";
    private static final int LOADOUTS_CLOSE_SLOT = 49;

    public PreventClosingLoadoutMenuFeature() {
        super(
                "preventClosingLoadoutMenu",
                "Prevent Closing Loadout Menu",
                () -> Configuration.preventClosingLoadoutMenu
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ScreenClickEvent.class, this::onScreenClick);
    }

    private void onScreenClick(@NotNull ScreenClickEvent event) {
        if (!isLoadoutsPageTitle(event.getScreen().getTitle().getString())) return;
        if (event.getActionType() != ContainerInput.PICKUP) return;
        if (event.getButton() != 0 && event.getButton() != 1) return;

        Slot slot = event.getSlot();
        if (slot == null || slot.index != LOADOUTS_CLOSE_SLOT) return;

        event.setCancelled(true);
    }

    private boolean isLoadoutsPageTitle(@NotNull String title) {
        return title.contains(LOADOUTS_PAGE_ONE_TITLE)
                || title.contains(LOADOUTS_PAGE_TWO_TITLE)
                || title.contains(LOADOUTS_PAGE_THREE_TITLE);
    }
}
