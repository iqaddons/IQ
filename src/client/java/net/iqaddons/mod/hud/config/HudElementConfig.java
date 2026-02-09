package net.iqaddons.mod.hud.config;

import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import org.jetbrains.annotations.NotNull;

public record HudElementConfig(
        @NotNull String id, float x, float y,
        float scale, @NotNull String anchor
) {

    public static final float DEFAULT_SCALE = 1.0f;
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;

    public static @NotNull HudElementConfig fromWidget(@NotNull HudWidget widget) {
        return new HudElementConfig(
                widget.getId(), widget.getX(), widget.getY(),
                widget.getScale(), widget.getAnchor().name()
        );
    }

    public static @NotNull HudElementConfig defaultConfig(
            @NotNull String id,
            float defaultX,
            float defaultY
    ) {
        return new HudElementConfig(
                id, defaultX, defaultY,
                DEFAULT_SCALE, HudAnchor.TOP_LEFT.name()
        );
    }

    public @NotNull HudElementConfig withPosition(float newX, float newY) {
        return new HudElementConfig(id, newX, newY, scale, anchor);
    }

    public @NotNull HudElementConfig withScale(float newScale) {
        float clamped = Math.clamp(newScale, MIN_SCALE, MAX_SCALE);
        return new HudElementConfig(id, x, y, clamped, anchor);
    }

    public void applyTo(@NotNull HudWidget widget) {
        widget.setX(x);
        widget.setY(y);
        widget.setScale(scale);

        try {
            widget.setAnchor(HudAnchor.valueOf(anchor));
        } catch (IllegalArgumentException e) {
            widget.setAnchor(HudAnchor.TOP_LEFT);
        }
    }

    public boolean isScaleValid() {
        return scale >= MIN_SCALE && scale <= MAX_SCALE;
    }

    public @NotNull HudElementConfig validated() {
        if (isScaleValid()) {
            return this;
        }
        return withScale(scale);
    }
}