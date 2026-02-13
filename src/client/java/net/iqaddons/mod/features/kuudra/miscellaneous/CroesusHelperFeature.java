package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ScreenDrawSlotEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.ChestProfitUtil;
import org.jetbrains.annotations.NotNull;

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
    }

    private void onScreenDrawSlot(@NotNull ScreenDrawSlotEvent event) {
        var title = event.screen().getTitle().getString().toLowerCase();
        if (!title.contains("croesus") && !title.contains("vesuvius")) return;

        var slot = event.slot();
        var hasOpenedChest = ChestProfitUtil.getLoreLines(slot.getStack()).stream()
                .map(String::toLowerCase)
                .anyMatch(line -> line.contains(OPENED_CHEST_LORE.toLowerCase()));
        if (hasOpenedChest) {
            int left = slot.x;
            int top = slot.y;

            event.drawContext().fill(left, top, left + 16, top + 16, 0x66FF0000);
        }
    }
}
