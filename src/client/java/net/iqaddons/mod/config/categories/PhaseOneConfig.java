package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
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
    @Comment("Customize or add pearl waypoints by editing the pearl_waypoints.json file." +
             "Save the file and run /iq reload to apply changes.")
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
            translation = "Pearl Waypoint Box Size"
    )
    @ConfigOption.Range(min = 0.2, max = 5)
    @ConfigOption.Slider
    @Comment("Global size for the pearl waypoint box (supply marker).")
    public static float pearlWaypointSize = 1.0f;

    @ConfigEntry(
            id = "useGlobalPearlWaypointSize",
            translation = "Use Global Pearl Waypoint Box Size"
    )
    @Comment("When enabled, all pearl waypoints will use the global 'Pearl Waypoint Box Size' value." +
            "When disabled, each waypoint will use its individual size from the JSON config file.")
    public static boolean useGlobalPearlWaypointSize = false;

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
    @Comment("Adjust the waypoint timer to match your ping (0 ≈ 160–200ms with Kuudra Heart Talisman).")
    public static int pearlWaypointsTimerDelay = 0;

    @ConfigEntry(
            id = "pearlWaypointTimes",
            translation = "Pearl Waypoint Type"
    )
    @ConfigOption.Select
    @Comment("Choose what pearl waypoints display: text, timer, or both.")
    public static PearlWaypointType pearlWaypointTimes = PearlWaypointType.TIMER;

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
    @Comment("Make pearl waypoint height follow your player movement.")
    public static boolean dynamicPearlWaypoints = true;

    @ConfigEntry(
            id = "dynamicPearlWaypointYMultiplier",
            translation = "Dynamic Pearl Waypoint Y Multiplier"
    )
    @ConfigOption.Range(min = 0.0, max = 10.0)
    @ConfigOption.Slider
    @Comment("Multiplier for the player's vertical movement when adjusting waypoint height.")
    public static double dynamicPearlWaypointYMultiplier = 1.0;

    @ConfigEntry(
            id = "dynamicPearlWaypointHeightAdjustmentFactor",
            translation = "Dynamic Pearl Waypoint Height Adjustment Factor"
    )
    @ConfigOption.Range(min = 0.0, max = 10.0)
    @ConfigOption.Slider
    @Comment("Extra height adjustment based on your forward/backward movement.")
    public static double dynamicPearlWaypointHeightAdjustmentFactor = 1.0;

    @ConfigEntry(
            id = "dynamicPearlWaypointLeftRightAdjustmentFactor",
            translation = "Dynamic Pearl Waypoint Left Right Adjustment Factor"
    )
    @ConfigOption.Range(min = 0.0, max = 10.0)
    @ConfigOption.Slider
    @Comment("Extra height adjustment based on your left/right movement.")
    public static double dynamicPearlWaypointLeftRightAdjustmentFactor = 1.0;

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
            translation = "Second Supply Alert "
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
        TEXT,
        TIMER,
        BOTH
    }

    public enum PearlWaypointRenderStyle {
        SOLID,
        OUTLINE,
        BOTH,
        NONE
    }
}