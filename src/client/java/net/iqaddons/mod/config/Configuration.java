package net.iqaddons.mod.config;

import com.teamresourceful.resourcefulconfig.api.annotations.*;

import java.awt.*;

@Config(
        value = "iqaddons",
        categories = {
                Configuration.PhaseOneConfig.class,
                Configuration.PhaseTwoConfig.class,
                Configuration.PhaseThreeConfig.class,
                Configuration.PhaseFourConfig.class,
        }
)
@ConfigInfo(
        title = "IQ Addons",
        description = "IQ is a Hypixel SkyBlock mod made especially for Kuudra.",
        links = {
                @ConfigInfo.Link(value = "https://github.com/pehenrii/IQ", icon = "code-2", text = "Github"),
                @ConfigInfo.Link(value = "https://discord.gg/HdhXhCWcW9", icon = "discord", text = "Discord"),
        }
)

public class Configuration {

    @ConfigButton(
            title = "Open HUD Editor",
            text = "Open"
    )
    @Comment("Open the HUD Editor to customize your HUD elements")
    public static final Runnable hudEditor = () -> {
    };

    @ConfigOption.Separator(
            value = "Kuudra"
    )
    @ConfigEntry(
            id = "autoRequeue",
            translation = "Auto Requeue"
    )
    @Comment("Automatically requeue a Kuudra run after boss death")
    public static boolean autoRequeue = false;

    @ConfigEntry(
            id = "requeueDelay",
            translation = "Requeue Delay"
    )
    @Comment("Delay in ticks before auto-requeue (5-50)")
    @ConfigOption.Range(min = 5, max = 50)
    @ConfigOption.Slider
    public static int requeueDelay = 20;

    @ConfigEntry(
            id = "customSplits",
            translation = "Custom Splits"
    )
    @Comment("Enable custom split timers")
    public static boolean customSplits = true;

    @ConfigEntry(
            id = "teamHighlight",
            translation = "Team Highlight"
    )
    @Comment("Highlight teammates and show freshers during the build phase")
    public static boolean teamHighlight = true;

    @ConfigEntry(
            id = "manaDrainNotify",
            translation = "Mana Drain Notify"
    )
    @Comment(" Send the amount of mana drained to party chat")
    public static boolean manaDrainNotify = true;

    @ConfigEntry(
            id = "partyJoinSound",
            translation = "Party Join Sound"
    )
    @Comment("Play a sound when a player joins your party")
    public static boolean partyJoinSound = true;

    @ConfigEntry(
            id = "hideMobNametags",
            translation = "Hide Mob Nametags"
    )
    @Comment("Prevent Kuudra mobs nametags from loading.")
    public static boolean hideMobNametags = false;

    @ConfigOption.Separator(
            value = "Waypoints"
    )

    @ConfigEntry(
            id = "renderWaypoints",
            translation = "Render Waypoints"
    )
    @Comment("Create waypoints from Patcher-formatted coordinates.")
    public static boolean renderWaypoints = true;


    @ConfigEntry(
            id = "waypointsDuration",
            translation = "Waypoints Duration (seconds)"
    )
    @ConfigOption.Range(min = 1, max = 60)
    @ConfigOption.Slider
    @Comment("Set a duration in seconds or 0 to disable (mob waypoints last 1/3 of the time).")
    public static int waypointsDuration = 1;

    @Category(
            value = "Phase 1 - Supplies"
    )
    public static class PhaseOneConfig {

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
        public static int supplyWaypointColor = new Color(0, 0, 0, 255).getRGB();

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

        @ConfigEntry(
                id = "crateDisplay",
                translation = "Crate Display"
        )
        @Comment("Custom HUD showing crate pickups")
        public static boolean crateDisplay = true;
    }

    @Category(
            value = "Phase 2 - Build"
    )
    public static class PhaseTwoConfig {

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

    @Category(
            value = "Phase 3 - Stun"
    )
    public static class PhaseThreeConfig {

        @ConfigEntry(
                id = "kuudraHPBossbar",
                translation = "Kuudra HP Bossbar"
        )
        @Comment("Show Kuudra's HP in a custom bossbar")
        public static boolean kuudraHPBossbar = true;

        @ConfigEntry(
                id = "kuudraHitbox",
                translation = "Kuudra Hitbox"
        )
        @Comment("Render Kuudra's hitbox during the stun phase")
        public static boolean kuudraHitbox = true;

        @ConfigEntry(
                id = "kuudraHitboxColor",
                translation = "Kuudra Hitbox Color"
        )
        @ConfigOption.Color(alpha = true)
        @Comment("Change Kuudra's hitbox color to something more visible")
        public static int kuudraHitboxColor = new Color(0.0f, 0.964f, 1.0f).getRGB();

        @ConfigEntry(
                id = "blockUselessPerks",
                translation = "Block Useless Perks"
        )
        @Comment("Prevent purchasing useless perks")
        public static boolean blockUselessPerks = true;
    }

    @Category(
            value = "Phase 4 - Boss Fight"
    )
    public static class PhaseFourConfig {

        @ConfigEntry(
                id = "kuudraSpawnAlert",
                translation = "Kuudra Spawn Alert"
        )
        @Comment("Alert which side Kuudra will spawn on")
        public static boolean kuudraSpawnAlert = true;

        @ConfigEntry(
                id = "rendDamageAlert",
                translation = "Rend Damage"
        )
        @Comment("Track when any teammate deals Rend Damage")
        public static boolean rendDamageAlert = true;

    }
}
