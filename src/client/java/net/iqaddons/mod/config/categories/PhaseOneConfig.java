package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.*;

@Category(
        value = "Phase 1 - Supplies"
)
public class PhaseOneConfig {

    @ConfigOption.Separator("Supply Widgets")

    @ConfigEntry(
            id = "supplyTimers",
            translation = "Supply Times"
    )
    @Comment("Show supply pickup times for each player.")
    public static boolean supplyTimers = true;

    @ConfigEntry(
            id = "supplyProgressDisplay",
            translation = "Supply Progress Display"
    )
    @Comment("Replace the default supply title with a movable widget.")
    public static boolean supplyProgressDisplay = true;

    @ConfigOption.Separator("Supply Waypoints")
    @ConfigEntry(
            id = "supplyWaypoints",
            translation = "Supply Waypoints"
    )
    @Comment("Show waypoints at supply locations.")
    public static boolean supplyWaypoints = true;

    @ConfigEntry(
            id = "supplyWaypointColor",
            translation = "Supply Waypoint Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of supply waypoints.")
    public static int supplyWaypointColor = new Color(0, 0, 0, 77).getRGB();

    @ConfigEntry(
            id = "supplyWaypointBoxSize",
            translation = "Supply Waypoint Box Size"
    )
    @ConfigOption.Range(min = 1, max = 3)
    @ConfigOption.Slider
    @Comment("Adjust the size of supply waypoint boxes.")
    public static float supplyWaypointBoxSize = 1.0f;

    @ConfigOption.Separator("Pearl Waypoints")
    @ConfigEntry(
            id = "pearlWaypoints",
            translation = "Pearl Waypoints"
    )
    @Comment("Show pearl throw waypoints during the supply phase.")
    public static boolean pearlWaypoints = true;

