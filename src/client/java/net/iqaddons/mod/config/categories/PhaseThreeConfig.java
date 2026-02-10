package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

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
            translation = "Kuudra Hitbox Config"
    )
    @Comment("Configure Kuudra's hitbox rendering during the stun phase")
    public static final KuudraHitbox kuudraHitbox = new KuudraHitbox();

    @ConfigObject
    public static class KuudraHitbox {

        @ConfigEntry(
                id = "kuudraHitbox",
                translation = "Kuudra Hitbox"
        )
        @Comment("Render Kuudra's hitbox during the stun phase")
        public static boolean enabled = true;

        @ConfigEntry(
                id = "kuudraHitboxColor",
                translation = "Kuudra Hitbox Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change Kuudra's hitbox color to something more visible")
        public static int color = new Color(255, 2, 2, 231).getRGB();

        @ConfigEntry(
                id = "kuudraHitboxStyle",
                translation = "Kuudra Hitbox Style"
        )
        @ConfigOption.Select
        @Comment("Change the style of the Elle highlight")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.OUTLINE;
    }
    @ConfigEntry(
            id = "stunWaypoints",
            translation = "Stun Waypoints"
    )
    @Comment("Display waypoints for stun locations")
    public static boolean stunWaypoints = false;

    @ConfigEntry(
            id = "stunWaypointColor",
            translation = "Stun Waypoints Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of the stun waypoints")
    public static int color = new Color(0, 245, 255, 200).getRGB();

    @ConfigEntry(
            id = "blockUselessPerks",
            translation = "Block Useless Perks"
    )
    @Comment("Prevent purchasing useless perks")
    public static boolean blockUselessPerks = true;
}
