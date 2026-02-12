package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.utils.render.WorldRenderUtils;

import java.awt.*;

@Category(
        value = "Phase 2 - Build"
)
public class PhaseTwoConfig {

    @ConfigOption.Separator("Build Waypoints")
    @ConfigEntry(
            id = "buildHelper",
            translation = "Build Helper"
    )
    @Comment("Render some helpful holograms during the build phase")
    public static boolean buildHelper = true;

    @ConfigOption.Separator("Build Widgets")
    @ConfigEntry(
            id = "buildProgressOverlay",
            translation = "Build Progress Overlay"
    )
    @Comment("Display a build progress overlay on the screen")
    public static boolean buildProgressOverlay = true;

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
    public static boolean freshCountdown = false;

    @ConfigOption.Separator("Build Highlights")
    @ConfigEntry(
            id = "teamHighlight",
            translation = "Team Highlight"
    )
    @Comment("Highlight teammates and show freshers during the build phase")
    public static boolean teamHighlight = true;

    @ConfigEntry(
            id = "teamHighlightColor",
            translation = "Team Hightlight Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Change the color of the teammate highlight")
    public static int teamHighlightColor = new Color(67, 179, 29, 209).getRGB();

    @ConfigEntry(
            id = "elleHighlightConfig",
            translation = "Elle Highlight Config"
    )
    @Comment("Configure the Elle highlight feature")
    public static final ElleConfig elleConfig = new ElleConfig();

    @ConfigObject
    public static class ElleConfig {

        @ConfigEntry(
                id = "elleHighlight",
                translation = "Elle Highlight"
        )
        @Comment("Draw a box around Elle during the build phase")
        public static boolean elleHighlight = true;

        @ConfigEntry(
                id = "elleHighlightColor",
                translation = "Elle Highlight Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change the color of the Elle highlight")
        public static int elleHighlightColor = new Color(255, 83, 83, 171).getRGB();

        @ConfigEntry(
                id = "elleHighlightStyle",
                translation = "Elle Highlight Style"
        )
        @ConfigOption.Select
        @Comment("Change the style of the Elle highlight")
        public static WorldRenderUtils.RenderStyle elleHighlightStyle = WorldRenderUtils.RenderStyle.BOTH;
    }

    @ConfigOption.Separator("Build Alerts")
    @ConfigEntry(
            id = "freshMessage",
            translation = "Fresh Message"
    )
    @Comment("Send a party message when you fresh")
    public static boolean freshMessage = true;

    @ConfigEntry(
            id = "teamHighlightFreshColor",
            translation = "Team Highlight Fresh Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Use a different color for freshers teammates")
    public static int freshHightlightColor = new Color(0.0f, 0.964f, 1.0f).getRGB();

    @ConfigOption.Separator("Ballista Sounds")
    @ConfigEntry(
            id = "replaceBallistaBuildSound",
            translation = "Replace Ballista Build Sound"
    )
    @Comment("Replace the default ballista build sound while in build phase")
    public static boolean replaceBallistaBuildSound = false;
}