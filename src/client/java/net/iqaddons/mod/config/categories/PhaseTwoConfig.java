package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

import java.awt.*;

@Category(
        value = "Phase 2 - Build"
)
public class PhaseTwoConfig {

    @ConfigEntry(
            id = "luckyBuild",
            translation = "Lucky Build Things"
    )
    @Comment("Give you some extra luck during the build phase ❤")
    public static boolean luckyBuild = true;

    @ConfigOption.Separator("Build Waypoints")
    @ConfigEntry(
            id = "buildHelper",
            translation = "Build Helper"
    )
    @Comment("Render some helpful holograms during the build phase")
    public static boolean buildHelper = true;

    @ConfigEntry(
            id = "buildWaypointsOpacity",
            translation = "Build Helper Opacity"
    )
    @ConfigOption.Range(min = 0, max = 1)
    @ConfigOption.Slider
    @Comment("Change the opacity of the build helper holograms.")
    public static float buildHelperOpacity = 0.5f;

    @ConfigEntry(
            id = "hideDefaultBuildPileText",
            translation = "Hide Default Build Pile Text"
    )
    @Comment("Hide Kuudra's default pile progress holograms while IQ build overlay is active.")
    public static boolean hideDefaultBuildPileText = true;

    @ConfigOption.Separator("Build Widgets")
    @ConfigEntry(
            id = "buildProgressOverlay",
            translation = "Build Progress Overlay"
    )
    @Comment("Display a build progress overlay on the screen")
    public static boolean buildProgressOverlay = true;

    @ConfigEntry(
            id = "simpleBuildProgressOverlay",
            translation = "Simple Build Progress Overlay"
    )
    @Comment("Display only the build percent as a compact single-line widget")
    public static boolean simpleBuildProgressOverlay = false;

    @ConfigEntry(
            id = "buildStartCountdownOverlay",
            translation = "Build Start Countdown"
    )
    @Comment("Show a countdown for when the build starts during the phase animation.")
    public static boolean buildStartCountdownOverlay = true;

    @ConfigEntry(
            id = "freshTimers",
            translation = "Fresh Times"
    )
    @Comment("Render a timer above freshers heads during the build phase")
    public static boolean freshTimers = true;

    @ConfigEntry(
            id = "freshCountdown",
            translation = "Fresh Countdown"
    )
    @Comment("Display a countdown timer of you fresh")
    public static boolean freshCountdown = true;

    @ConfigEntry(
            id = "elleHighlightConfig",
            translation = "Elle Highlight Config"
    )
    @Comment("Configure the Elle highlight feature.")
    public static final ElleConfig elleConfig = new ElleConfig();
    @ConfigOption.Separator("Build Highlights")
    @ConfigEntry(
            id = "teamHighlight",
            translation = "Team Highlight"
    )
    @Comment("Highlight teammates with the normal team glow during the run.")
    public static boolean teamHighlight = true;

    @ConfigEntry(
            id = "teamHighlightColor",
            translation = "Team Highlight Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of the teammate highlight.")
    public static int teamHighlightColor = new Color(67, 179, 29, 209).getRGB();

    @ConfigEntry(
            id = "freshHighlightIndependent",
            translation = "Fresh Highlight"
    )
    @Comment("Temporarily override a player's glow when they fresh.")
    public static boolean freshHighlightIndependent = true;

    @ConfigEntry(
            id = "freshHighlightColor",
            translation = "Fresh Highlight Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Color to use when highlighting freshers.")
    public static int freshHighlightColor = new Color(0.0f, 0.964f, 1.0f).getRGB();

    @ConfigOption.Separator("Build Alerts")
    @ConfigEntry(
            id = "freshMessage",
            translation = "Fresh Message"
    )
    @Comment("Send a party message when you fresh.")
    public static boolean freshMessage = true;

    @ConfigOption.Separator("Ballista Sounds")

    @ConfigEntry(
            id = "cleanBallistaBuildSounds",
            translation = "Clean Ballista Build Sounds"
    )
    @Comment("Disable most of sounds during build for a cleaner audio experience.")
    public static boolean cleanBallistaSounds = true;
    @ConfigEntry(
            id = "replaceBallistaBuildSound",
            translation = "Replace Ballista Build Sound"
    )
    @Comment("Replace the default ballista build sound while in build phase.")
    public static boolean replaceBallistaBuildSound = true;

    @ConfigObject
    public static class ElleConfig {

        @ConfigEntry(
                id = "elleHighlight",
                translation = "Elle Highlight"
        )
        @Comment("Draw a box around Elle during the build phase.")
        public static boolean elleHighlight = true;

        @ConfigEntry(
                id = "elleHighlightColor",
                translation = "Elle Highlight Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of the Elle highlight.")
        public static int elleHighlightColor = new Color(255, 83, 83, 171).getRGB();

        @ConfigEntry(
                id = "elleHighlightStyle",
                translation = "Elle Highlight Style"
        )
        @ConfigOption.Select
        @Comment("Change the style of the Elle highlight.")
        public static WorldRenderUtils.RenderStyle elleHighlightStyle = WorldRenderUtils.RenderStyle.OUTLINE;
    }
}