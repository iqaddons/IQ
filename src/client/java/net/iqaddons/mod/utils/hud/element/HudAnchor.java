package net.iqaddons.mod.utils.hud.element;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

public enum HudAnchor {

    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    CENTER_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float xFactor;
    private final float yFactor;

    HudAnchor(float xFactor, float yFactor) {
        this.xFactor = xFactor;
        this.yFactor = yFactor;
    }

    public float calculateX(int screenWidth, int elementWidth, float offsetX) {
        float anchorX = screenWidth * xFactor;

        return switch (this) {
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> anchorX - elementWidth + offsetX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> anchorX - (elementWidth / 2.0f) + offsetX;
            default -> anchorX + offsetX;
        };
    }

    public float calculateY(int screenHeight, int elementHeight, float offsetY) {
        float anchorY = screenHeight * yFactor;

        return switch (this) {
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> anchorY - elementHeight + offsetY;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> anchorY - (elementHeight / 2.0f) + offsetY;
            default -> anchorY + offsetY;
        };
    }

    public float toOffsetX(float absoluteX, int screenWidth, int elementWidth) {
        float anchorX = screenWidth * xFactor;

        return switch (this) {
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> absoluteX - anchorX + elementWidth;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> absoluteX - anchorX + (elementWidth / 2.0f);
            default -> absoluteX - anchorX;
        };
    }

    public float toOffsetY(float absoluteY, int screenHeight, int elementHeight) {
        float anchorY = screenHeight * yFactor;

        return switch (this) {
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> absoluteY - anchorY + elementHeight;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> absoluteY - anchorY + (elementHeight / 2.0f);
            default -> absoluteY - anchorY;
        };
    }

    public static @NotNull HudAnchor suggestAnchor(float x, float y, int screenWidth, int screenHeight) {
        float relX = x / screenWidth;
        float relY = y / screenHeight;

        int xZone = relX < 0.33f ? 0 : (relX < 0.66f ? 1 : 2);
        int yZone = relY < 0.33f ? 0 : (relY < 0.66f ? 1 : 2);

        return switch (yZone * 3 + xZone) {
            case 1 -> TOP_CENTER;
            case 2 -> TOP_RIGHT;
            case 3 -> CENTER_LEFT;
            case 4 -> CENTER;
            case 5 -> CENTER_RIGHT;
            case 6 -> BOTTOM_LEFT;
            case 7 -> BOTTOM_CENTER;
            case 8 -> BOTTOM_RIGHT;
            default -> TOP_LEFT;
        };
    }

    public static int @NotNull [] getScreenDimensions() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return new int[]{
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight()
        };
    }
}