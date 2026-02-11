package net.iqaddons.mod;

import java.util.regex.Pattern;

public final class IQConstants {

    public static final String SKYBLOCK_AREA_ID = "SKYBLOCK";
    public static final String KUUDRA_AREA_ID = "Kuudra's Hollow";

    public static final int DEFAULT_CHECK_INTERVAL_TICKS = 20;

    public static final String FRESH_TOOLS_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";
    public static final Pattern SUPPLY_PATTERN = Pattern.compile("(.+) recovered one of Elle's supplies! \\((\\d)/6\\)");
    public static final Pattern SUPPLY_DROPPED_PATTERN = Pattern.compile("(.+) dropped one of Elle's supplies! \\((\\d)/6\\)");
    public static final Pattern PARTY_FRESH_PATTERN = Pattern.compile(
            "Party > (?:\\[[^]]+] )?(\\w+): (?:\\[IQ] )?FRESH\\b",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern PROTECT_ELLE_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");

    public static final String ELLE_HEAD_OVER_MESSAGE = "[NPC] Elle: Head over to the main platform";
    public static final String ELLE_NOT_AGAIN_MESSAGE = "[NPC] Elle: Not again!";
}
