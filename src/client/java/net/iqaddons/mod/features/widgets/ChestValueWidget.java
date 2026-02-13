package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.model.profit.chest.ChestItemValue;
import net.iqaddons.mod.model.profit.chest.ChestValueBreakdown;
import net.iqaddons.mod.model.profit.chest.type.ChestType;
import net.iqaddons.mod.utils.ChestProfitUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChestValueWidget extends HudWidget {

    private static final DecimalFormat COIN_FORMAT = new DecimalFormat("#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    public ChestValueWidget() {
        super("chestValueWidget", "Chest Value",
                160.0f, 170.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.chestValueWidget);
        setVisibilityCondition(this::isKuudraChestScreenOpen);

        setExampleLines(List.of(
                HudLine.of("§7Total Value: §612.503.632,21"),
                HudLine.of("§7Profit: §a+9.832.543,63"),
                HudLine.of(""),
                HudLine.of("§5Molten Bracelet§f: §6720.000,00"),
                HudLine.of("§dCrimson Essence §8x2000§f: §62.542.763,22")
        ));
    }

    @Override
    protected void onActivate() {
        subscribe(ClientTickEvent.class, event -> {
            if (!event.isNthTick(20)) return;
            updateFromCurrentChest();
        });

        updateFromCurrentChest();
    }

    private void updateFromCurrentChest() {
        List<HudLine> lines = new ArrayList<>();
        if (!isKuudraChestScreenOpen()) {
            setLines(lines);
            return;
        }

        GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
        ChestValueBreakdown breakdown = ChestProfitUtil.analyzeChest(screen.getScreenHandler().slots);

        lines.add(HudLine.of("§7Total Value: §6" + formatCoins(breakdown.totalValue())));

        String profitPrefix = breakdown.profit() >= 0 ? "§a+" : "§c-";
        lines.add(HudLine.of("§7Profit: " + profitPrefix + formatCoins(Math.abs(breakdown.profit()))));
        lines.add(HudLine.of(""));

        for (ChestItemValue item : breakdown.items()) {
            lines.add(HudLine.of(item.displayName().copy()
                    .append(Text.literal(item.count() > 1
                            ? " §8x" + item.count()
                            : ""))
                    .append(Text.literal("§f: §6" + formatCoins(item.value())))
            ));
        }

        setLines(lines);
        markDimensionsDirty();
    }

    private String formatCoins(double value) {
        synchronized (COIN_FORMAT) {
            return COIN_FORMAT.format(value);
        }
    }

    private boolean isKuudraChestScreenOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            return false;
        }

        ChestType type = ChestType.fromString(screen.getTitle().getString());
        return type != ChestType.UNKNOWN;
    }
}
