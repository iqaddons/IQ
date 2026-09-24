package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.config.screen.KuudraWaypointsScreen;
import net.iqaddons.mod.features.kuudra.waypoints.StunWaypointsFeature;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;

import java.awt.*;

@Category(
        value = "Phase 3 - Stun"
)
public class PhaseThreeConfig {

    @ConfigOption.Separator("Stun Widgets")
    @ConfigEntry(
            id = "eatenTimer",
            translation = "Eaten Timer"
    )
    @Comment("Display an overlay showing the remaining time until the player is eaten by Kuudra in cannonball")
    public static boolean eatenTimer = true;

    @ConfigEntry(
            id = "eatenTimerConfig",
            translation = "Eaten Timer Config"
    )
    @Comment("Customize the Eaten Timer display and vanilla countdown visibility")
    public static final EatenTimerConfig eatenTimerConfig = new EatenTimerConfig();

    @ConfigEntry(
            id = "kuudraHealthDisplay",
            translation = "Kuudra Health Widget"
    )
    @Comment("Show HP and percentage in the health widget")
    public static boolean kuudraHealthDisplay = true;

    @ConfigEntry(
            id = "kuudraHealthWidgetConfig",
            translation = "Kuudra Health Widget Config"
    )
    @Comment("Customize the Kuudra health widget")
    public static final KuudraHealthWidgetConfig kuudraHealthWidgetConfig = new KuudraHealthWidgetConfig();

    @ConfigObject
    public static class KuudraHealthWidgetConfig {
        @ConfigEntry(
                id = "renderHealthOnly",
                translation = "Render Health Only"
        )
        @Comment("Hide the title and show only Kuudra's health")
        public static boolean renderHealthOnly = true;

        @ConfigEntry(id = "highPercentageColor", translation = "75%-100% Percentage Color")
        @ConfigOption.Color(alpha = true)
        public static int highPercentageColor = new Color(85, 255, 85, 255).getRGB();

        @ConfigEntry(id = "midPercentageColor", translation = "50%-75% Percentage Color")
        @ConfigOption.Color(alpha = true)
        public static int midPercentageColor = new Color(255, 255, 85, 255).getRGB();

        @ConfigEntry(id = "lowPercentageColor", translation = "25%-50% Percentage Color")
        @ConfigOption.Color(alpha = true)
        public static int lowPercentageColor = new Color(255, 170, 0, 255).getRGB();

        @ConfigEntry(id = "criticalPercentageColor", translation = "0%-25% Percentage Color")
        @ConfigOption.Color(alpha = true)
        public static int criticalPercentageColor = new Color(255, 85, 85, 255).getRGB();
    }


    @ConfigEntry(
            id = "kuudraHitbox",
            translation = "Kuudra Hitbox"
    )
    @Comment("Show Kuudra's hitbox overlay")
    public static boolean enabled = true;

    @ConfigEntry(
            id = "kuudraHitbox",
            translation = "Kuudra Hitbox Config"
    )
    @Comment("Configure Kuudra's hitbox overlay during the run")
    public static final KuudraHitbox kuudraHitbox = new KuudraHitbox();

    @ConfigObject
    public static class KuudraHitbox {

        @ConfigEntry(
                id = "kuudraHitboxColor",
                translation = "Kuudra Hitbox Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change the hitbox color for better visibility")
        public static int color = new Color(255, 2, 2, 231).getRGB();

        @ConfigEntry(
                id = "kuudraHitboxStyle",
                translation = "Kuudra Hitbox Style"
        )
        @ConfigOption.Select
        @Comment("Choose how the hitbox is drawn (outline, filled, or both)")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.OUTLINE;
    }

    @ConfigEntry(
            id = "kuudraHealth",
            translation = "Kuudra Health"
    )
    @Comment("Show Kuudra's real health inside the Magma Cube")
    public static boolean kuudraHealth = true;

    @ConfigEntry(
            id = "kuudraHealthColorConfig",
            translation = "Kuudra Health Colors"
    )
    @Comment("Customize the colors used for each Kuudra health range")
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

    @ConfigOption.Separator("Stun Waypoints")
    @ConfigEntry(
            id = "stunWaypoints",
            translation = "Stun Waypoints"
    )
    @Comment("Show waypoints for stun positions")
    public static boolean stunWaypoints = true;

    @ConfigEntry(
            id = "stunWaypointColor",
            translation = "Stun Waypoints Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of stun waypoints")
    public static int stunWaypointColor = new Color(0, 245, 255, 200).getRGB();

    @ConfigEntry(
            id = "stunWaypointStyle",
            translation = "Stun Waypoint Style"
    )
    @ConfigOption.Select
    @Comment("Choose how stun waypoints are drawn (outline, filled, or both)")
    public static WorldRenderUtils.RenderStyle stunWaypointStyle = WorldRenderUtils.RenderStyle.OUTLINE;

    @ConfigEntry(
            id = "stunWaypointBlock",
            translation = "Stun Waypoint Block"
    )
    @ConfigOption.Select
    @Comment("Choose which stun location marker to display")
    public static StunWaypointsFeature.StunWaypoint stunWaypointBlock = StunWaypointsFeature.StunWaypoint.LEFT_POD;

    @ConfigOption.Separator("DPS Waypoint")

    @ConfigEntry(
            id = "dpsWaypoint",
            translation = "DPS Waypoint"
    )
    @Comment("Render DPS phase waypoints to help positioning during Phase 3")
    public static boolean dpsWaypoint = true;

