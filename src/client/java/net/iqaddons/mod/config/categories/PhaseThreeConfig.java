package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

import java.awt.*;

@Category(
        value = "Phase 3 - Stun"
)
public class PhaseThreeConfig {

    @ConfigEntry(
            id = "kuudraHPBossbar",
            translation = "Kuudra HP Bossbar"
    )
    @Comment("Show Kuudra's HP in a custom bossbar")
    public static boolean kuudraHPBossbar = true;

    @ConfigEntry(
            id = "kuudraHealthDisplay",
            translation = "Kuudra Health Display"
    )
    @Comment("Display Kuudra's health with health, percent & damage")
    public static boolean kuudraHealthDisplay = true;

    @ConfigEntry(
            id = "kuudraHitbox",
            translation = "Kuudra Hitbox"
    )
    @Comment("Render Kuudra's hitbox during the stun phase")
    public static boolean kuudraHitbox = true;

    @ConfigEntry(
            id = "kuudraHitboxColor",
            translation = "Kuudra Hitbox Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change Kuudra's hitbox color to something more visible")
    public static int kuudraHitboxColor = new Color(255, 2, 2, 231).getRGB();

    @ConfigEntry(
            id = "stunWaypoints",
            translation = "Stun Waypoints"
    )
    @Comment("Display waypoints for stun locations")
    public static boolean stunWaypoints = false;

    @ConfigEntry(
            id = "blockUselessPerks",
            translation = "Block Useless Perks"
    )
    @Comment("Prevent purchasing useless perks (W.I.P)")
    public static boolean blockUselessPerks = true;
}
