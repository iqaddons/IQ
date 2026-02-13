package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.features.kuudra.waypoints.StunWaypointsFeature;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

import java.awt.*;

@Category(
        value = "Phase 3 - Stun"
)
public class PhaseThreeConfig {

    @ConfigOption.Separator("Stun Widgets")

    @ConfigEntry(
            id = "kuudraHealth",
            translation = "Kuudra Health"
    )
    @Comment("Render Kuudra's health inside the Magma Cube")
    public static boolean kuudraHealth = true;

    @ConfigEntry(
            id = "kuudraHealthColorConfig",
            translation = "Kuudra Health Colors"
    )
    @Comment("Choose colors for each health range in the Kuudra Health Feature")
    public static final KuudraHealthColorConfig kuudraHealthColorConfig = new KuudraHealthColorConfig();

    @ConfigObject
    public static class KuudraHealthColorConfig {
        @ConfigEntry(id = "high", translation = "75%-100% Color")
        @ConfigOption.Color(alpha = true)
        public static int high = new Color(85, 255, 85, 255).getRGB();

        @ConfigEntry(id = "mid", translation = "50%-75% Color")
        @ConfigOption.Color(alpha = true)
        public static int mid = new Color(255, 255, 85, 255).getRGB();

        @ConfigEntry(id = "low", translation = "25-50% Color")
        @ConfigOption.Color(alpha = true)
        public static int low = new Color(255, 170, 0, 255).getRGB();

        @ConfigEntry(id = "critical", translation = "0-25% Color")
        @ConfigOption.Color(alpha = true)
        public static int critical = new Color(255, 85, 85, 255).getRGB();
    }

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
        @Comment("Change the style of the Kuudra hitbox")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.OUTLINE;
    }

    @ConfigEntry(
            id = "blockUselessPerks",
            translation = "Block Useless Perks"
    )
    @Comment("Prevent purchasing useless perks")
    public static boolean blockUselessPerks = true;

    @ConfigOption.Separator("Stun Waypoints")
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
    public static int stunWaypointColor = new Color(0, 245, 255, 200).getRGB();

    @ConfigEntry(
            id = "stunWaypointBlock",
            translation = "Stun Waypoint Block"
    )
    @ConfigOption.Select
    @Comment("Change the block used for stun waypoints")
    public static StunWaypointsFeature.StunWaypoint stunWaypointBlock = StunWaypointsFeature.StunWaypoint.BLOCK_2;
}
