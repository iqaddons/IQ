package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.kuudra.tracker.ChestCounterTrackerFeature;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Slf4j
public class ChestCounterWidget extends HudWidget {

    /**
     * The icon is rendered directly as a texture instead of a custom bitmap font glyph.
     * This avoids the "white square" bug caused by some mods (e.g. ImmediatelyFast) that
     * interfere with custom font providers, causing the glyph to fall back to a blank white box.
     */
    private static final Identifier KUUDRA_TEXTURE = Identifier.of("iq", "textures/widgets/kuudra_icon.png");
    private static final int KUUDRA_TEXTURE_SIZE = 284;
    // Visible icon bounds inside kuudra_icon.png (avoids scaling transparent margins).
    private static final int ICON_U = 49;
    private static final int ICON_V = 38;
    private static final int ICON_SOURCE_WIDTH = 186;
    private static final int ICON_SOURCE_HEIGHT = 208;
    private static final int ICON_SIZE_FALLBACK = 10;
    private static final int ICON_GAP  = 3;  // pixels between icon and text
    private static final int ICON_Y_OFFSET = -1;

    private static final Set<String> KUUDRA_AREAS = Set.of(
            "Dungeon Hub",
            "Forgotten Skull",
            "Kuudra's Hollow"
    );

    private final HudLine line = HudLine.of(buildText("§f", 0));

    public ChestCounterWidget() {
        super("chestCounterWidget", "Chest Counter",
                581.0f, 490.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.chestCounterTracker);
        setVisibilityCondition(() -> {
            if (ChestCounterManager.get().getChests() <= 0) return false;
            if (ChestCounterTrackerFeature.overlayVisible) return true;
            String area = ScoreboardUtils.getArea();
            return KUUDRA_AREAS.stream().anyMatch(area::startsWith);
        });

        setExampleLines(HudLine.of(buildText("§a", 20)));
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

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected float getLineStartX(@NotNull TextRenderer textRenderer) {
        return getIconRenderWidth() + ICON_GAP;
    }

    @Override
    protected void renderBeforeLines(
            @NotNull DrawContext context,
            float x,
            float y,
            int width,
            int height,
            @NotNull TextRenderer textRenderer
    ) {
        drawKuudraIcon(context, x, y, textRenderer);
    }

    /**
     * Include the icon's footprint in the reported widget width so that the HUD-editor
     * selection / drag box correctly wraps the full visible element.
     */
    @Override
    public int getWidth() {
        return super.getWidth() + getIconRenderWidth() + ICON_GAP;
    }

    @Override
    public int getHeight() {
        return Math.max(super.getHeight(), getIconRenderHeight(mc.textRenderer));
    }

    private void drawKuudraIcon(
            @NotNull DrawContext context,
            float x,
            float y,
            @NotNull TextRenderer textRenderer
    ) {
        int iconWidth = getIconRenderWidth();
        int iconHeight = getIconRenderHeight(textRenderer);
        int iconX = Math.round(x);
        int iconY = Math.round(y) + ICON_Y_OFFSET;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, KUUDRA_TEXTURE,
                iconX, iconY, (float) ICON_U, (float) ICON_V,
                iconWidth, iconHeight,
                ICON_SOURCE_WIDTH, ICON_SOURCE_HEIGHT,
                KUUDRA_TEXTURE_SIZE, KUUDRA_TEXTURE_SIZE);
    }

    private int getIconRenderHeight(TextRenderer textRenderer) {
        if (textRenderer == null) {
            return ICON_SIZE_FALLBACK;
        }

        return Math.max(1, textRenderer.fontHeight);
    }

    private int getIconRenderWidth() {
        float aspect = ICON_SOURCE_WIDTH / (float) ICON_SOURCE_HEIGHT;
        int iconHeight = getIconRenderHeight(mc.textRenderer);
        return Math.max(1, Math.round(iconHeight * aspect));
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void updateLine() {
        int chests = ChestCounterManager.get().getChests();
        line.text(buildText(getColor(chests), chests));

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

    /** Counter text only — the icon is painted separately via {@link #drawKuudraIcon}. */
    private Text buildText(String color, int chests) {
        return Text.literal(String.format("%s%d§7/%d", color, chests, ChestCounterManager.MAX_CHESTS));
    }
}