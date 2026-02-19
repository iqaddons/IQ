package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.features.kuudra.waypoints.StunWaypointsFeature;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

import java.awt.*;

@Category(
        value = "Phase 3 - Stun"
)
public class PhaseThreeConfig {

    @ConfigEntry(
            id = "blockUselessPerks",
            translation = "Block Useless Perks"
    )
    @Comment("Prevent buying perks that provide no benefit for your run.")
    public static boolean blockUselessPerks = true;

    @ConfigOption.Separator("Stun Widgets")

    @ConfigEntry(
            id = "kuudraHealth",
            translation = "Kuudra Health"
    )
    @Comment("Show Kuudra's real health inside the Magma Cube.")
    public static boolean kuudraHealth = true;

    @ConfigEntry(
            id = "kuudraHealthColorConfig",
            translation = "Kuudra Health Colors"
    )
    @Comment("Customize the colors used for each Kuudra health range.")
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
    @Comment("Show HP and percentage in the health widget.")
    public static boolean kuudraHealthDisplay = true;

    @ConfigEntry(
            id = "kuudraHitbox",
            translation = "Kuudra Hitbox Config"
    )
    @Comment("Configure Kuudra's hitbox overlay during the run.")
    public static final KuudraHitbox kuudraHitbox = new KuudraHitbox();

    @ConfigObject
    public static class KuudraHitbox {

        @ConfigEntry(
                id = "kuudraHitbox",
                translation = "Kuudra Hitbox"
        )
        @Comment("Show Kuudra's hitbox overlay.")
        public static boolean enabled = true;

        @ConfigEntry(
                id = "kuudraHitboxColor",
                translation = "Kuudra Hitbox Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change the hitbox color for better visibility.")
        public static int color = new Color(255, 2, 2, 231).getRGB();

        @ConfigEntry(
                id = "kuudraHitboxStyle",
                translation = "Kuudra Hitbox Style"
        )
        @ConfigOption.Select
        @Comment("Choose how the hitbox is drawn (outline, filled, or both).")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.OUTLINE;

    }

    @ConfigOption.Separator("Stun Waypoints")
    @ConfigEntry(
            id = "stunWaypoints",
            translation = "Stun Waypoints"
    )
    @Comment("Show waypoints for stun positions.")
    public static boolean stunWaypoints = true;

    @ConfigEntry(
            id = "stunWaypointColor",
            translation = "Stun Waypoints Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of stun waypoints.")
    public static int stunWaypointColor = new Color(0, 245, 255, 200).getRGB();

    @ConfigEntry(
            id = "stunWaypointStyle",
            translation = "Stun Waypoint Style"
    )
    @ConfigOption.Select
    @Comment("Choose how stun waypoints are drawn (outline, filled, or both).")
    public static WorldRenderUtils.RenderStyle stunWaypointStyle = WorldRenderUtils.RenderStyle.OUTLINE;

    @ConfigEntry(
            id = "stunWaypointBlock",
            translation = "Stun Waypoint Block"
    )
    @ConfigOption.Select
    @Comment("Choose which stun location marker to display.")
    public static StunWaypointsFeature.StunWaypoint stunWaypointBlock = StunWaypointsFeature.StunWaypoint.LEFT_POD;
}
