package net.iqaddons.mod.config;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.MinecraftClient;

import java.awt.*;

@Config(
        value = "iqaddons",
        categories = {
                PhaseOneConfig.class, PhaseTwoConfig.class,
                PhaseThreeConfig.class, PhaseFourConfig.class,
        }
)
@ConfigInfo(
        title = "IQ Addons",
        description = "IQ is a Hypixel SkyBlock mod made especially for Kuudra.",
        links = {
                @ConfigInfo.Link(value = "https://github.com/pehenrii/IQ", icon = "code-2", text = "Github"),
                @ConfigInfo.Link(value = "https://discord.gg/HdhXhCWcW9", icon = "discord", text = "Discord"),
                @ConfigInfo.Link(value = "https://patreon.com/IQAddons", icon = "patreon", text = "Patreon")
        }
)

public class Configuration {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @ConfigButton(
            title = "Open HUD Editor",
            text = "OPEN"
    )
    @Comment("Open the HUD Editor to customize your HUD elements")
    public static final Runnable hudEditor = () -> {
        mc.execute(() -> HudManager.get().openEditor());
    };

    @ConfigOption.Separator(
            value = "Kuudra"
    )
    @ConfigEntry(
            id = "autoRequeue",
            translation = "Auto Requeue"
    )
    @Comment("Automatically requeue a Kuudra run after boss death (W.I.P)")
    public static boolean autoRequeue = false;

    @ConfigEntry(
            id = "requeueDelay",
            translation = "Requeue Delay"
    )
    @Comment("Delay in ticks before auto-requeue (5-50) (W.I.P)")
    @ConfigOption.Range(min = 5, max = 50)
    @ConfigOption.Slider
    public static int requeueDelay = 20;

    @ConfigEntry(
            id = "kuudraProfitTracker",
            translation = "Kuudra Profit Tracker"
    )
    @Comment("Track your profit/loss after each Kuudra run (W.I.P)")
    public static boolean kuudraProfitTracker = false;

    @ConfigEntry(
            id = "customSplits",
            translation = "Custom Splits"
    )
    @Comment("Render a overlay with all the Kuudra phases times")
    public static boolean customSplits = true;

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
            id = "manaDrainNotify",
            translation = "Mana Drain Notify"
    )
    @Comment(" Send the amount of mana drained to party chat")
    public static boolean manaDrainNotify = true;

    @ConfigEntry(
            id = "kuudraPhaseAlert",
            translation = "Phase Alert"
    )
    @Comment("Alert when Kuudra phase changes")
    public static boolean kuudraPhaseAlert = true;

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
    @Comment("Prevent Kuudra mobs nametags from loading")
    public static boolean hideMobNametags = false;

    @ConfigEntry(
            id = "fixFishingHook",
            translation = "Fix Fishing Hook"
    )
    @Comment("Fix the fishing hook block when throw the rod (W.I.P)")
    public static boolean fixFishingHook = true;

    @ConfigEntry(
            id = "waypointConfig",
            translation = "Shared Waypoints"
    )
    @Comment("Configure the shared waypoints feature")
    public static final Waypoints waypointConfig = new Waypoints();

    @ConfigObject
    public static class Waypoints {

        @ConfigEntry(
                id = "renderWaypoints",
                translation = "Render Waypoints"
        )
        @Comment("Create waypoints from Patcher-formatted coordinates")
        public static boolean activated = true;

        @ConfigEntry(
                id = "waypointsDuration",
                translation = "Waypoints Duration (seconds)"
        )
        @ConfigOption.Range(min = 1, max = 60)
        @ConfigOption.Slider
        @Comment("Set a duration in seconds or 0 to disable.")
        public static int duration = 1;

        @ConfigEntry(
                id = "waypointStyle",
                translation = "Waypoint Style"
        )
        @ConfigOption.Select
        @Comment("Change the style of the waypoint rendering")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.BOTH;
    }
}
