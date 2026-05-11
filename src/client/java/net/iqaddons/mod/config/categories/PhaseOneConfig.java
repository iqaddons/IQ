package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.utils.TextColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.util.Util;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Category(
        value = "Phase 1 - Supplies"
)
public class PhaseOneConfig {

    @ConfigOption.Separator("Supply Widgets")

    @ConfigEntry(
            id = "supplyTimers",
            translation = "Supply Times"
    )
    @Comment("Display an overlay that tracks all supply pickup times.")
    public static boolean supplyTimers = true;

    @ConfigEntry(
            id = "supplyTimesTitleColor",
            translation = "Supply Times Title Color"
    )
    @ConfigOption.Select
    @Comment("Choose the color used for the Supply Times title.")
    public static TextColor supplyTimesTitleColor = TextColor.AQUA;

    @ConfigEntry(
            id = "supplyTimerCountdown",
            translation = "Supply Timer Countdown"
    )
    @Comment("Show a countdown in Kuudra Splits displaying how long remains until supplies spawn.")
    public static boolean supplyTimerCountdown = true;

    @ConfigEntry(
            id = "supplyProgressDisplay",
            translation = "Supply Progress Display"
    )
    @Comment("Replace the default supply title with a movable widget.")
    public static boolean supplyProgressDisplay = true;

    @ConfigOption.Separator("Crate Priority")

    @ConfigEntry(
            id = "cratePriority",
            translation = "Crate Priority (P2)"
    )
    @Comment("Alerts you where to go after picking up your first supply. \nPriority can be edited in crate_priority.json config.")
    public static boolean cratePriority = true;

    @ConfigEntry(
            id = "cratePriorityConfig",
            translation = "Crate Priority Config"
    )
    @Comment("Customize title color, duration, animation, and sound for Crate Priority.")
    public static final CratePriorityConfig cratePriorityConfig = new CratePriorityConfig();

    @ConfigOption.Separator("Supply Waypoints")
    @ConfigEntry(
            id = "supplyWaypoints",
            translation = "Supply Waypoints"
    )
    @Comment("Show waypoints at supply locations.")
    public static boolean supplyWaypoints = true;

    @ConfigEntry(
            id = "supplyWaypointsConfig",
            translation = "Supply Waypoints Config"
    )
    @Comment("Customize rendering and helper behavior for Supply Waypoints.")
    public static final SupplyWaypointsConfig supplyWaypointsConfig = new SupplyWaypointsConfig();

    @ConfigOption.Separator("Pearl Waypoints")
    @ConfigEntry(
            id = "pearlWaypoints",
            translation = "Pearl Waypoints"
    )
    @Comment("Show pearl throw waypoints during the supply phase.")
    public static boolean pearlWaypoints = true;

    @ConfigEntry(
            id = "pearlWaypointsConfig",
            translation = "Pearl Waypoints Config"
    )
    @Comment("Customize rendering, timing, and helper behavior for Pearl Waypoints.")
    public static final PearlWaypointsConfig pearlWaypointsConfig = new PearlWaypointsConfig();


    @ConfigOption.Separator("Pile Waypoints")

    @ConfigEntry(
            id = "pileWaypoints",
            translation = "Pile Waypoints"
    )
    @Comment("Show waypoints at crate pile locations.")
    public static boolean pileWaypoints = true;

    @ConfigEntry(
            id = "pileWaypointsConfig",
            translation = "Pile Waypoints Config"
    )
    @Comment("Customize labels, colors, and file editing for Pile Waypoints.")
    public static final PileWaypointsConfig pileWaypointsConfig = new PileWaypointsConfig();

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

    @ConfigObject
    public static class CratePriorityConfig {
        @ConfigEntry(id = "cratePriorityColor", translation = "Crate Priority Title Color")
        @ConfigOption.Select
        @Comment("Choose the text color used by the Crate Priority title widget.")
        public static TextColor cratePriorityColor = TextColor.GREEN;

        @ConfigEntry(id = "cratePriorityDurationSeconds", translation = "Crate Priority Duration")
        @ConfigOption.Range(min = 1, max = 10)
        @ConfigOption.Slider
        @Comment("How long the Crate Priority title stays on screen (seconds).")
        public static int cratePriorityDurationSeconds = 6;

        @ConfigEntry(id = "cratePriorityAnimation", translation = "Crate Priority Animation")
        @ConfigOption.Select
        @Comment("Choose the animation style for the Crate Priority title widget.")
        public static CratePriorityAnimation cratePriorityAnimation = CratePriorityAnimation.FADE;

        @ConfigEntry(id = "cratePrioritySound", translation = "Crate Priority Sound")
        @Comment("Play a sound when a new Crate Priority action is shown.")
        public static boolean cratePrioritySound = true;
    }

    @ConfigObject
    public static class SupplyWaypointsConfig {
        @ConfigEntry(id = "supplyPullCircle", translation = "Supply Pull Circle")
        @Comment("Render a circle in area that you can pull the supply.")
        public static boolean supplyPullCircle = true;

        @ConfigEntry(id = "supplyWaypointColor", translation = "Supply Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of supply waypoints.")
        public static int supplyWaypointColor = new Color(0, 255, 255, 40).getRGB();

