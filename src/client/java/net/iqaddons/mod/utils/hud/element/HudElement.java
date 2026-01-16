package net.iqaddons.mod.utils.hud.element;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public interface HudElement {

    @NotNull String getId();
    @NotNull String getDisplayName();

    void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta);

    boolean onClick(double mouseX, double mouseY, int button);

    float getX();
    float getY();

    void setPosition(float x, float y);

    float getScale();
    void setScale(float scale);

    @NotNull HudAnchor getAnchor();
    void setAnchor(@NotNull HudAnchor anchor);

    int getWidth();
    int getHeight();

    default int getScaledWidth() {
        return (int) (getWidth() * getScale());
    }

    default int getScaledHeight() {
        return (int) (getHeight() * getScale());
    }

    default boolean isMouseOver(double mouseX, double mouseY) {
        float x = getX();
        float y = getY();
        int width = getScaledWidth();
        int height = getScaledHeight();

        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    boolean shouldRender();

    void setVisibilityCondition(@NotNull BooleanSupplier condition);

    boolean isSelected();
    void setSelected(boolean selected);

    void onConfigChanged();

    default void renderExample(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        render(context, mouseX, mouseY, delta);
    }
}