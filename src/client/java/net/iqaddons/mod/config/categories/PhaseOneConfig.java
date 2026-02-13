package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;

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
    @Comment("Display supply pickup times for all players")
    public static boolean supplyTimers = true;

    @ConfigEntry(
            id = "supplyProgressDisplay",
            translation = "Supply Progress Display"
    )
    @Comment("Hide supply progress title and render it as a movable widget")
    public static boolean supplyProgressDisplay = false;

    @ConfigOption.Separator("Supply Waypoints")
    @ConfigEntry(
            id = "supplyWaypoints",
            translation = "Supply Waypoints"
    )
    @Comment("Draw a beacon at supply locations")
    public static boolean supplyWaypoints = true;

    @ConfigEntry(
            id = "supplyWaypointColor",
            translation = "Supply Waypoint Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of the supply waypoints")
    public static int supplyWaypointColor = new Color(0, 0, 0, 77).getRGB();

    @ConfigEntry(
            id = "supplyWaypointBoxSize",
            translation = "Supply Waypoint Box Size"
    )
    @ConfigOption.Range(min = 1, max = 3)
    @ConfigOption.Slider
    @Comment("Adjust the size of supply waypoint boxes")
    public static float supplyWaypointBoxSize = 1.0f;

    @ConfigOption.Separator("Pearl Waypoints")

    @ConfigEntry(
            id = "pearlWaypoints",
            translation = "Pearl Waypoints"
    )
    @Comment("Show pearl throw waypoints during the supply phase")
    public static boolean pearlWaypoints = true;

    @ConfigEntry(
            id = "pearlWaypointsScale",
            translation = "Pearl Waypoint Text Scale"
    )
    @ConfigOption.Range(min = 0.01, max = 1)
    @ConfigOption.Slider
    @Comment("Scale the text on pearl throw waypoints")
    public static float pearlWaypointsScale = 0.25f;

    @ConfigEntry(
            id = "pearlWaypointsTimerDelay",
            translation = "Pearl Waypoints Timer Delay"
    )
    @ConfigOption.Range(min = -4, max = 4)
    @ConfigOption.Slider
    @Comment("Adjust the displayed supply tick percentage below pearl waypoints to match your ping (0 ~= 160-200ms)")
    public static int pearlWaypointsTimerDelay = 0;

    @ConfigOption.Separator("Pile Waypoints")

    @ConfigEntry(
            id = "pileWaypoints",
            translation = "Pile Waypoints"
    )
    @Comment("Display beacons at all crate pile locations")
    public static boolean pileWaypoints = true;

    @ConfigEntry(
            id = "normalPileColor",
            translation = "Normal Pile Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Customize the beacon color of standard supply piles.")
    public static int normalPileColor = new Color(255, 255, 255, 52).getRGB();

    @ConfigEntry(
            id = "noPrePileColor",
            translation = "No Pre Pile Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Customize the beacon color of no-pre supply pile (mainly useful for Square).")
    public static int noPrePileColor = new Color(0, 255, 144, 50).getRGB();

    @ConfigOption.Separator("Supply Alerts")

    @ConfigEntry(
            id = "supplyRecoverMessage",
            translation = "Custom Supply Recover Message"
    )
    @Comment("Send a custom message when recover a supply")
    public static boolean supplyRecoverMessage = true;

    @ConfigEntry(
            id = "noPreAlert",
            translation = "Send No Pre Alert"
    )
    @Comment("Send a message in party chat if you have no pre")
    public static boolean noPreAlert = true;

    @ConfigEntry(
            id = "secondSupplyAlert",
            translation = "Second Supply Alert "
    )
    @Comment("Announce the position of the second supply in chat")
    public static boolean secondSupplyAlert = true;

}