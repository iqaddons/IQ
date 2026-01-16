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

    protected @NotNull List<HudLine> getRenderableLines() {
        if (lines.isEmpty() && !exampleLines.isEmpty() && HudManager.get().isEditorOpen()) {
            return exampleLines;
        }
        return lines;
    }

    @Override
    public int getWidth() {
        if (dimensionsDirty) {
            recalculateDimensions();
        }

        return cachedWidth;
    }

    @Override
    public int getHeight() {
        if (dimensionsDirty) {
            recalculateDimensions();
        }

        return cachedHeight;
    }

    protected void markDimensionsDirty() {
        dimensionsDirty = true;
    }

    private void recalculateDimensions() {
        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getRenderableLines();

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

        cachedWidth = maxWidth;
        cachedHeight = totalHeight;
        dimensionsDirty = false;
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
        if (!shouldRender() && !HudManager.get().isEditorOpen()) return;

        TextRenderer textRenderer = MC.textRenderer;
        if (textRenderer == null) return;

        List<HudLine> renderLines = getRenderableLines();
        if (renderLines.isEmpty()) return;

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

    private void renderSelectionBorder(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height,
            @NotNull TextRenderer textRenderer
    ) {
        int borderColor = new Color(255, 0, 0, 170).getRGB();
        context.drawStrokedRectangle(x, y, width, height, borderColor);

        String info = String.format("X: %.0f Y: %.0f Scale: %.1f", this.x, this.y, scale);
        context.drawTextWithShadow(
                textRenderer,
                info,
                x,
                y - textRenderer.fontHeight - 2,
                new Color(255, 255, 255, 200).getRGB()
        );
    }

    @Override
    public void renderExample(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        if (!exampleLines.isEmpty()) {
            List<HudLine> originalLines = new ArrayList<>(lines);
            lines.clear();
            lines.addAll(exampleLines);
            markDimensionsDirty();

            render(context, mouseX, mouseY, delta);

            lines.clear();
            lines.addAll(originalLines);
            markDimensionsDirty();
        } else {
            render(context, mouseX, mouseY, delta);
        }
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

    public void loadConfig(
            @Nullable Float savedX,
            @Nullable Float savedY,
            @Nullable Float savedScale,
            @Nullable String savedAnchor
    ) {
        if (savedX != null) this.x = savedX;
        if (savedY != null) this.y = savedY;
        if (savedScale != null) this.scale = savedScale;
        if (savedAnchor != null) {
            try {
                this.anchor = HudAnchor.valueOf(savedAnchor);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid anchor name: {}", savedAnchor);
            }
        }
    }
}