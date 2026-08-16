package net.iqaddons.mod.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
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
            int argb
    ) {
        if (size <= 0.0f || ((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        int texW = Math.max(1, texture.getTexture().getWidth(0));
        int texH = Math.max(1, texture.getTexture().getHeight(0));
        float scale = Math.min(size / texW, size / texH);
        int drawW = Math.max(1, Math.round(texW * scale));
        int drawH = Math.max(1, Math.round(texH * scale));
        int drawX = Math.round(x + (size - drawW) * 0.5f);
        int drawY = Math.round(y + (size - drawH) * 0.5f);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                id,
                drawX,
                drawY,
                0.0f,
                0.0f,
                drawW,
                drawH,
                texW,
                texH,
                texW,
                texH,
                argb
        );
    }

    public static void drawNative(
            GuiGraphicsExtractor graphics,
            Identifier id,
            float x,
            float y,
            int argb
    ) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        int texW = Math.max(1, texture.getTexture().getWidth(0));
        int texH = Math.max(1, texture.getTexture().getHeight(0));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                id,
                Math.round(x),
                Math.round(y),
                0.0f,
                0.0f,
                texW,
                texH,
                texW,
                texH,
                texW,
                texH,
                argb
        );
    }

    public static int nativeSize(Identifier id) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        return Math.max(
                texture.getTexture().getWidth(0),
                texture.getTexture().getHeight(0)
        );
    }
}