    @ConfigEntry(
            id = "dpsWaypointConfig",
            translation = "DPS Waypoint Config"
    )
    @Comment("Customize DPS waypoints in the Kuudra Waypoints editor")
    public static final DpsWaypointConfig dpsWaypointConfig = new DpsWaypointConfig();

    @ConfigObject
    public static class DpsWaypointConfig {
        @ConfigButton(title = "Edit Waypoints", text = "OPEN")
        @Comment("Open the Kuudra Waypoints editor")
        @SuppressWarnings("unused")
        public static final Runnable editWaypoints = () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.gui.setScreen(new KuudraWaypointsScreen(mc.gui.screen())));
        };

        @ConfigEntry(id = "dpsWaypointColor", translation = "Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used by DPS phase waypoints")
        public static int waypointColor = new Color(0, 255, 255, 200).getRGB();

        @ConfigEntry(id = "dpsWaypointShowText", translation = "Show Text")
        @Comment("Show labels above DPS waypoints")
        public static boolean showText = false;

        @ConfigEntry(id = "dpsWaypointTextSize", translation = "Text Size")
        @ConfigOption.Range(min = 0.02, max = 0.16)
        @ConfigOption.Slider
        @Comment("Adjust the label size for DPS waypoints")
        public static float textSize = 0.05f;
    }

    @ConfigOption.Separator("SKIP Waypoint")

    @ConfigEntry(
            id = "skipWaypoint",
            translation = "SKIP Waypoint"
    )
    @Comment("Render the etherwarp target used to skip during Phase 3")
    public static boolean skipWaypoint = true;

    @ConfigEntry(
            id = "skipWaypointConfig",
            translation = "SKIP Waypoint Config"
    )
    @Comment("Customize SKIP waypoints in the Kuudra Waypoints editor")
    public static final SkipWaypointConfig skipWaypointConfig = new SkipWaypointConfig();

    @ConfigObject
    public static class SkipWaypointConfig {
        @ConfigButton(title = "Edit Waypoints", text = "OPEN")
        @Comment("Open the Kuudra Waypoints editor")
        @SuppressWarnings("unused")
        public static final Runnable editWaypoints = () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.gui.setScreen(new KuudraWaypointsScreen(mc.gui.screen())));
        };

        @ConfigEntry(id = "skipWaypointColor", translation = "Waypoint Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used by SKIP etherwarp waypoints")
        public static int waypointColor = new Color(0, 255, 255, 200).getRGB();

        @ConfigEntry(id = "skipWaypointShowText", translation = "Show Text")
        @Comment("Show labels above SKIP waypoints")
        public static boolean showText = false;

        @ConfigEntry(id = "skipWaypointTextSize", translation = "Text Size")
        @ConfigOption.Range(min = 0.02, max = 0.16)
        @ConfigOption.Slider
        @Comment("Adjust the label size for SKIP waypoints")
        public static float textSize = 0.05f;

        @ConfigEntry(id = "skipWaypointTracer", translation = "Waypoint Tracer")
        @Comment("Render a tracer line to the SKIP waypoint")
        public static boolean waypointTracer = false;

        @ConfigEntry(id = "skipWaypointTracerColor", translation = "Waypoint Tracer Color")
        @ConfigOption.Color(alpha = true)
        @Comment("Color used by the SKIP waypoint tracer line")
        public static int waypointTracerColor = new Color(0, 255, 255, 180).getRGB();
    }

    public enum EatenTimerType {
        TIMER_MS,
        TIMER_SECONDS,
        TIMER_TICKS,
    }

    public enum EatenTimerStartMode {
        COUNTDOWN_START(
                "COUNTDOWN START",
                "Shows as soon as Kuudra's 3/2/1 cannonball countdown starts"
        ),
        COUNTDOWN_END(
                "COUNTDOWN END",
                "Waits for the 3/2/1 countdown to finish, then starts at 35 ticks and re-syncs on blindness"
        ),
        BLINDNESS_EFFECT(
                "BLINDNESS EFFECT",
                "Shows only when the blindness effect is received, using the final 9 ticks"
        );

        private final String displayName;
        private final String description;

        EatenTimerStartMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String description() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigObject
    public static class EatenTimerConfig {

        @ConfigEntry(
                id = "timerType",
                translation = "Timer Type"
        )
        @ConfigOption.Select
        @Comment("Choose what the Eaten Timer displays: timer in milliseconds, seconds, or ticks")
        public static EatenTimerType timerType = EatenTimerType.TIMER_SECONDS;

        @ConfigEntry(
                id = "startMode",
                translation = "Start Mode"
        )
        @ConfigOption.Select
        @Comment("Choose when the Eaten Timer appears: countdown start, countdown end, or blindness effect")
        public static EatenTimerStartMode startMode = EatenTimerStartMode.COUNTDOWN_START;

        @ConfigEntry(
                id = "hideDefaultCountdown",
                translation = "Hide Default Countdown"
        )
        @Comment("Hide Kuudra's default 3/2/1 title/subtitle countdown while the Eaten Timer is active")
        public static boolean hideDefaultCountdown = false;
    }

    @ConfigOption.Separator("BLOCKERS")

    @ConfigEntry(
            id = "blockUselessPerks",
            translation = "Block Useless Perks"
    )
    @Comment("Prevent buying perks that provide no benefit for your run")
    public static boolean blockUselessPerks = true;

    @ConfigEntry(
            id = "blockPickobulus",
            translation = "Block Pickobulus"
    )
    @Comment("Blocks items with the Pickobulus ability outside Eaten and Stun")
    public static boolean blockPickobulus = true;

}
