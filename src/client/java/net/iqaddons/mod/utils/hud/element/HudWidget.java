package net.iqaddons.mod.utils.hud.element;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.utils.hud.HudManager;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Slf4j
@Data
public abstract class HudWidget implements HudElement {

    protected static final MinecraftClient MC = MinecraftClient.getInstance();

    private final String id;
    private final String displayName;

    private float x;
    private float y;
    private float scale;

    private HudAnchor anchor;
    private boolean selected;

    private final List<HudLine> lines = new ArrayList<>();
    private final List<HudLine> exampleLines = new ArrayList<>();

    private BooleanSupplier visibilityCondition = () -> true;
    private BooleanSupplier enabledSupplier = () -> true;

    private boolean active = false;

    private int cachedWidth = 0;
    private int cachedHeight = 0;
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
        this.anchor = defaultAnchor;
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
        if (dimensionsDirty) {
            recalculateDimensions();
        }
        return Math.max(cachedWidth, 1);
    }

    @Override
    public int getHeight() {
        if (dimensionsDirty) {
            recalculateDimensions();
        }
        return Math.max(cachedHeight, 1);
    }

    protected void markDimensionsDirty() {
        dimensionsDirty = true;
    }

    private void recalculateDimensions() {
        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getCurrentRenderableLines();

        int maxWidth = 0;
        int currentLineWidth = 0;
        int totalHeight = 0;

        for (HudLine line : renderLines) {
            if (!line.shouldRender()) continue;

            int lineWidth = textRenderer.getWidth(line.getText());
            currentLineWidth += lineWidth;

            if (line.hasLineBreak()) {
                maxWidth = Math.max(maxWidth, currentLineWidth);
                currentLineWidth = 0;
                totalHeight += textRenderer.fontHeight + 1;
            }
        }

        maxWidth = Math.max(maxWidth, currentLineWidth);

        cachedWidth = Math.max(maxWidth, 20);
        cachedHeight = Math.max(totalHeight, textRenderer.fontHeight);
        dimensionsDirty = false;
    }

    protected void addLine(@NotNull HudLine line) {
        lines.add(line);
        markDimensionsDirty();
    }

    protected void addLineAt(int index, @NotNull HudLine line) {
        lines.add(index, line);
        markDimensionsDirty();
    }

    protected void setLines(@NotNull List<HudLine> newLines) {
        lines.clear();
        lines.addAll(newLines);
        markDimensionsDirty();
    }

    protected void removeLine(@NotNull HudLine line) {
        lines.remove(line);
        markDimensionsDirty();
    }

    protected void clearLines() {
        lines.clear();
        markDimensionsDirty();
    }

    public @NotNull List<HudLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    protected void setExampleLines(@NotNull List<HudLine> examples) {
        exampleLines.clear();
        exampleLines.addAll(examples);
    }

    private @NotNull List<HudLine> getCurrentRenderableLines() {
        if (lines.isEmpty() && !exampleLines.isEmpty() && HudManager.get().isEditorOpen()) {
            return exampleLines;
        }
        return lines;
    }

    protected @NotNull List<HudLine> getRenderableLines() {
        return getCurrentRenderableLines();
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
        onActivate();
        log.debug("HUD Widget activated: {}", displayName);
    }

    public final void deactivate() {
        if (!active) return;
        active = false;
        onDeactivate();
        log.debug("HUD Widget deactivated: {}", displayName);
    }

    protected void onActivate() {}

    protected void onDeactivate() {}

    @Override
    public void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getRenderableLines();
        if (renderLines.isEmpty()) return;

        renderInternal(context, mouseX, mouseY, renderLines, textRenderer);
    }

    @Override
    public void renderExample(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return;

        List<HudLine> renderLines;
        if (!exampleLines.isEmpty()) {
            renderLines = lines.isEmpty() ? exampleLines : lines;
        } else {
            renderLines = lines;
        }

        if (renderLines.isEmpty()) {
            renderEmptyPlaceholder(context, textRenderer);
            return;
        }

        renderInternal(context, mouseX, mouseY, renderLines, textRenderer);
    }

    private void renderInternal(
            @NotNull DrawContext context,
            double mouseX, double mouseY,
            @NotNull List<HudLine> renderLines,
            @NotNull TextRenderer textRenderer
    ) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        float currentX = scaledX;
        float currentY = scaledY;

        int totalWidth = getWidth();
        int totalHeight = getHeight();

        if (selected) {
            renderSelectionBorder(context, (int) scaledX, (int) scaledY, totalWidth, totalHeight, textRenderer);
        }

        if (isMouseOver(mouseX, mouseY) && HudManager.get().isEditorOpen()) {
            context.fill(
                    (int) scaledX, (int) scaledY,
                    (int) scaledX + totalWidth, (int) scaledY + totalHeight,
                    new Color(0, 0, 0, 100).getRGB()
            );
        }

        for (HudLine line : renderLines) {
            if (!line.shouldRender()) continue;

            line.updateHoverState(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale);
            line.render(context, (int) currentX, (int) currentY, textRenderer);

            if (line.hasLineBreak()) {
                currentY += textRenderer.fontHeight + 1;
                currentX = scaledX;
            } else {
                currentX += textRenderer.getWidth(line.getText());
            }
        }

        context.getMatrices().popMatrix();
    }

    private void renderEmptyPlaceholder(
            @NotNull DrawContext context,
            @NotNull TextRenderer textRenderer
    ) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        String placeholder = "§7[" + displayName + "]";
        int width = textRenderer.getWidth(placeholder);
        int height = textRenderer.fontHeight;

        context.fill(
                (int) scaledX - 2, (int) scaledY - 2,
                (int) scaledX + width + 2, (int) scaledY + height + 2,
                new Color(0, 0, 0, 150).getRGB()
        );

        context.drawTextWithShadow(
                textRenderer,
                placeholder,
                (int) scaledX,
                (int) scaledY,
                new Color(128, 128, 128).getRGB()
        );

        if (selected) {
            renderSelectionBorder(context, (int) scaledX - 2, (int) scaledY - 2, width + 4, height + 4, textRenderer);
        }

        context.getMatrices().popMatrix();
    }

    private void renderSelectionBorder(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height,
            @NotNull TextRenderer textRenderer
    ) {
        int borderColor = new Color(255, 0, 0, 170).getRGB();

        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);

        context.drawTextWithShadow(
                textRenderer,
                String.format("X: %.0f Y: %.0f Scale: %.1f", this.x, this.y, scale),
                x,
                y - textRenderer.fontHeight - 2,
                new Color(255, 255, 255, 200).getRGB()
        );
    }

    @Override
    public boolean onClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return false;

        float scaledX = getAbsoluteX() / scale;
        float scaledY = getAbsoluteY() / scale;

        float currentX = scaledX;
        float currentY = scaledY;

        for (HudLine line : getRenderableLines()) {
            if (!line.shouldRender()) continue;
            if (line.handleClick(mouseX, mouseY, currentX * scale, currentY * scale, textRenderer, scale)) {
                return true;
            }

            if (line.hasLineBreak()) {
                currentY += textRenderer.fontHeight + 1;
                currentX = scaledX;
            } else {
                currentX += textRenderer.getWidth(line.getText());
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