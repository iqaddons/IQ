package net.iqaddons.mod.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IqFonts {

    private static final float WORK_SCALE = 4.0f;
    private static final float OUTPUT_SCALE = 2.0f;
    private static final int BASE_PADDING = 2;
    private static final int SHADOW_OFFSET = 1;
    private static final int SHADOW_ALPHA = 112;
    private static final int MAX_CACHED_RUNS = 1280;
    private static final float LOGICAL_SIZE = 9.0f;
    private static final String FONT_RESOURCE = "/assets/iq/font/rubik.ttf";
    private static final int TEXTURE_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
    private static final char SECTION = '\u00A7';

    private static Font baseFont;
    private static final Map<RunKey, CachedRun> RUNS = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<RunKey, CachedRun> eldest) {
            if (size() <= MAX_CACHED_RUNS) {
                return false;
            }
            eldest.getValue().close();
            return true;
        }
    };

    private IqFonts() {
    }

    public static void draw(GuiGraphicsExtractor graphics, String text, float x, float y, int argb) {
        if (text == null || text.isEmpty() || ((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        if (text.indexOf(SECTION) >= 0) {
            drawFormatted(graphics, text, x, y, argb);
            return;
        }
        drawRun(graphics, getOrCreateRun(text, argb), x, y);
    }

    public static void draw(GuiGraphicsExtractor graphics, Component component, float x, float y, int argb) {
        if (component == null) {
            return;
        }
        draw(graphics, component.getString(), x, y, argb);
    }

    public static void drawCentered(
            GuiGraphicsExtractor graphics,
            String text,
            float x,
            float y,
            float width,
            float height,
            int argb
    ) {
        float textW = width(text);
        float textH = lineHeight();
        draw(graphics, text, x + (width - textW) * 0.5f, y + (height - textH) * 0.5f, argb);
    }

    public static float width(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        if (text.indexOf(SECTION) >= 0) {
            return widthFormatted(text);
        }
        return getOrCreateRun(text, 0xFFFFFFFF).advance;
    }

    public static float width(Component component) {
        if (component == null) {
            return 0.0f;
        }
        return width(component.getString());
    }

    public static int widthPx(String text) {
        return Math.round(width(text));
    }

    public static int widthPx(Component component) {
        return Math.round(width(component));
    }

    public static int lineHeight() {
        return Math.max(1, Math.round(getOrCreateRun("Ag", 0xFFFFFFFF).lineHeight));
    }

    public static String trim(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (width(text) <= maxWidth) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        int offset = 0;
        while (offset < text.length()) {
            int cp = text.codePointAt(offset);
            String next = out.toString() + new String(Character.toChars(cp));
            if (width(next) > maxWidth) {
                break;
            }
            out.appendCodePoint(cp);
            offset += Character.charCount(cp);
        }
        return out.toString();
    }

    private static float drawFormatted(GuiGraphicsExtractor graphics, String text, float x, float y, int defaultArgb) {
        float cursor = x;
        int color = defaultArgb;
        int offset = 0;
        StringBuilder chunk = new StringBuilder();
        while (offset < text.length()) {
            char ch = text.charAt(offset);
            if (ch == SECTION && offset + 1 < text.length()) {
                if (!chunk.isEmpty()) {
                    draw(graphics, chunk.toString(), cursor, y, color);
                    cursor += width(chunk.toString());
                    chunk.setLength(0);
                }
                ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(offset + 1));
                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        color = defaultArgb;
                    } else if (formatting.isColor() && formatting.getColor() != null) {
                        color = 0xFF000000 | formatting.getColor();
                    }
                }
                offset += 2;
                continue;
            }
            chunk.append(ch);
            offset++;
        }
        if (!chunk.isEmpty()) {
            draw(graphics, chunk.toString(), cursor, y, color);
            cursor += width(chunk.toString());
        }
        return cursor;
    }

    private static float widthFormatted(String text) {
        float total = 0.0f;
        int offset = 0;
        StringBuilder chunk = new StringBuilder();
        while (offset < text.length()) {
            char ch = text.charAt(offset);
            if (ch == SECTION && offset + 1 < text.length()) {
                if (!chunk.isEmpty()) {
                    total += width(chunk.toString());
                    chunk.setLength(0);
                }
                offset += 2;
                continue;
            }
            chunk.append(ch);
            offset++;
        }
        if (!chunk.isEmpty()) {
            total += width(chunk.toString());
        }
        return total;
    }

    private static CachedRun getOrCreateRun(String text, int argb) {
        RunKey key = new RunKey(text, argb);
        CachedRun existing = RUNS.get(key);
        if (existing != null) {
            return existing;
        }
        CachedRun created = createRun(text, argb);
        RUNS.put(key, created);
        return created;
    }

    private static CachedRun createRun(String text, int argb) {
        Font font = styledFont();
        BufferedImage measuringImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measuringGraphics = measuringImage.createGraphics();
        applyTextHints(measuringGraphics);
        TextLayout layout = new TextLayout(text, font, measuringGraphics.getFontRenderContext());
        Rectangle2D pixelBounds = layout.getPixelBounds(measuringGraphics.getFontRenderContext(), 0.0f, 0.0f);
        measuringGraphics.dispose();

        float advance = layout.getAdvance();
        float nominalHeight = layout.getAscent() + layout.getDescent() + layout.getLeading();
        float leftOverhang = Math.max(0.0f, (float) (-pixelBounds.getX()));
        float rightOverhang = Math.max(0.0f, (float) (pixelBounds.getMaxX() - advance));
        float topOverhang = Math.max(0.0f, (float) (-(layout.getAscent() + pixelBounds.getY())));
        float bottomOverhang = Math.max(0.0f, (float) (layout.getAscent() + pixelBounds.getMaxY() - nominalHeight));

        int leftPadding = Math.max(BASE_PADDING, (int) Math.ceil(leftOverhang / WORK_SCALE));
        int rightPadding = Math.max(BASE_PADDING, (int) Math.ceil(rightOverhang / WORK_SCALE) + SHADOW_OFFSET);
        int topPadding = Math.max(BASE_PADDING, (int) Math.ceil(topOverhang / WORK_SCALE));
        int bottomPadding = Math.max(BASE_PADDING, (int) Math.ceil(bottomOverhang / WORK_SCALE) + SHADOW_OFFSET);
        int logicalAdvance = Math.max(1, (int) Math.ceil(advance / WORK_SCALE));
        int logicalLineHeight = Math.max(1, (int) Math.ceil(nominalHeight / WORK_SCALE));
        int logicalWidth = logicalAdvance + leftPadding + rightPadding;
        int logicalHeight = logicalLineHeight + topPadding + bottomPadding;

        BufferedImage workImage = new BufferedImage(
                logicalWidth * (int) WORK_SCALE,
                logicalHeight * (int) WORK_SCALE,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D workGraphics = workImage.createGraphics();
        applyTextHints(workGraphics);
        workGraphics.setComposite(AlphaComposite.SrcOver);
        float baselineX = leftPadding * WORK_SCALE;
        float baselineY = topPadding * WORK_SCALE + layout.getAscent();
        workGraphics.setColor(new Color(0, 0, 0, SHADOW_ALPHA));
        layout.draw(workGraphics, baselineX + SHADOW_OFFSET * WORK_SCALE, baselineY + SHADOW_OFFSET * WORK_SCALE);
        workGraphics.setColor(Color.WHITE);
        layout.draw(workGraphics, baselineX, baselineY);
        workGraphics.dispose();

        int pixelWidth = logicalWidth * (int) OUTPUT_SCALE;
        int pixelHeight = logicalHeight * (int) OUTPUT_SCALE;
        BufferedImage outputImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D outputGraphics = outputImage.createGraphics();
        outputGraphics.setComposite(AlphaComposite.Src);
        outputGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        outputGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        outputGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        outputGraphics.drawImage(workImage, 0, 0, pixelWidth, pixelHeight, null);
        outputGraphics.dispose();

        GpuTexture texture = uploadTexture(outputImage, argb);
        return new CachedRun(
                texture,
                RenderSystem.getDevice().createTextureView(texture),
                logicalWidth,
                logicalHeight,
                leftPadding,
                topPadding,
                advance / WORK_SCALE,
                nominalHeight / WORK_SCALE
        );
    }

    private static GpuTexture uploadTexture(BufferedImage image, int argb) {
        int width = image.getWidth();
        int height = image.getHeight();
        GpuTexture texture = RenderSystem.getDevice().createTexture(
                () -> "IQ text run",
                TEXTURE_USAGE,
                TextureFormat.RGBA8,
                width,
                height,
                1,
                1
        );
        try (NativeImage nativeImage = new NativeImage(width, height, false)) {
            int colorAlpha = (argb >>> 24) & 0xFF;
            if (colorAlpha == 0) {
                colorAlpha = 255;
            }
            int cr = (argb >>> 16) & 0xFF;
            int cg = (argb >>> 8) & 0xFF;
            int cb = argb & 0xFF;
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int ink = image.getRGB(px, py);
                    int coverage = (ink >>> 24) & 0xFF;
                    int luminance = (ink >>> 16) & 0xFF;
                    int r = cr * luminance / 255;
                    int g = cg * luminance / 255;
                    int b = cb * luminance / 255;
                    int a = coverage * colorAlpha / 255;
                    nativeImage.setPixelABGR(px, py, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, nativeImage);
        }
        return texture;
    }

    private static void drawRun(GuiGraphicsExtractor graphics, CachedRun run, float x, float y) {
        int drawX = Math.round(x - run.leftPadding);
        int drawY = Math.round(y - run.topPadding);
        graphics.blit(
                run.view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                drawX,
                drawY,
                drawX + run.logicalWidth,
                drawY + run.logicalHeight,
                0.0f,
                1.0f,
                0.0f,
                1.0f
        );
    }

    private static Font styledFont() {
        Map<AttributedCharacterIterator.Attribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.SIZE, LOGICAL_SIZE * WORK_SCALE);
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        return baseFont().deriveFont(attributes);
    }

    private static Font baseFont() {
        if (baseFont == null) {
            try (InputStream in = IqFonts.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing font resource: " + FONT_RESOURCE);
                }
                baseFont = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | FontFormatException e) {
                throw new IllegalStateException("Failed to load Rubik", e);
            }
        }
        return baseFont;
    }

    private static void applyTextHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private record RunKey(String text, int argb) {
    }

    private record CachedRun(
            GpuTexture texture,
            GpuTextureView view,
            int logicalWidth,
            int logicalHeight,
            int leftPadding,
            int topPadding,
            float advance,
            float lineHeight
    ) implements AutoCloseable {
        @Override
        public void close() {
            view.close();
            texture.close();
        }
    }
}
