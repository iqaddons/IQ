package net.iqaddons.mod.config;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.config.categories.*;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.MinecraftClient;

@Config(
        value = "iqaddons",
        categories = {
                KuudraGeneralConfig.class,
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

    @ConfigEntry(
            id = "partyJoinSound",
            translation = "Party Join Sound"
    )
    @Comment("Play a sound when a player joins your party")
    public static boolean partyJoinSound = true;

    @ConfigEntry(
            id = "fixFishingHook",
            translation = "Fix Fishing Hook"
    )
    @Comment("Fix the fishing hook block when throw the rod")
    public static boolean fixFishingHook = true;

    @ConfigEntry(
            id = "partyCommands",
            translation = "Party Commands"
    )
    @Comment("Enable commands that can be triggered from party chat messages")
    public static final PartyCommands partyCommands = new PartyCommands();

    @ConfigObject
    public static class PartyCommands {
        @ConfigEntry(
                id = "partyCommandsEnabled",
                translation = "Party Commands"
        )
        @Comment("Enable command triggers from party chat messages")
        public static boolean enable = false;

        @ConfigEntry(id = "partyCommandWarp", translation = "!warp (!w, !wp)")
        @Comment("Run /party warp when someone sends !warp")
        public static boolean warpCommand = true;

        @ConfigEntry(id = "partyCommandTransfer", translation = "!pt (!ptme, !transfer)")
        @Comment("Transfer party leadership to the player that requested it")
        public static boolean transferCommand = true;

        @ConfigEntry(id = "partyCommandPing", translation = "!ping")
        @Comment("Send the requester's ping in party chat")
        public static boolean partyCommandPing = true;

        @ConfigEntry(id = "partyCommandAllInvite", translation = "!allinvite (!allinv, !invites)")
        @Comment("Toggle all-invite with /party settings allinvite")
        public static boolean allInviteCommand = true;

        @ConfigEntry(id = "partyCommandTps", translation = "!tps")
        @Comment("Send current server TPS to party chat")
        public static boolean partyCommandTps = true;

        @ConfigEntry(id = "partyCommandPromote", translation = "!promote")
        @Comment("Promote the player that requested it")
        public static boolean promoteCommand = true;

        @ConfigEntry(id = "partyCommandKick", translation = "!kick <player>")
        @Comment("Kick a target player from the party")
        public static boolean partyCommandKick = true;

        @ConfigEntry(id = "partyCommandKuudra", translation = "!t[1-5]")
        @Comment("Start a Kuudra run")
        public static boolean kuudraCommand = true;

        @ConfigEntry(id = "partyCommandChests", translation = "!chests")
        @Comment("Reply with your current chest counter progress")
        public static boolean partyCommandChests = true;
    }

    @ConfigOption.Separator("Wardrobe")

    @ConfigEntry(
            id = "wardrobeKeybinds",
            translation = "Wardrobe Keybinds"
    )
    @Comment("Enable keybind-based wardrobe slot selection")
    public static boolean wardrobeKeybinds = false;

    @ConfigEntry(
            id = "wardrobeSound",
            translation = "Wardrobe Sound"
    )
    @Comment("Play a sound after selecting a wardrobe slot")
    public static boolean wardrobeSound = true;

    @ConfigOption.Separator("Shared Waypoints")
    @ConfigEntry(
            id = "waypointConfig",
            translation = "Shared Waypoints Config"
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
