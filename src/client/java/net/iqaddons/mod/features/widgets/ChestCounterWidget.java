package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.kuudra.tracker.ChestCounterTrackerFeature;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.iqaddons.mod.IQConstants.IQ_FONT_IDENTIFIER;

@Slf4j
public class ChestCounterWidget extends HudWidget {

    private static final String KUUDRA_ICON = "\uE000";
    private static final StyleSpriteSource.Font IQ_FONT = new StyleSpriteSource.Font(IQ_FONT_IDENTIFIER);

    private final HudLine line = HudLine.of(Text.literal(KUUDRA_ICON)
            .styled(style -> style.withFont(IQ_FONT))
            .append(Text.literal(" §f0§7/60")));

    public ChestCounterWidget() {
        super("chestCounterWidget", "Chest Counter",
                10.0f, 110.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.chestCounterTracker);
        setVisibilityCondition(
                () -> ChestCounterManager.get().getChests() > 0 && ChestCounterTrackerFeature.overlayVisible
        );

        setExampleLines(List.of(HudLine.of(Text.literal(KUUDRA_ICON)
                .styled(style -> style.withFont(IQ_FONT))
                .append(Text.literal(" §a20§7/60"))))
        );
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
        line.text(Text.literal(KUUDRA_ICON)
                .styled(style -> style.withFont(IQ_FONT))
                .append(Text.literal(String.format(" %s%d§7/%d",
                        getColor(chests), chests, ChestCounterManager.MAX_CHESTS)))
        );

        markDimensionsDirty();
    }

    @Contract(pure = true)
    private @NotNull String getColor(int count) {
        if (count >= 60) return "§4§l";
        if (count >= 50) return "§c";
        if (count >= 40) return "§6";
        if (count >= 30) return "§e";
        if (count >= 20) return "§a";
        if (count >= 10) return "§2";
        return "§f";
    }
}