    @ConfigButton(
            title = "Make Your Own Waypoints",
            text = "OPEN"
    )
    @Comment("Customize or add pearl waypoints by editing the pearl_waypoints.json file.\nSave the file and run /iq reload to apply changes.")
    @SuppressWarnings("unused")
    public static final Runnable makeYourOwnPearls = () -> {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("iq");
            Files.createDirectories(configDir);
            Util.getOperatingSystem().open(configDir.toFile());
        } catch (Exception e) {
            // Silently fail
        }
    };


    @ConfigEntry(
            id = "pearlWaypointColor",
            translation = "Pearl Waypoint Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Color for pearl waypoints. This color is used for all waypoints unless custom colors are set per-waypoint in the JSON config.")
    public static int pearlWaypointColor = new Color(0, 255, 255, 110).getRGB();

    @ConfigEntry(
            id = "pearlWaypointSize",
            translation = "Pearl Waypoint Box Size Adjustment"
    )
    @ConfigOption.Range(min = -5, max = 5)
    @ConfigOption.Slider
    @Comment("Adjusts all JSON waypoint sizes by 10% per step. \n0 = no change (use JSON base sizes).")
    public static int pearlWaypointSize = 0;

    @ConfigEntry(
            id = "pearlWaypointsScale",
            translation = "Pearl Waypoint Text Scale"
    )
    @ConfigOption.Range(min = 0.01, max = 1)
    @ConfigOption.Slider
    @Comment("Adjust the size of the text displayed on pearl waypoints.")
    public static float pearlWaypointsScale = 0.3f;

    @ConfigEntry(
            id = "pearlWaypointsTimerDelay",
            translation = "Pearl Waypoints Timer Delay"
    )
    @ConfigOption.Range(min = -4, max = 4)
    @ConfigOption.Slider
    @Comment("Adjust the waypoint timer to match your ping, each step changes 1 progress tick. \n0 = 160-200ms with Kuudra's Heart (Tier 3 Talisman)")
    public static int pearlWaypointsTimerDelay = 0;

    @ConfigEntry(
            id = "pearlWaypointTimes",
            translation = "Pearl Waypoint Type"
    )
    @ConfigOption.Select
    @Comment("Choose what pearl waypoints display: timer in milliseconds, timer in seconds, or static text.")
    public static PearlWaypointType pearlWaypointTimes = PearlWaypointType.TIMER_MS;

    @ConfigEntry(
            id = "pearlWaypointRenderStyle",
            translation = "Pearl Waypoint Render Style"
    )
    @ConfigOption.Select
    @Comment("Choose how pearl waypoints are rendered: solid, outline, both, or none.")
    public static PearlWaypointRenderStyle pearlWaypointRenderStyle = PearlWaypointRenderStyle.SOLID;


    @ConfigEntry(
            id = "pearlThrowAlert",
            translation = "Pearl Throw Alert"
    )
    @Comment("Play a sound and highlight when it's time to throw a pearl.")
    public static boolean pearlThrowAlert = true;

    @ConfigOption.Separator("Dynamic Waypoints")

    @ConfigEntry(
            id = "dynamicPearlWaypoints",
            translation = "Dynamic Pearl Waypoints"
    )
    @Comment("Makes pearl waypoints move up or down based on your position so the marker stays more accurate while doing supplies.")
    public static boolean dynamicPearlWaypoints = true;

    @ConfigEntry(
            id = "dynamicPearlWaypointConfig",
            translation = "Dynamic Pearl Waypoint Config"
    )
    @Comment("Advanced tuning for Dynamic Pearl Waypoints. If you do not know what these values do, it is recommended to leave them unchanged.")
    public static final DynamicPearlWaypointSettings dynamicPearlWaypointConfig = new DynamicPearlWaypointSettings();

    @ConfigObject
    public static class DynamicPearlWaypointSettings {
        @ConfigEntry(
                id = "dynamicPearlWaypointOverride",
                translation = "Override Dynamic Pearl Values"
        )
        @Comment("Enables the custom values below. If this is disabled, IQ will use the recommended built-in values. If you do not know what you are doing, do not change these settings.")
        @SuppressWarnings("unused")
        public boolean dynamicPearlWaypointOverride = false;

        @ConfigEntry(
                id = "dynamicPearlWaypointYMultiplier",
                translation = "Dynamic Pearl Waypoint Y Multiplier"
        )
        @ConfigOption.Range(min = 0.0, max = 2.0)
        @ConfigOption.Slider
        @Comment("Controls how much your vertical movement changes the waypoint height. Recommended default: 0.81. Only used when Override Dynamic Pearl Values is enabled.")
        public double dynamicPearlWaypointYMultiplier = 0.81;

        @ConfigEntry(
                id = "dynamicPearlWaypointHeightAdjustmentFactor",
                translation = "Dynamic Pearl Waypoint Height Adjustment Factor"
        )
        @ConfigOption.Range(min = 0.0, max = 2.0)
        @ConfigOption.Slider
        @Comment("Controls how much forward and backward movement changes waypoint height. Recommended default: 0.31. Only used when Override Dynamic Pearl Values is enabled.")
        public double dynamicPearlWaypointHeightAdjustmentFactor = 0.31;

        @ConfigEntry(
                id = "dynamicPearlWaypointLeftRightAdjustmentFactor",
                translation = "Dynamic Pearl Waypoint Left Right Adjustment Factor"
        )
        @ConfigOption.Range(min = 0.0, max = 2.0)
        @ConfigOption.Slider
        @Comment("Controls how much left and right movement changes waypoint height. Recommended default: 0.63. Only used when Override Dynamic Pearl Values is enabled.")
        public double dynamicPearlWaypointLeftRightAdjustmentFactor = 0.63;
    }

    @ConfigOption.Separator("Pile Waypoints")

    @ConfigEntry(
            id = "pileWaypoints",
            translation = "Pile Waypoints"
    )
    @Comment("Show waypoints at crate pile locations.")
    public static boolean pileWaypoints = true;

    @ConfigEntry(
            id = "pileWaypointNames",
            translation = "Pile Waypoint Names"
    )
    @Comment("Show or hide pile name labels above waypoints.")
    public static boolean pileWaypointNames = true;

    @ConfigEntry(
            id = "normalPileColor",
            translation = "Normal Pile Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of standard supply pile waypoints.")
    public static int normalPileColor = new Color(255, 255, 255, 52).getRGB();

    @ConfigEntry(
            id = "noPrePileColor",
            translation = "No Pre Pile Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of no-pre supply pile waypoints.")
    public static int noPrePileColor = new Color(0, 255, 144, 50).getRGB();

    @ConfigOption.Separator("Supply Alerts")

    @ConfigEntry(
            id = "supplyRecoverMessage",
            translation = "Custom Supply Recover Message"
    )
    @Comment("Send a custom message when recover a supply.")
    public static boolean supplyRecoverMessage = true;

    @ConfigEntry(
            id = "noPreAlert",
            translation = "Send No Pre Alert"
    )
    @Comment("Send a party chat message if you don't have a pre.")
    public static boolean noPreAlert = true;

    @ConfigEntry(
            id = "secondSupplyAlert",
            translation = "Second Supply Alert"
    )
    @Comment("Announce the position of the second supply in chat.")
    public static boolean secondSupplyAlert = true;

    @ConfigEntry(
            id = "supplyGiantHitboxAlert",
            translation = "Supply Giant Hitbox Alert"
    )
    @Comment("Play a sound and highlight the giant in red when you recover supply inside its hitbox.")
    public static boolean supplyGiantHitboxAlert = true;

    @ConfigEntry(
            id = "supplyGiantHitboxStyle",
            translation = "Supply Giant Hitbox Style"
    )
    @ConfigOption.Select
    @Comment("Choose how the giant hitbox highlight is rendered.")
    public static WorldRenderUtils.RenderStyle supplyGiantHitboxStyle = WorldRenderUtils.RenderStyle.BOTH;

    public enum PearlWaypointType {
        TIMER_MS,
        TIMER_SECONDS,
        TEXT_STATIC,
    }

    public enum PearlWaypointRenderStyle {
        SOLID,
        OUTLINE,
        BOTH,
        NONE
    }
}