        @ConfigEntry(id = "supplyWaypointStyle", translation = "Supply Hitbox Style")
        @ConfigOption.Select
        @Comment("Choose if supply hitboxes are outlined, filled, or both.")
        public static WorldRenderUtils.RenderStyle supplyWaypointStyle = WorldRenderUtils.RenderStyle.BOTH;

        @ConfigEntry(id = "supplyWaypointBoxSize", translation = "Supply Waypoint Box Size")
        @ConfigOption.Range(min = 1, max = 3)
        @ConfigOption.Slider
        @Comment("Adjust the size of supply waypoint boxes.")
        public static float supplyWaypointBoxSize = 1.0f;

        @ConfigEntry(id = "supplyHitBox", translation = "Supply Interaction Box")
        @Comment("Displays the interaction points where you can pick up the supply.")
        public static boolean supplyHitBox = true;
    }

    @ConfigObject
    public static class PearlWaypointsConfig {
        @ConfigButton(title = "Make Your Own Pearl Waypoints", text = "OPEN")
        @Comment("Customize or add pearl waypoints by editing the pearl_waypoints.json file.\nSave the file and run /iq reload to apply changes.\nDelete the file to restore default waypoints.")
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

        @ConfigEntry(id = "pearlWaypointColor", translation = "Pearl Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color for pearl waypoints. This color is used for all waypoints unless custom colors are set per-waypoint in the JSON config.")
        public static int pearlWaypointColor = new Color(0, 255, 255, 110).getRGB();

        @ConfigEntry(id = "pearlWaypointSize", translation = "Pearl Waypoint Box Size Adjustment")
        @ConfigOption.Range(min = -5, max = 5)
        @ConfigOption.Slider
        @Comment("Adjusts all JSON waypoint sizes by 10% per step. \n0 = no change (use JSON base sizes).")
        public static int pearlWaypointSize = -3;

        @ConfigEntry(id = "pearlWaypointsScale", translation = "Pearl Waypoint Text Scale")
        @ConfigOption.Range(min = 0.01, max = 1)
        @ConfigOption.Slider
        @Comment("Adjust the size of the text displayed on pearl waypoints.")
        public static float pearlWaypointsScale = 0.15f;

        @ConfigEntry(id = "pearlWaypointsTimerDelay", translation = "Pearl Waypoints Timer Delay")
        @ConfigOption.Range(min = -4, max = 4)
        @ConfigOption.Slider
        @Comment("Adjust the waypoint timer to match your ping, each step changes 1 progress tick. \n0 = 160-200ms with Kuudra's Heart (Tier 3 Talisman)")
        public static int pearlWaypointsTimerDelay = 0;

        @ConfigEntry(id = "pearlWaypointTimes", translation = "Pearl Waypoint Type")
        @ConfigOption.Select
        @Comment("Choose what pearl waypoints display: timer in milliseconds, timer in seconds, or static text.")
        public static PearlWaypointType pearlWaypointTimes = PearlWaypointType.TIMER_SECONDS;

        @ConfigEntry(id = "pearlWaypointRenderStyle", translation = "Pearl Waypoint Render Style")
        @ConfigOption.Select
        @Comment("Choose how pearl waypoints are rendered: solid, outline, both, or none.")
        public static PearlWaypointRenderStyle pearlWaypointRenderStyle = PearlWaypointRenderStyle.SOLID;

        @ConfigEntry(id = "pearlWaypointBlockOutlines", translation = "Pearl Waypoint Stand Block")
        @Comment("Toggle the outlined block you should stand on when throwing pearls.")
        public static boolean pearlWaypointBlockOutlines = true;

        @ConfigEntry(id = "pearlThrowAlert", translation = "Pearl Throw Alert")
        @Comment("Play a sound and highlight when it's time to throw a pearl.")
        public static boolean pearlThrowAlert = true;

        @ConfigEntry(id = "dynamicPearlWaypoints", translation = "Dynamic Pearl Waypoints")
        @Comment("Makes pearl waypoints move up or down based on your position so the marker stays more accurate while doing supplies.")
        public static boolean dynamicPearlWaypoints = true;
    }

    @ConfigObject
    public static class PileWaypointsConfig {
        @ConfigButton(title = "Edit Pile Locations", text = "OPEN")
        @Comment("Customize pile waypoints by editing the pile_locations.json file.\nSave the file and run /iq reload to apply changes.")
        @SuppressWarnings("unused")
        public static final Runnable editPileLocations = () -> {
            try {
                Path configDir = FabricLoader.getInstance().getConfigDir().resolve("iq");
                Files.createDirectories(configDir);
                Util.getOperatingSystem().open(configDir.toFile());
            } catch (Exception e) {
                // Silently fail
            }
        };

        @ConfigEntry(id = "pileWaypointNames", translation = "Pile Waypoint Names")
        @Comment("Show or hide pile name labels above waypoints.")
        public static boolean pileWaypointNames = true;

        @ConfigEntry(id = "normalPileColor", translation = "Normal Pile Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of standard supply pile waypoints.")
        public static int normalPileColor = new Color(255, 255, 255, 52).getRGB();

        @ConfigEntry(id = "noPrePileColor", translation = "No Pre Pile Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of no-pre supply pile waypoints.")
        public static int noPrePileColor = new Color(0, 255, 144, 50).getRGB();
    }

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

    public enum CratePriorityAnimation {
        NONE,
        FADE,
        SLIDE
    }
}