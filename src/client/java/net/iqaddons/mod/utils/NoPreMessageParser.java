package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.iqaddons.mod.model.spot.PreSpot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class NoPreMessageParser {

    // More robust alias matching to avoid rare misses caused by punctuation, formatting or unicode
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final List<AliasRule> ALIAS_RULES;

    static {
        ALIASES.put("triangle", "TRIANGLE");
        ALIASES.put("tri", "TRIANGLE");

        ALIASES.put("x", "X");
        ALIASES.put("xc", "X CANNON");
        ALIASES.put("xcannon", "X CANNON");
        ALIASES.put("x cannon", "X CANNON");

        ALIASES.put("equals", "EQUALS");
        ALIASES.put("eq", "EQUALS");

        ALIASES.put("slash", "SLASH");

        ALIASES.put("shop", "SHOP");

        ALIASES.put("square", "SQUARE");

        List<AliasRule> rules = new ArrayList<>();
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            String alias = entry.getKey();
            Pattern pattern = Pattern.compile(
                    "\\b(?:no|missing)\\s+" + Pattern.quote(alias) + "\\b",
                    Pattern.CASE_INSENSITIVE
            );
            rules.add(new AliasRule(alias, entry.getValue(), pattern));
        }

        // Always prefer specific aliases first (e.g. "x cannon" before "x")
        rules.sort(Comparator.comparingInt((AliasRule rule) -> rule.alias().length()).reversed());
        ALIAS_RULES = List.copyOf(rules);
    }

    public @Nullable ParsedNoPreCall parse(@NotNull String message) {
        if (message == null || message.isBlank()) return null;

        // normalize: remove any leftover formatting chars and non-alphanum (keep spaces)
        String normalized = message.toLowerCase(Locale.ROOT)
                .replaceAll("§.", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        for (AliasRule rule : ALIAS_RULES) {
            // look for "no <alias>" or "missing <alias>"
            Matcher m = rule.pattern().matcher(normalized);
            if (m.find()) {
                int missingPreValue = PreSpot.getMissingPreValueFromPileName(rule.canonical());
                if (missingPreValue > 0) {
                    return new ParsedNoPreCall(missingPreValue, rule.canonical());
                }
            }
        }

        return null;
    }

    public record ParsedNoPreCall(int missingPreValue, @NotNull String canonicalPileName) {
    }

    private record AliasRule(@NotNull String alias, @NotNull String canonical, @NotNull Pattern pattern) {
    }
}
