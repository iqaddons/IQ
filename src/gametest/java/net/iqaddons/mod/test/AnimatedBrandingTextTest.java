package net.iqaddons.mod.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.render.text.AnimatedBrandingText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class AnimatedBrandingTextTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        Style original = Style.EMPTY.withBold(true).withItalic(true).withInsertion("metadata");
        List<Glyph> transformed = capture(AnimatedBrandingText.transform(sequence("Hi DarkJota! darkerjotaX DarkerJota.", original), 0L, false));
        assertColored(transformed, 3, 8);
        assertNotColored(transformed, 13, 11);
        assertColored(transformed, 25, 10);
        Style recolored = transformed.get(3).style();
        if (!recolored.isBold() || !recolored.isItalic() || !"metadata".equals(recolored.getInsertion())) throw new AssertionError("Style metadata lost");
        List<Glyph> later = capture(AnimatedBrandingText.transform(sequence("DarkJota", original), 600L, false));
        if (later.getFirst().color() == transformed.get(3).color()) throw new AssertionError("Nick wave did not animate");
        var prefix = AnimatedBrandingText.supporterPrefix().getVisualOrderText();
        List<Glyph> supporter = capture(AnimatedBrandingText.transform(prefix, 0L, true));
        List<Glyph> modrinth = capture(AnimatedBrandingText.transform(prefix, 0L, false));
        if (supporter.getFirst().color() == modrinth.getFirst().color()) throw new AssertionError("Supporter prefix not exclusive");
        List<Glyph> arbitrary = capture(AnimatedBrandingText.transform(sequence("[IQ] hello", Style.EMPTY), 0L, true));
        if (arbitrary.getFirst().color() != -1) throw new AssertionError("Unmarked prefix recolored");
        List<Glyph> legacy = capture(AnimatedBrandingText.transform("§cDarkJota", 0L, false));
        if (legacy.size() != 8 || legacy.stream().anyMatch(g -> g.codePoint() == '§')) throw new AssertionError("Legacy formatting was rendered as text");
        String emojiText = "😀 DarkJota";
        String joined = AnimatedBrandingText.nickRuns(emojiText, 0L).stream().map(AnimatedBrandingText.NickRun::text).reduce("", String::concat);
        if (!emojiText.equals(joined)) throw new AssertionError("NanoVG runs corrupted supplementary Unicode");
        context.setScreen(() -> null);
    }

    private static FormattedCharSequence sequence(String text, Style style) {
        return sink -> { int[] index = {0}; return text.codePoints().allMatch(cp -> sink.accept(index[0]++, style, cp)); };
    }
    private static List<Glyph> capture(FormattedCharSequence sequence) {
        List<Glyph> result = new ArrayList<>();
        sequence.accept((index, style, cp) -> { result.add(new Glyph(cp, style)); return true; });
        return result;
    }
    private static void assertColored(List<Glyph> glyphs, int start, int length) {
        for (int i = start; i < start + length; i++) if (glyphs.get(i).color() == -1) throw new AssertionError("Expected color at " + i);
    }
    private static void assertNotColored(List<Glyph> glyphs, int start, int length) {
        for (int i = start; i < start + length; i++) if (glyphs.get(i).color() != -1) throw new AssertionError("Unexpected color at " + i);
    }
    private record Glyph(int codePoint, Style style) { int color() { return style.getColor() == null ? -1 : style.getColor().getValue(); } }
}
