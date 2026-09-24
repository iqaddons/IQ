package net.iqaddons.mod.hud.element;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.SubscriptionOwner;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class HudWidget extends SubscriptionOwner implements HudElement {

    protected static final Minecraft mc = Minecraft.getInstance();
    private static final int HOVER_BACKGROUND_COLOR = 0x7D000000;
    private static final int PLACEHOLDER_BACKGROUND_COLOR = 0xBE000000;
    private static final int PLACEHOLDER_TEXT_COLOR = 0xFF808080;
    private static final int SELECTION_BORDER_COLOR = 0xAAFF0000;
    private static final int SELECTION_NAME_COLOR = 0xDCFFFFFF;
    private static final int SELECTION_LOCATION_COLOR = 0xC8FFFFFF;

    private final String id;
    private final String displayName;

    private float x;
    private float y;
    private float scale;

    private final float defaultX;
    private final float defaultY;
    private final float defaultScale;

    private HudAnchor anchor;
    private final HudAnchor defaultAnchor;
    private boolean selected;

    private final List<HudLine> lines = new ArrayList<>();
    private final List<HudLine> exampleLines = new ArrayList<>();

    private BooleanSupplier visibilityCondition = () -> true;
    private BooleanSupplier enabledSupplier = () -> true;

    private boolean active = false;

    private int cachedWidth = 0;
    private int cachedHeight = 0;
    private int nanoWidth = -1;
    private int nanoHeight = -1;
    private boolean dimensionsDirty = true;

    protected HudWidget(
            @NotNull String id,
            @NotNull String displayName,
            float defaultX,
            float defaultY,
            float defaultScale,
            @NotNull HudAnchor defaultAnchor
    ) {
        this.id = id;
        this.displayName = displayName;
        this.x = defaultX;
        this.y = defaultY;
        this.scale = defaultScale;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.defaultScale = defaultScale;
        this.anchor = defaultAnchor;
        this.defaultAnchor = defaultAnchor;
    }

    protected HudWidget(
            @NotNull String id,
            @NotNull String displayName,
            float defaultX,
            float defaultY
    ) {
        this(id, displayName, defaultX, defaultY, 1.0f, HudAnchor.TOP_LEFT);
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void resetToDefaults() {
        this.x = defaultX;
        this.y = defaultY;
        this.scale = defaultScale;
        this.anchor = defaultAnchor;
        markDimensionsDirty();
    }

    public float getAbsoluteX() {
        int[] screen = HudAnchor.getScreenDimensions();
        return anchor.calculateX(screen[0], getScaledWidth(), x);
    }

    public float getAbsoluteY() {
        int[] screen = HudAnchor.getScreenDimensions();
        return anchor.calculateY(screen[1], getScaledHeight(), y);
    }

    @Override
    public int getWidth() {
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle() && nanoWidth > 0) {
            return nanoWidth;
        }
        if (dimensionsDirty) {
            recalculateDimensions();
        }
        return Math.max(cachedWidth, 1);
    }

    @Override
    public int getHeight() {
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle() && nanoHeight > 0) {
            return nanoHeight;
        }
        if (dimensionsDirty) {
            recalculateDimensions();
        }
        return Math.max(cachedHeight, 1);
    }

    protected void markDimensionsDirty() {
        dimensionsDirty = true;
    }

    /**
     * Public wrapper to force dimension recalculation.
     */
    public void refreshDimensions() {
        markDimensionsDirty();
    }

    private void recalculateDimensions() {
        Font textRenderer = mc.font;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getRenderableLines();

        int maxWidth = 0;
        int currentLineWidth = 0;
        int totalHeight = 0;

        for (HudLine line : renderLines) {
            if (!line.shouldRender()) continue;

            int lineWidth = line.getWidth(textRenderer);
            currentLineWidth += lineWidth;

            if (line.hasLineBreak()) {
                maxWidth = Math.max(maxWidth, currentLineWidth);
                currentLineWidth = 0;
                totalHeight += textRenderer.lineHeight + 1;
            }
        }

        maxWidth = Math.max(maxWidth, currentLineWidth);

        cachedWidth = Math.max(maxWidth, 20);
        cachedHeight = Math.max(totalHeight, textRenderer.lineHeight);
        dimensionsDirty = false;
    }

    protected synchronized void addLine(@NotNull HudLine line) {
        lines.add(line);
        markDimensionsDirty();
    }

    protected synchronized void addLines(HudLine @NotNull ... lines) {
        for (HudLine line : lines) {
            addLine(line);
        }
    }

    protected synchronized void addLineAt(int index, @NotNull HudLine line) {
        lines.add(index, line);
        markDimensionsDirty();
    }

    protected synchronized void setLines(@NotNull List<HudLine> newLines) {
        lines.clear();
        lines.addAll(newLines);
        markDimensionsDirty();
    }

    protected synchronized void removeLine(@NotNull HudLine line) {
        lines.remove(line);
        markDimensionsDirty();
    }

    protected synchronized void clearLines() {
        lines.clear();
        markDimensionsDirty();
    }

    public synchronized @NotNull List<HudLine> getLines() {
        return Collections.unmodifiableList(new ArrayList<>(lines));
    }

    protected synchronized void setExampleLines(HudLine @NotNull ... examples) {
        exampleLines.clear();
        Collections.addAll(exampleLines, examples);
    }

    protected synchronized void setExampleLines(@NotNull List<HudLine> examples) {
        exampleLines.clear();
        exampleLines.addAll(examples);
    }

    private @NotNull List<HudLine> getCurrentRenderableLines() {
        if (HudManager.get().isEditorOpen() && !exampleLines.isEmpty()) {
            return exampleLines;
        }

        if (lines.isEmpty() && !exampleLines.isEmpty()) {
            return exampleLines;
        }

        return lines;
    }

    protected synchronized @NotNull List<HudLine> getRenderableLines() {
        return new ArrayList<>(getCurrentRenderableLines());
    }

    public synchronized @NotNull List<HudLine> getNanoRenderableLines() {
        return Collections.unmodifiableList(new ArrayList<>(getCurrentRenderableLines()));
    }

    public float getNanoLineStartX(@NotNull Font textRenderer, @NotNull HudLine line) {
        return getLineStartX(textRenderer, line);
    }

    public boolean isNanoLineCentered(@NotNull Font textRenderer, @NotNull HudLine line) {
        return false;
    }

    public void setNanoDimensions(int width, int height) {
        this.nanoWidth = Math.max(width, 1);
        this.nanoHeight = Math.max(height, 1);
    }

    public boolean isNanoActive() {
        return active;
    }

    public boolean isNanoEnabled() {
        return enabledSupplier.getAsBoolean();
    }

    @Override
    public void setVisibilityCondition(@NotNull BooleanSupplier condition) {
        this.visibilityCondition = condition;
    }

    public void setEnabledSupplier(@NotNull BooleanSupplier supplier) {
        this.enabledSupplier = supplier;
    }

    @Override
    public boolean shouldRender() {
        return enabledSupplier.getAsBoolean() && visibilityCondition.getAsBoolean();
    }

    public final void activate() {
        if (active) return;

        active = true;
        try {
            onActivate();
        } catch (Exception e) {
            log.error("Failed to activate widget", e);
            clearSubscriptions();
        }

        log.debug("HUD Widget activated: {}", displayName);
    }

    public final void deactivate() {
        if (!active) return;

        active = false;
        onDeactivate();
        clearSubscriptions();

        log.debug("HUD Widget deactivated: {}", displayName);
    }

    protected void onActivate() {}

    protected void onDeactivate() {}

    protected float getLineStartX(@NotNull Font textRenderer) {
        return 0.0f;
    }

    protected float getLineStartX(@NotNull Font textRenderer, @NotNull HudLine line) {
        return getLineStartX(textRenderer);
    }

    protected void renderBeforeLines(
            @NotNull GuiGraphicsExtractor context,
            float x,
            float y,
            int width,
            int height,
            @NotNull Font textRenderer
    ) {}

    @Override
    public void render(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        var textRenderer = mc.font;
        if (textRenderer == null) return;

        var renderLines = getRenderableLines();
        if (renderLines.isEmpty()) return;

        renderInternal(context, mouseX, mouseY, renderLines, textRenderer);
    }

    @Override
    public void renderExample(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        var textRenderer = mc.font;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getRenderableLines();
        if (renderLines.isEmpty()) {
            renderEmptyPlaceholder(context, textRenderer);
            return;
        }

        renderInternal(context, mouseX, mouseY, renderLines, textRenderer);
    }

    private void renderInternal(
            @NotNull GuiGraphicsExtractor context,
            double mouseX, double mouseY,
            @NotNull List<HudLine> renderLines,
            @NotNull Font textRenderer
    ) {
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        float currentX = scaledX;
        float currentY = scaledY;
        boolean atLineStart = true;

        int totalWidth = getWidth();
        int totalHeight = getHeight();

        boolean modernHud = IqNanoGlobalConfigScreen.isSharedModernHudStyle();
        if (selected && !modernHud) {
            renderSelectionBorder(context, (int) scaledX, (int) scaledY, totalWidth, totalHeight, textRenderer);
        }

        if (!modernHud && isMouseOver(mouseX, mouseY) && HudManager.get().isEditorOpen()) {
            context.fill(
                    (int) scaledX, (int) scaledY,
                    (int) scaledX + totalWidth, (int) scaledY + totalHeight,
                    HOVER_BACKGROUND_COLOR
            );
        }

        renderBeforeLines(context, scaledX, scaledY, totalWidth, totalHeight, textRenderer);

        for (HudLine line : renderLines) {
            if (!line.shouldRender()) continue;
            if (atLineStart) {
                currentX = scaledX + getLineStartX(textRenderer, line);
            }

            line.updateHoverState(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale);
            line.render(context, (int) currentX, (int) currentY, textRenderer);

            if (line.hasLineBreak()) {
                currentY += textRenderer.lineHeight + 1;
                atLineStart = true;
            } else {
                currentX += line.getWidth(textRenderer);
                atLineStart = false;
            }
        }

        context.pose().popMatrix();

        for (HudLine line : renderLines) {
            if (!line.shouldRender()) continue;
            line.renderHover(context, textRenderer);
        }
    }

    private void renderEmptyPlaceholder(
            @NotNull GuiGraphicsExtractor context,
            @NotNull Font textRenderer
    ) {
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        String placeholder = "§7[" + displayName + "]";
        int width = textRenderer.width(placeholder);
        int height = textRenderer.lineHeight;

        context.fill(
                (int) scaledX - 2, (int) scaledY - 2,
                (int) scaledX + width + 2, (int) scaledY + height + 2,
                PLACEHOLDER_BACKGROUND_COLOR
        );

        context.text(
                textRenderer,
                placeholder,
                (int) scaledX,
                (int) scaledY,
                PLACEHOLDER_TEXT_COLOR
        );

        if (selected) {
            renderSelectionBorder(context, (int) scaledX - 2, (int) scaledY - 2, width + 4, height + 4, textRenderer);
        }

        context.pose().popMatrix();
    }

    private void renderSelectionBorder(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height,
            @NotNull Font textRenderer
    ) {
        context.fill(x, y, x + width, y + 1, SELECTION_BORDER_COLOR);
        context.fill(x, y + height - 1, x + width, y + height, SELECTION_BORDER_COLOR);
        context.fill(x, y, x + 1, y + height, SELECTION_BORDER_COLOR);
        context.fill(x + width - 1, y, x + width, y + height, SELECTION_BORDER_COLOR);

        String widgetName = displayName;
        String widgetLocation = "X: " + Math.round(this.x) + " Y: " + Math.round(this.y);

        int nameWidth = textRenderer.width(widgetName);
        int locationWidth = textRenderer.width(widgetLocation);

        int nameX = x + (width - nameWidth) / 2;
        int locationX = x + (width - locationWidth) / 2;

        context.text(
                textRenderer,
                widgetName,
                nameX,
                y - textRenderer.lineHeight - 2,
                SELECTION_NAME_COLOR
        );

        context.text(
                textRenderer,
                widgetLocation,
                locationX,
                y + height + 2,
                SELECTION_LOCATION_COLOR
        );
    }

    @Override
    public boolean onClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        Font textRenderer = mc.font;
        if (textRenderer == null) return false;

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        float currentX = scaledX;
        float currentY = scaledY;
        boolean atLineStart = true;

        for (HudLine line : getRenderableLines()) {
            if (!line.shouldRender()) continue;
            if (atLineStart) {
                currentX = scaledX + getLineStartX(textRenderer, line);
            }

            if (line.handleClick(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale)) {
                return true;
            }

            if (line.hasLineBreak()) {
                currentY += textRenderer.lineHeight + 1;
                atLineStart = true;
            } else {
                currentX += line.getWidth(textRenderer);
                atLineStart = false;
            }
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float absX = getAbsoluteX();
        float absY = getAbsoluteY();
        int width = getScaledWidth();
        int height = getScaledHeight();

        return mouseX >= absX && mouseX <= absX + width
                && mouseY >= absY && mouseY <= absY + height;
    }

    @Override
    public void onConfigChanged() {
        HudManager.get().saveConfig();
    }

}
