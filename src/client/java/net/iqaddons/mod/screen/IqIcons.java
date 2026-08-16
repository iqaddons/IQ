package net.iqaddons.mod.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class IqIcons {

    private IqIcons() {
    }

    public static void draw(
            GuiGraphicsExtractor graphics,
            Identifier id,
            float x,
            float y,
            float size,
            int texSize,
            int argb
    ) {
        if (size <= 0.0f || ((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        int pixelSize = Math.max(1, Math.round(size));
        int drawX = Math.round(x + (size - pixelSize) * 0.5f);
        int drawY = Math.round(y + (size - pixelSize) * 0.5f);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                id,
                drawX,
                drawY,
                0.0f,
                0.0f,
                pixelSize,
                pixelSize,
                texSize,
                texSize,
                texSize,
                texSize,
                argb
        );
    }
}
