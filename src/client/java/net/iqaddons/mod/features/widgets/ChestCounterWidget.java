package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.ChestCounterManager;

import java.util.List;

public class ChestCounterWidget extends HudWidget {

    private final HudLine line = HudLine.of("§b§lChests §f§l0/60");

    public ChestCounterWidget() {
        super("chestCounterWidget", "Chest Counter", 10.0f, 110.0f, 1.0f, HudAnchor.TOP_LEFT);
        setEnabledSupplier(() -> KuudraGeneralConfig.chestCounterTracker);

        setExampleLines(List.of(HudLine.of("§b§lChests §a§l20§f§l/60")));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLine(line);

        subscribe(ClientTickEvent.class, event -> {
            if (event.isNthTick(5)) {
                updateLine();
            }
        });

        updateLine();
    }

    private void updateLine() {
        int chests = ChestCounterManager.get().getChests();
        line.text("§b§lChests " + getColor(chests) + "§l" + chests + "§f§l/" + ChestCounterManager.MAX_CHESTS);
        markDimensionsDirty();
    }

    private String getColor(int count) {
        if (count >= 60) return "§c";
        if (count >= 50) return "§4";
        if (count >= 40) return "§6";
        if (count >= 30) return "§e";
        if (count >= 20) return "§a";
        if (count >= 10) return "§2";
        return "§f";
    }
}