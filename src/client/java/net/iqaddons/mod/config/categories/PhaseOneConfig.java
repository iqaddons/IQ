package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

import java.awt.*;

@Category(
        value = "Phase 1 - Supplies"
)
public class PhaseOneConfig {

    @ConfigEntry(
            id = "supplyTimers",
            translation = "Supply Times"
    )
    @Comment("Display supply pickup times for all players")
    public static boolean supplyTimers = true;

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
            id = "pearlWaypoints",
            translation = "Pearl Waypoints"
    )
    @Comment("Show pearl throw waypoints during the supply phase")
    public static boolean pearlWaypoints = true;

    @ConfigEntry(
            id = "pileWaypoints",
            translation = "Pile Waypoints"
    )
    @Comment("Display beacons at all crate pile locations")
    public static boolean pileWaypoints = true;

    @ConfigEntry(
            id = "supplyPickingAlert",
            translation = "Supply Already Picking Alert "
    )
    @Comment("Alert when another player is already picking your supply")
    public static boolean supplyPickingAlert = true;

    @ConfigEntry(
            id = "noPreAlert",
            translation = "No Pre Alert"
    )
    @Comment("Send a chat message if you have no pre")
    public static boolean noPreAlert = true;

    @ConfigEntry(
            id = "secondSupplyAlert",
            translation = "Second Supply Alert "
    )
    @Comment("Announce the position of the second supply in chat")
    public static boolean secondSupplyAlert = true;

    @ConfigEntry(
            id = "supplyRecoverMessage",
            translation = "Custom Supply Recover Message"
    )
    @Comment("Send a custom message when recover a supply")
    public static boolean supplyRecoverMessage = true;

}