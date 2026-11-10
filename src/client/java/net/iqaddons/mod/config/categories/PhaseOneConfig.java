package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.config.screen.KuudraWaypointsScreen;
import net.iqaddons.mod.model.pearl.PearlTalismanTier;
import net.iqaddons.mod.utils.TextColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;

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
    @Comment("Display an overlay that tracks all supply pickup times")
    public static boolean supplyTimers = true;

    @ConfigEntry(
            id = "supplyTimesConfig",
            translation = "Supply Times Config"
    )
    @Comment("Customize the Supply Times overlay title and countdown")
    public static final SupplyTimesConfig supplyTimesConfig = new SupplyTimesConfig();

    @ConfigEntry(
            id = "supplyProgressDisplay",
            translation = "Supply Progress Display"
    )
    @Comment("Replace the default supply title with a movable widget")
    public static boolean supplyProgressDisplay = true;


    @ConfigOption.Separator("Pearl Waypoints")
    @ConfigEntry(
            id = "pearlWaypoints",
            translation = "Pearl Waypoints"
    )
    @Comment("Show pearl throw waypoints during the supply phase")
    public static boolean pearlWaypoints = true;

    @ConfigEntry(
            id = "pearlWaypointsConfig",
            translation = "Pearl Waypoints Config"
    )
    @Comment("Customize rendering, timing, and helper behavior for Pearl Waypoints")
    public static final PearlWaypointsConfig pearlWaypointsConfig = new PearlWaypointsConfig();

    @ConfigObject
    public static class PearlWaypointsConfig {

        @ConfigButton(title = "Edit Waypoints", text = "OPEN")
        @Comment("Open the Kuudra Waypoints editor for Pearl, Etherwarp, DPS and SKIP waypoints")
        @SuppressWarnings("unused")
        public static final Runnable editWaypoints = () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.gui.setScreen(new KuudraWaypointsScreen(mc.gui.screen())));
        };

        @ConfigEntry(id = "pearlWaypointColor", translation = "Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color for pearl waypoints")
        public static int pearlWaypointColor = new Color(0, 255, 255, 255).getRGB();

        @ConfigEntry(id = "pearlWaypointSize", translation = "Waypoint Size")
        @ConfigOption.Range(min = -5, max = 5)
        @ConfigOption.Slider
        @Comment("Adjust pearl waypoint marker size")
        public static int pearlWaypointSize = 0;

        @ConfigEntry(id = "pearlWaypointsScale", translation = "Text Scale")
        @ConfigOption.Range(min = 0.02, max = 0.12)
        @ConfigOption.Slider
        @Comment("Adjust the size of the text displayed on pearl waypoints")
        public static float pearlWaypointsScale = 0.08f;

        @ConfigEntry(id = "pearlWaypointPingMs", translation = "Average Ping")
        @ConfigOption.Range(min = 0, max = 500)
        @ConfigOption.Slider
        @Comment("Select your average ping used by trajectory pearl timers\nLower ping = slower Higher ping = faster")
        public static int pearlWaypointPingMs = 100;

        @ConfigEntry(id = "pearlWaypointTalismanTier", translation = "Talisman Tier")
        @ConfigOption.Select
        @Comment("Select your Kuudra Talisman tier used for trajectory pearl timers")
        public static PearlTalismanTier pearlWaypointTalismanTier = PearlTalismanTier.TIER_3;

        @ConfigEntry(id = "pearlWaypointTimes", translation = "Waypoint Timer Type")
        @ConfigOption.Select
        @Comment("Choose what pearl waypoints display: timer in milliseconds, seconds, or ticks")
        public static PearlWaypointType pearlWaypointTimes = PearlWaypointType.TIMER_SECONDS;

        @ConfigEntry(id = "pearlWaypointTextPosition", translation = "Timer Position")
        @ConfigOption.Select
        @Comment("Choose whether the pearl timer is rendered above or below the waypoint")
        public static PearlWaypointTextPosition pearlWaypointTextPosition = PearlWaypointTextPosition.BELOW;

        @ConfigEntry(id = "pearlWaypointRenderStyle", translation = "Render Style")
        @ConfigOption.Select
        @Comment("Choose how pearl waypoints are rendered")
        public static PearlWaypointRenderStyle pearlWaypointRenderStyle = PearlWaypointRenderStyle.SQUARE;

        @ConfigEntry(id = "pearlWaypointBlockOutlines", translation = "Stand Blocks")
        @Comment("Toggle the outlined blocks showing good positions in supplies phase")
        public static boolean pearlWaypointBlockOutlines = true;

        @ConfigEntry(id = "pearlThrowAlert", translation = "Throw Alert")
        @Comment("Play a sound and make waypoint green when it's time to throw the pearl")
        public static boolean pearlThrowAlert = true;

        @ConfigEntry(id = "pearlWaypointAreaDebug", translation = "Area Debug")
        @Comment("Highlight Pearl Waypoint area bounds for tuning and debugging")
        public static boolean pearlWaypointAreaDebug = false;

        @ConfigEntry(id = "pearlWaypointOffsets", translation = "Waypoint Offsets")
        @Comment("Show additional controls for adjusting Flat Pearl waypoint offsets")
        public static boolean pearlWaypointOffsets = false;

        @ConfigEntry(id = "pearlFlatXAreaYOffset", translation = "Flat Pearl X Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the X area. 100 = 1 block")
        public static int pearlFlatXAreaYOffset = 0;

        @ConfigEntry(id = "pearlFlatEqualsAreaYOffset", translation = "Flat Pearl Equals Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the Equals area. 100 = 1 block")
        public static int pearlFlatEqualsAreaYOffset = 0;

        @ConfigEntry(id = "pearlFlatSlashAreaYOffset", translation = "Flat Pearl Slash Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the Slash area. 100 = 1 block")
        public static int pearlFlatSlashAreaYOffset = 0;

        @ConfigEntry(id = "pearlFlatTriangleAreaYOffset", translation = "Flat Pearl Triangle Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the Triangle area. 100 = 1 block")
        public static int pearlFlatTriangleAreaYOffset = 0;

        @ConfigEntry(id = "pearlFlatSquareAreaYOffset", translation = "Flat Pearl Square Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the Square area. 100 = 1 block")
        public static int pearlFlatSquareAreaYOffset = 0;

        @ConfigEntry(id = "pearlFlatShopAreaYOffset", translation = "Flat Pearl Shop Y Offset")
        @ConfigOption.Range(min = -100, max = 100)
        @ConfigOption.Slider
        @Comment("Vertical offset for Flat Pearl waypoints in the Shop area. 100 = 1 block")
        public static int pearlFlatShopAreaYOffset = 0;

    }


    @ConfigOption.Separator("Crate Priority")

    @ConfigEntry(
            id = "cratePriority",
            translation = "Crate Priority (P2)"
    )
    @Comment("Alerts you where to go after picking up your first supply. \nPriority can be edited in crate_priority.json config")
    public static boolean cratePriority = true;

    @ConfigEntry(
            id = "cratePriorityConfig",
            translation = "Crate Priority Config"
    )
    @Comment("Customize title color, duration, animation, and sound for Crate Priority")
    public static final CratePriorityConfig cratePriorityConfig = new CratePriorityConfig();

    @ConfigOption.Separator("Supply Waypoints")
    @ConfigEntry(
            id = "supplyWaypoints",
            translation = "Supply Waypoints"
    )
    @Comment("Show waypoints at supply locations")
    public static boolean supplyWaypoints = true;

    @ConfigEntry(
            id = "supplyWaypointsConfig",
            translation = "Supply Waypoints Config"
    )
    @Comment("Customize rendering and helper behavior for Supply Waypoints")
    public static final SupplyWaypointsConfig supplyWaypointsConfig = new SupplyWaypointsConfig();

    @ConfigOption.Separator("Etherwarp Waypoints")

    @ConfigEntry(
            id = "etherwarpWaypoints",
            translation = "Etherwarp Waypoints"
    )
    @Comment("Show dynamic etherwarp target waypoints during Supplies based on your current pre spot")
    public static boolean etherwarpWaypoints = true;

    @ConfigEntry(
            id = "etherwarpWaypointsConfig",
            translation = "Etherwarp Waypoints Config"
    )
    @Comment("Customize Supplies etherwarp targets in the Kuudra Waypoints editor")
    public static final EtherwarpWaypointsConfig etherwarpWaypointsConfig = new EtherwarpWaypointsConfig();

    @ConfigObject
    public static class EtherwarpWaypointsConfig {
        @ConfigButton(title = "Edit Waypoints", text = "OPEN")
        @Comment("Open the Kuudra Waypoints editor")
        @SuppressWarnings("unused")
        public static final Runnable editWaypoints = () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.gui.setScreen(new KuudraWaypointsScreen(mc.gui.screen())));
        };

        @ConfigEntry(id = "etherwarpWaypointRenderStyle", translation = "Render Style")
        @ConfigOption.Select
        @Comment("Choose how Supplies etherwarp waypoints are rendered. TOP draws a thin outline on the top face of the target block.")
        public static EtherwarpWaypointRenderStyle etherwarpWaypointRenderStyle = EtherwarpWaypointRenderStyle.TOP;

        @ConfigEntry(id = "etherwarpWaypointColor", translation = "Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Default color used by Supplies etherwarp waypoints when Dynamic Render is not selecting a target")
        public static int etherwarpWaypointColor = new Color(0, 255, 255, 180).getRGB();

        @ConfigEntry(id = "etherwarpWaypointText", translation = "Waypoint Text")
        @Comment("Show the waypoint label above Supplies etherwarp waypoints")
        public static boolean etherwarpWaypointText = true;

        @ConfigEntry(id = "etherwarpWaypointTextPosition", translation = "Text Position")
        @ConfigOption.Select
        @Comment("Choose whether the Supplies etherwarp waypoint label is rendered above or below the waypoint")
        public static EtherwarpWaypointTextPosition etherwarpWaypointTextPosition = EtherwarpWaypointTextPosition.BELOW;

        @ConfigEntry(id = "etherwarpWaypointTextScale", translation = "Waypoint Text Scale")
        @ConfigOption.Range(min = 0.02, max = 0.12)
        @ConfigOption.Slider
        @Comment("Adjust the size of the waypoint label text")
        public static float etherwarpWaypointTextScale = 0.06083702f;

        @ConfigEntry(id = "etherwarpWaypointTextColor", translation = "Waypoint Text Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used by the waypoint label text")
        public static int etherwarpWaypointTextColor = new Color(0, 255, 255, 255).getRGB();

        @ConfigEntry(id = "etherwarpWaypointDynamicRender", translation = "Dynamic Render")
        @Comment("Use Crate Priority's current GO target to color matching etherwarp waypoints as active and the others as inactive")
        public static boolean etherwarpWaypointDynamicRender = true;

        @ConfigEntry(id = "etherwarpWaypointDynamicActiveColor", translation = "Dynamic Active Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for waypoints matching the current Crate Priority target")
        public static int etherwarpWaypointDynamicActiveColor = new Color(85, 255, 85, 220).getRGB();

        @ConfigEntry(id = "etherwarpWaypointDynamicInactiveColor", translation = "Dynamic Inactive Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for etherwarp waypoints that do not match the current Crate Priority GO target")
        public static int etherwarpWaypointDynamicInactiveColor = new Color(255, 85, 85, 150).getRGB();

        @ConfigEntry(id = "etherwarpWaypointTracer", translation = "Waypoint Tracer")
        @Comment("Render a tracer line to each visible Supplies etherwarp waypoint using the same color as Dynamic Render")
        public static boolean etherwarpWaypointTracer = false;
    }

    @ConfigOption.Separator("Pile Waypoints")

    @ConfigEntry(
            id = "pileWaypoints",
            translation = "Pile Waypoints"
    )
    @Comment("Show waypoints at crate pile locations")
    public static boolean pileWaypoints = true;

    @ConfigEntry(
            id = "pileWaypointsConfig",
            translation = "Pile Waypoints Config"
    )
    @Comment("Customize labels and colors for Pile Waypoints")
    public static final PileWaypointsConfig pileWaypointsConfig = new PileWaypointsConfig();

    @ConfigOption.Separator("Supply Alerts")

    @ConfigEntry(
            id = "supplyPlacementEfficiency",
            translation = "Supply Placement Efficiency"
    )
    @Comment("Send a party chat message with your supply placement time and a dynamic rating")
    public static boolean supplyPlacementEfficiency = false;

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
    @Comment("Send a party chat message if you don't have a pre")
    public static boolean noPreAlert = true;

    @ConfigEntry(
            id = "secondSupplyAlert",
            translation = "Second Supply Alert"
    )
    @Comment("Announce the position of the second supply in chat")
    public static boolean secondSupplyAlert = true;

    @ConfigEntry(
            id = "supplyGiantHitboxAlert",
            translation = "Supply Giant Hitbox Alert"
    )
    @Comment("Highlight supply giants on contact; while collecting, keep Possible Double Pearl for body overlap or Need Double Pearl for eyes inside until you leave")
    public static boolean supplyGiantHitboxAlert = true;

    @ConfigEntry(
            id = "supplyGiantHitboxStyle",
            translation = "Supply Giant Hitbox Style"
    )
    @ConfigOption.Select
    @Comment("Choose how the giant hitbox highlight is rendered")
    public static WorldRenderUtils.RenderStyle supplyGiantHitboxStyle = WorldRenderUtils.RenderStyle.BOTH;

    @ConfigObject
    public static class SupplyTimesConfig {
        @ConfigEntry(id = "supplyTimesTitleColor", translation = "Title Color")
        @ConfigOption.Select
        @Comment("Choose the color used for the title")
        public static TextColor titleColor = TextColor.AQUA;

        @ConfigEntry(id = "supplyTimerCountdown", translation = "Countdown")
        @Comment("Show a countdown in Kuudra Splits displaying how long remains until supplies spawn")
        public static boolean countdown = true;

        @ConfigEntry(id = "supplyTimerCountdownWidget", translation = "Countdown Widget")
        @Comment("Transform the countdown text into a movable HUD widget")
        public static boolean countdownWidget = false;
    }

    @ConfigObject
    public static class CratePriorityConfig {
        @ConfigEntry(id = "cratePriorityColor", translation = "Title Color")
        @ConfigOption.Select
        @Comment("Choose the text color used by the Crate Priority title widget")
        public static TextColor cratePriorityColor = TextColor.GREEN;

        @ConfigEntry(id = "cratePriorityDurationSeconds", translation = "Notification Duration")
        @ConfigOption.Range(min = 1, max = 10)
        @ConfigOption.Slider
        @Comment("How long the Crate Priority title stays on screen (seconds)")
        public static int cratePriorityDurationSeconds = 6;

        @ConfigEntry(id = "cratePriorityAnimation", translation = "Notification Animation")
        @ConfigOption.Select
        @Comment("Choose the animation style for the Crate Priority title widget")
        public static CratePriorityAnimation cratePriorityAnimation = CratePriorityAnimation.FADE;

        @ConfigEntry(id = "cratePrioritySound", translation = "Notification Sound")
        @Comment("Play a sound when a new Crate Priority action is shown")
        public static boolean cratePrioritySound = true;
    }

    @ConfigObject
    public static class SupplyWaypointsConfig {
        @ConfigEntry(id = "supplyWaypointColor", translation = "Supply Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for the main supply marker")
        public static int supplyWaypointColor = new Color(0, 255, 255, 40).getRGB();

        @ConfigEntry(id = "supplyWaypointBoxSize", translation = "Box Size")
        @ConfigOption.Range(min = 1, max = 3)
        @ConfigOption.Slider
        @Comment("Size of the main supply hitbox")
        public static float supplyWaypointBoxSize = 1.0f;

        @ConfigEntry(id = "supplyHitBox", translation = "Interaction Box")
        @Comment("Show the pickup interaction box")
        public static boolean supplyHitBox = true;

        @ConfigEntry(id = "supplyInteractionBoxColor", translation = "Interaction Box Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for the interaction box")
        public static int supplyInteractionBoxColor = new Color(0, 255, 255, 90).getRGB();

        @ConfigEntry(id = "supplyInteractionBoxRange", translation = "Interaction Box Range")
        @Comment("Color the parts of the supply interaction box that are inside the player's vanilla interaction reach")
        public static boolean supplyInteractionBoxRange = true;

        @ConfigEntry(id = "supplyInteractionBoxInRangeColor", translation = "Interaction Box Range Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for the parts of the interaction box inside vanilla reach")
        public static int supplyInteractionBoxInRangeColor = new Color(85, 255, 85, 150).getRGB();

        @ConfigEntry(id = "supplyPullCircle", translation = "Pull Circle")
        @Comment("Show the supply pull circle")
        public static boolean supplyPullCircle = true;

        @ConfigEntry(id = "supplyPullCircleColor", translation = "Pull Circle Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used for the pull circle")
        public static int supplyPullCircleColor = new Color(0, 255, 255, 95).getRGB();

        @ConfigEntry(id = "supplyPullCircleBobberRange", translation = "Pull Circle Bobber Range")
        @Comment("Use a different color when your bobber is in range")
        public static boolean supplyPullCircleBobberRange = true;

        @ConfigEntry(id = "supplyPullCircleActiveColor", translation = "Pull Circle Bobber Range Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used when your bobber is in range")
        public static int supplyPullCircleActiveColor = new Color(85, 255, 85, 150).getRGB();

    }

    @ConfigObject
    public static class PileWaypointsConfig {
        @ConfigEntry(id = "pileWaypointNames", translation = "Pile Waypoint Names")
        @Comment("Show or hide pile name labels above waypoints")
        public static boolean pileWaypointNames = true;

        @ConfigEntry(id = "pileWaypointNameColor", translation = "Pile Waypoint Name Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of pile name labels")
        public static int pileWaypointNameColor = new Color(255, 255, 255, 255).getRGB();

        @ConfigEntry(id = "normalPileColor", translation = "Normal Pile Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of standard supply pile waypoints")
        public static int normalPileColor = new Color(255, 255, 255, 0).getRGB();

        @ConfigEntry(id = "noPrePileColor", translation = "No Pre Pile Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of no-pre supply pile waypoints")
        public static int noPrePileColor = new Color(0, 255, 144, 50).getRGB();

        @ConfigEntry(id = "placeAreaHitbox", translation = "Place Area Hitbox (Experimental)")
        @Comment("Render the server supply placement area on the floor around each pile")
        public static boolean placeAreaHitbox = false;

        @ConfigEntry(id = "placeAreaHitboxThroughWalls", translation = "Place Area Hitbox Through Walls")
        @Comment("Render the supply placement area through blocks")
        public static boolean placeAreaHitboxThroughWalls = false;

        @ConfigEntry(id = "placeAreaHitboxColor", translation = "Place Area Hitbox Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of the supply placement area")
        public static int placeAreaHitboxColor = new Color(85, 255, 85, 90).getRGB();

        @ConfigEntry(id = "placeAreaHitboxActiveColor", translation = "Place Area Hitbox Active Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of the supply placement area while you are standing inside it")
        public static int placeAreaHitboxActiveColor = new Color(255, 205, 85, 90).getRGB();
    }

    public enum PearlWaypointType {
        TIMER_MS,
        TIMER_SECONDS,
        TIMER_TICKS,
    }

    public enum PearlWaypointTextPosition {
        ABOVE,
        BELOW
    }

    public enum PearlWaypointRenderStyle {
        FULL_BLOCK,
        FILLED_OUTLINE,
        BLOCK_OUTLINE,
        SQUARE,
        CIRCLE
    }

    public enum EtherwarpWaypointRenderStyle {
        TOP,
        FULL_BLOCK,
        FILLED_OUTLINE,
        BLOCK_OUTLINE,
        SQUARE,
        CIRCLE
    }

    public enum EtherwarpWaypointTextPosition {
        ABOVE,
        BELOW
    }

    public enum CratePriorityAnimation {
        NONE,
        FADE,
        SLIDE
    }

}
