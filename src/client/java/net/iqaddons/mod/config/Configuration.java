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
            title = "HUD Editor",
            text = "OPEN"
    )
    @Comment("Open the HUD Editor to move and customize HUD elements.")
    public static final Runnable hudEditor = () -> {
        mc.execute(() -> HudManager.get().openEditor());
    };

    @ConfigEntry(
            id = "partyJoinSound",
            translation = "Party Join Sound"
    )
    @Comment("Play a sound when someone joins your party.")
    public static boolean partyJoinSound = true;

    @ConfigEntry(
            id = "fixFishingHook",
            translation = "Fix Fishing Hook"
    )
    @Comment("Fix the fishing hook getting stuck when throwing the rod.")
    public static boolean fixFishingHook = true;

    @ConfigEntry(
            id = "partyCommands",
            translation = "Party Commands"
    )
    @Comment("Configure commands triggered by party chat messages.")
    public static final PartyCommands partyCommands = new PartyCommands();

    @ConfigObject
    public static class PartyCommands {

        @ConfigEntry(
                id = "partyCommandsEnabled",
                translation = "Enable Party Commands"
        )
        @Comment("Allow party chat messages to trigger commands.")
        public static boolean enable = true;

        @ConfigEntry(id = "partyCommandWarp", translation = "!warp (!w, !wp)")
        @Comment("Run /party warp when someone sends !warp.")
        public static boolean warpCommand = true;

        @ConfigEntry(id = "partyCommandTransfer", translation = "!pt (!ptme, !transfer)")
        @Comment("Transfer party leader to the requester.")
        public static boolean transferCommand = true;

        @ConfigEntry(id = "partyCommandPing", translation = "!ping")
        @Comment("Reply with the requester's ping in party chat.")
        public static boolean partyCommandPing = true;

        @ConfigEntry(id = "partyCommandAllInvite", translation = "!allinvite (!allinv, !invites)")
        @Comment("Toggle /party settings allinvite when requested.")
        public static boolean allInviteCommand = true;

        @ConfigEntry(id = "partyCommandTps", translation = "!tps")
        @Comment("Reply with the current server TPS in party chat.")
        public static boolean partyCommandTps = true;

        @ConfigEntry(id = "partyCommandPromote", translation = "!promote")
        @Comment("Promote the requester.")
        public static boolean promoteCommand = true;

        @ConfigEntry(id = "partyCommandKick", translation = "!kick <player>")
        @Comment("Kick the specified player from the party.")
        public static boolean partyCommandKick = true;

        @ConfigEntry(id = "partyCommandKuudra", translation = "!t[1-5]")
        @Comment("Start a Kuudra run (Tier 1–5) when requested.")
        public static boolean kuudraCommand = true;

        @ConfigEntry(id = "partyCommandChests", translation = "!chests")
        @Comment("Reply with your current chest counter progress.")
        public static boolean partyCommandChests = true;
    }

    @ConfigOption.Separator("Wardrobe")

    @ConfigEntry(
            id = "wardrobeKeybinds",
            translation = "Wardrobe Keybinds"
    )
    @Comment("Enable keybind-based wardrobe slot selection")
    public static boolean wardrobeKeybinds = false;
    @Comment("Enable wardrobe slot selection using keybinds. Configure slot keybinds in Options → Controls → Keybinds.")
    public static boolean wardrobeKeybinds = true;

    @ConfigEntry(
            id = "wardrobeSound",
            translation = "Wardrobe Selection Sound"
    )
    @Comment("Play a sound after selecting a wardrobe slot via keybind.")
    public static boolean wardrobeSound = true;

    @ConfigOption.Separator("Shared Waypoints")

    @ConfigEntry(
            id = "waypointConfig",
            translation = "Shared Waypoints"
    )
    @Comment("Configure shared waypoint creation and rendering.")
    public static final Waypoints waypointConfig = new Waypoints();

    @ConfigObject
    public static class Waypoints {

        @ConfigEntry(
                id = "renderWaypoints",
                translation = "Enable Shared Waypoints"
        )
        @Comment("Create and render waypoints from Patcher-formatted coordinates.")
        public static boolean activated = true;

        @ConfigEntry(
                id = "waypointsDuration",
                translation = "Waypoint Duration (seconds)"
        )
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("How long waypoints stay on screen. Set to 0 to keep them until cleared/expired.")
        public static int duration = 7;

        @ConfigEntry(
                id = "waypointStyle",
                translation = "Render Style"
        )
        @ConfigOption.Select
        @Comment("Choose how waypoints are displayed.")
        public static WorldRenderUtils.RenderStyle style = WorldRenderUtils.RenderStyle.BOTH;
    }

}
