package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

import java.awt.*;

@Category(
        value = "Phase 2 - Build"
)
public class PhaseTwoConfig {

    @ConfigEntry(
            id = "buildOverlay",
            translation = "Build Overlay"
    )
    @Comment("Custom build overlay with pile colors based on percentage")
    public static boolean buildOverlay = true;

    @ConfigEntry(
            id = "buildHelper",
            translation = "Build Helper"
    )
    @Comment("Build helper HUD")
    public static boolean buildHelper = true;

    @ConfigEntry(
            id = "freshTimers",
            translation = "Fresh Times"
    )
    @Comment("Display build times for all freshers")
    public static boolean freshTimers = true;

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
    public static int elleHighlightColor = new Color(0, 0, 0, 255).getRGB();

    @ConfigEntry(
            id = "freshMessage",
            translation = "Fresh Message"
    )
    @Comment(" Send a party message when you fresh")
    public static boolean freshMessage = true;

    @ConfigEntry(
            id = "teamHighlightFreshColor",
            translation = "Team Highlight Fresh Color"
    )
    @ConfigOption.Color(alpha = true)
    @Comment("Use a different color for freshers teammates")
    public static int freshHightlightColor = new Color(0.0f, 0.964f, 1.0f).getRGB();
}