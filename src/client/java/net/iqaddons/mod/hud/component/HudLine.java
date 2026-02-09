package net.iqaddons.mod.hud.component;

import lombok.Data;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

@Data
public class HudLine {

    private String text;
    private OrderedText orderedText;

    private boolean shadow = true;
    private boolean lineBreak = true;

    private int cachedWidth = -1;

    private @Nullable Runnable clickAction;
    private @Nullable Runnable mouseEnterAction;
    private @Nullable Runnable mouseLeaveAction;
    private @Nullable BiConsumer<DrawContext, TextRenderer> hoverAction;

    private boolean hovered = false;
    private BooleanSupplier visibilityCondition = () -> true;

    private HudLine(@NotNull String text) {
        this.text = text;
        this.orderedText = Text.of(text).asOrderedText();
    }

    public static @NotNull HudLine of(@NotNull String text) {
        return new HudLine(text);
    }

    public static @NotNull HudLine formatted(@NotNull String format, Object... args) {
        return new HudLine(String.format(format, args));
    }

    public static @NotNull HudLine empty() {
        return new HudLine("");
    }

    public static @NotNull HudLine inline(@NotNull String text) {
        HudLine line = new HudLine(text);
        line.lineBreak = false;
        return line;
    }

    public @NotNull HudLine text(@NotNull String text) {
        this.text = text;
        this.orderedText = Text.of(text).asOrderedText();
        this.cachedWidth = -1;
        return this;
    }

    public int getWidth(@NotNull TextRenderer textRenderer) {
        if (cachedWidth < 0) {
            cachedWidth = textRenderer.getWidth(orderedText);
        }

        return cachedWidth;
    }

    public @NotNull HudLine onClick(@NotNull Runnable action) {
        this.clickAction = action;
        return this;
    }

    public @NotNull HudLine onMouseEnter(@NotNull Runnable action) {
        this.mouseEnterAction = action;
        return this;
    }

    public @NotNull HudLine onMouseLeave(@NotNull Runnable action) {
        this.mouseLeaveAction = action;
        return this;
    }

    public @NotNull HudLine onHover(@NotNull BiConsumer<DrawContext, TextRenderer> action) {
        this.hoverAction = action;
        return this;
    }

    public @NotNull HudLine showWhen(@NotNull BooleanSupplier condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public @NotNull HudLine showWhenNonZero(@NotNull java.util.function.IntSupplier valueSupplier) {
        this.visibilityCondition = () -> valueSupplier.getAsInt() != 0;
        return this;
    }

    public boolean shouldRender() {
        return visibilityCondition.getAsBoolean();
    }

    public boolean hasLineBreak() {
        return lineBreak;
    }

    public boolean isInteractive() {
        return clickAction != null || hoverAction != null
                || mouseEnterAction != null || mouseLeaveAction != null;
    }

    public boolean isMouseOver(
            double mouseX, double mouseY,
            float lineX, float lineY,
            @NotNull TextRenderer textRenderer,
            float scale
    ) {
        if (text.isEmpty()) return false;

        float width = getWidth(textRenderer) * scale;
        float height = (textRenderer.fontHeight + 1) * scale - 1;

        return mouseX >= lineX && mouseX <= lineX + width
                && mouseY >= lineY && mouseY <= lineY + height;
    }

    public void updateHoverState(
            double mouseX, double mouseY,
            float lineX, float lineY,
            @NotNull TextRenderer textRenderer,
            float scale
    ) {
        if (!isInteractive() || text.isEmpty()) return;

        boolean wasHovered = hovered;
        boolean isNowHovered = isMouseOver(mouseX, mouseY, lineX, lineY, textRenderer, scale);
        hovered = isNowHovered;

        if (isNowHovered && !wasHovered && mouseEnterAction != null) {
            mouseEnterAction.run();
        } else if (!isNowHovered && wasHovered && mouseLeaveAction != null) {
            mouseLeaveAction.run();
        }
    }

    public boolean handleClick(
            double mouseX, double mouseY,
            float lineX, float lineY,
            @NotNull TextRenderer textRenderer,
            float scale
    ) {
        if (clickAction == null || text.isEmpty()) return false;

        if (isMouseOver(mouseX, mouseY, lineX, lineY, textRenderer, scale)) {
            clickAction.run();
            return true;
        }

        return false;
    }

    public void render(
            @NotNull DrawContext context,
            int x, int y,
            @NotNull TextRenderer textRenderer
    ) {
        if (text.isEmpty()) return;

        context.drawText(textRenderer, orderedText, x, y, -1, shadow);
    }

    public void renderHover(@NotNull DrawContext context, @NotNull TextRenderer textRenderer) {
        if (hovered && hoverAction != null) {
            hoverAction.accept(context, textRenderer);
        }
    }

    public static @NotNull HudLine clickable(
            @NotNull String defaultText,
            @NotNull String hoverText,
            @NotNull Runnable onClick
    ) {
        HudLine line = new HudLine(defaultText);
        line.onClick(onClick);
        line.onMouseEnter(() -> line.text(hoverText));
        line.onMouseLeave(() -> line.text(defaultText));
        return line;
    }

    public static @NotNull HudLine button(@NotNull String text, @NotNull Runnable onClick) {
        return clickable(text, text + "§n", onClick);
    }
}