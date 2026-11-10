package net.iqaddons.mod.render.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

import java.util.ArrayList;
import java.util.List;

/** Central, renderer-agnostic branding policy for the IQ prefix and author names. */
public final class AnimatedBrandingText {
    public static final long CYCLE_MILLIS = 1_800L;
    public static final String SUPPORTER_PREFIX_MARKER = "iqaddons:supporter_prefix";
    private static final String[] NAMES = {"DarkerJota", "DarkJota"};
    private static final int[] NICK_PALETTE = {0x241A22, 0x51263F, 0xFF5AAF};
    private static final int[] PREFIX_PALETTE = {0xFFB3DE, 0xFF4FB4, 0xD84CFF};

    private AnimatedBrandingText() {}

    public static MutableComponent supporterPrefix() {
        return Component.literal("[IQ] ").withStyle(style -> style
                .withBold(true)
                .withColor(0xFF4FB4)
                .withInsertion(SUPPORTER_PREFIX_MARKER));
    }

    public static FormattedCharSequence transform(String text, long millis, boolean supporterPrefixEnabled) {
        if (!containsPossibleName(text)) return sequence(text, Style.EMPTY);
        return transform(sequence(text, Style.EMPTY), millis, supporterPrefixEnabled);
    }

    public static FormattedCharSequence transform(FormattedCharSequence input, long millis, boolean supporterPrefixEnabled) {
        if (input instanceof AnimatedSequence animated) return animated;
        Scan scan = new Scan();
        input.accept(scan);
        if (!containsPossibleName(scan.plain.toString()) && !(supporterPrefixEnabled && scan.hasSupporterPrefix)) return input;
        return new AnimatedSequence(input, millis, supporterPrefixEnabled);
    }

    public static boolean containsNick(String text) { return containsPossibleName(text); }

    public static boolean isTransformed(FormattedCharSequence input) {
        return input instanceof AnimatedSequence;
    }

    public static List<NickRun> nickRuns(String text, long millis) {
        int[] colors = colorsForText(text, millis);
        List<NickRun> runs = new ArrayList<>();
        if (colors.length == 0) return List.of(new NickRun("", null));
        int[] charOffsets = new int[colors.length + 1];
        for (int glyph = 0, charOffset = 0; glyph < colors.length; glyph++) {
            charOffsets[glyph] = charOffset;
            charOffset += Character.charCount(text.codePointAt(charOffset));
            charOffsets[glyph + 1] = charOffset;
        }
        int startGlyph = 0;
        Integer color = colors.length == 0 || colors[0] < 0 ? null : colors[0];
        for (int i = 1; i <= colors.length; i++) {
            Integer next = i == colors.length || colors[i] < 0 ? null : colors[i];
            if (i == colors.length || !java.util.Objects.equals(color, next)) {
                runs.add(new NickRun(text.substring(charOffsets[startGlyph], charOffsets[i]), color));
                startGlyph = i;
                color = next;
            }
        }
        return runs;
    }

    public record NickRun(String text, Integer color) {}

    private static final class AnimatedSequence implements FormattedCharSequence {
        private final FormattedCharSequence source;
        private final long millis;
        private final boolean prefixEnabled;

        private AnimatedSequence(FormattedCharSequence source, long millis, boolean prefixEnabled) {
            this.source = source;
            this.millis = millis;
            this.prefixEnabled = prefixEnabled;
        }

        @Override
        public boolean accept(net.minecraft.util.FormattedCharSink sink) {
            List<Glyph> glyphs = new ArrayList<>();
            source.accept((index, style, codePoint) -> { glyphs.add(new Glyph(index, style, codePoint)); return true; });
            StringBuilder plain = new StringBuilder(glyphs.size());
            for (Glyph glyph : glyphs) plain.appendCodePoint(glyph.codePoint());
            int[] nameColors = colorsForText(plain.toString(), millis);

            for (int i = 0; i < glyphs.size(); i++) {
                Glyph glyph = glyphs.get(i);
                Style style = glyph.style();
                int color = i < nameColors.length ? nameColors[i] : -1;
                if (color < 0 && prefixEnabled && SUPPORTER_PREFIX_MARKER.equals(style.getInsertion()) && !Character.isWhitespace(glyph.codePoint())) {
                    color = waveColor(PREFIX_PALETTE, millis, i);
                }
                if (color >= 0) style = style.withColor(color);
                if (!sink.accept(glyph.index(), style, glyph.codePoint())) return false;
            }
            return true;
        }
    }

    private static int[] colorsForText(String text, long millis) {
        int glyphCount = text.codePointCount(0, text.length());
        int[] colors = new int[glyphCount];
        java.util.Arrays.fill(colors, -1);
        if (!containsPossibleName(text)) return colors;
        int[] codePoints = text.codePoints().toArray();
        for (int start = 0; start < codePoints.length; start++) {
            if (colors[start] >= 0) continue;
            for (String name : NAMES) {
                int[] target = name.codePoints().toArray();
                if (matches(codePoints, start, target) && boundary(codePoints, start - 1) && boundary(codePoints, start + target.length)) {
                    for (int offset = 0; offset < target.length; offset++) colors[start + offset] = waveColor(NICK_PALETTE, millis, offset);
                    start += target.length - 1;
                    break;
                }
            }
        }
        return colors;
    }

    private static boolean matches(int[] source, int start, int[] target) {
        if (start + target.length > source.length) return false;
        for (int i = 0; i < target.length; i++) if (Character.toLowerCase(source[start + i]) != Character.toLowerCase(target[i])) return false;
        return true;
    }

    private static boolean boundary(int[] text, int index) {
        if (index < 0 || index >= text.length) return true;
        int cp = text[index];
        return cp != '_' && !Character.isLetterOrDigit(cp);
    }

    private static boolean containsPossibleName(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("darkjota") || lower.contains("darkerjota");
    }

    private static int waveColor(int[] palette, long millis, int glyphOffset) {
        double phase = Math.floorMod(millis, CYCLE_MILLIS) / (double) CYCLE_MILLIS + glyphOffset * 0.105;
        double scaled = (phase - Math.floor(phase)) * palette.length;
        int segment = (int) Math.floor(scaled);
        double t = scaled - segment;
        t = (1.0 - Math.cos(Math.PI * t)) * 0.5;
        return lerp(palette[segment], palette[(segment + 1) % palette.length], t);
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static FormattedCharSequence sequence(String text, Style style) {
        return sink -> StringDecomposer.iterateFormatted(text, style, sink);
    }

    private record Glyph(int index, Style style, int codePoint) {}
    private static final class Scan implements net.minecraft.util.FormattedCharSink {
        private final StringBuilder plain = new StringBuilder();
        private boolean hasSupporterPrefix;
        @Override public boolean accept(int index, Style style, int codePoint) {
            plain.appendCodePoint(codePoint);
            hasSupporterPrefix |= SUPPORTER_PREFIX_MARKER.equals(style.getInsertion());
            return true;
        }
    }
}
