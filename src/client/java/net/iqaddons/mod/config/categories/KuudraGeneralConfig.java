package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.features.kuudra.miscellaneous.HideUselessArmorStandsFeature;
import net.iqaddons.mod.model.profit.CrimsonFaction;
import net.iqaddons.mod.utils.TextColor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import net.iqaddons.mod.config.screen.EtherwarpCategorySelectorScreen;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;

@Category(
        value = "Kuudra Utilities"
)
public class KuudraGeneralConfig {

    @ConfigOption.Separator("Kuudra Requeuing")

    @ConfigEntry(
            id = "autoRequeue",
            translation = "Auto Requeue"
    )
    @Comment("Automatically start a new Kuudra run after the boss is defeated.")
    public static boolean autoRequeue = true;

    @ConfigEntry(
            id = "requeueDelay",
            translation = "Auto Requeue Delay"
    )
    @Comment("Delay before requeueing (in ticks).")
    @ConfigOption.Range(min = 1, max = 50)
    @ConfigOption.Slider
    public static int requeueDelay = 20;

    @ConfigOption.Separator("Profit Tracking")

    @ConfigEntry(
            id = "kuudraProfitTracker",
            translation = "Kuudra Profit Tracker"
    )
    @Comment("Track profit and loss after each Kuudra run and display it on screen.")
    public static boolean kuudraProfitTracker = true;

    @ConfigEntry(
            id = "profitTrackerVisibility",
            translation = "Profit Tracker Visibility"
    )
    @ConfigOption.Select
    @Comment("Control when the profit tracker is visible.")
    public static ProfitTrackerVisibility profitTrackerVisibility = ProfitTrackerVisibility.KUUDRA_AREAS;

    @ConfigEntry(
            id = "bazaarPricingMode",
            translation = "Bazaar Pricing Mode"
    )
    @ConfigOption.Select
    @Comment("Choose how Bazaar prices are calculated for profit and chest value.")
    public static BazaarPricingMode bazaarPricingMode = BazaarPricingMode.SELL_ORDER;

    @ConfigEntry(
            id = "crimsonIsleFaction",
            translation = "Crimson Isle Faction"
    )
    @ConfigOption.Select
    @Comment("Select your Crimson Isle faction to improve profit calculations.")
    public static CrimsonFaction crimsonIsleFaction = CrimsonFaction.MAGE;


    @ConfigEntry(
            id = "armorValueType",
            translation = "Armor Value Type"
    )
    @ConfigOption.Select
    @Comment("Choose how armor value is calculated in the profit tracker.")
    public static ArmorValueType armorValueType = ArmorValueType.SALVAGE;

    @ConfigEntry(
            id = "kuudraPetBonus",
            translation = "Kuudra Pet Bonus"
    )
    @ConfigOption.Range(min = 0, max = 20)
    @ConfigOption.Slider
    @Comment("Apply your Kuudra pet level bonus to profit calculations.")
    public static int kuudraPetBonus = 20;

    @ConfigOption.Separator("Widgets")

    @ConfigEntry(
            id = "chestValueWidget",
            translation = "Chest Value Display"
    )
    @Comment("Show the value of each chest when opened.")
    public static boolean chestValueWidget = true;

    @ConfigEntry(
            id = "croesusHelper",
            translation = "Croesus Helper"
    )
    @Comment("Highlight already opened chests in Croesus and Vesuvius menus.")
    public static boolean croesusHelper = true;

    @ConfigEntry(
            id = "chestCounterTracker",
            translation = "Chest Counter Tracker"
    )
    @Comment("Track progress toward the 60 chest cap.")
    public static boolean chestCounterTracker = true;

    @ConfigEntry(
            id = "chestCounterPartyAnnouncements",
            translation = "Chest Counter Party Announcements"
    )
    @Comment("Send reminders to party chat at milestones and when nearing the cap.")
    public static boolean chestCounterPartyAnnouncements = true;

    @ConfigOption.Separator("Custom Splits")

    @ConfigEntry(
            id = "customSplits",
            translation = "Custom Splits"
    )
    @Comment("Display an overlay with timings for each Kuudra phase.")
    public static boolean customSplits = true;

    @ConfigEntry(
            id = "customSplitsBenchmarks",
            translation = "Pace Custom Splits"
    )
    @Comment("Set the split timings that are used to calculate the pace feature.")
    public static final CustomSplitsBenchmarks customSplitsBenchmarks = new CustomSplitsBenchmarks();

    @ConfigEntry(
            id = "splitColorConfig",
            translation = "Split Time Colors"
    )
    @Comment("Customize the color of each phase of Custom Splits.")
    public static final SplitColorConfig splitColorConfig = new SplitColorConfig();

    @ConfigObject
    public static class SplitColorConfig {
        @ConfigEntry(id = "supplies", translation = "Supplies Text Color")
        @ConfigOption.Select
        public static TextColor supplies = TextColor.GRAY;

        @ConfigEntry(id = "build", translation = "Build Text Color")
        @ConfigOption.Select
        public static TextColor build = TextColor.GRAY;

        @ConfigEntry(id = "eaten", translation = "Eaten Text Color")
        @ConfigOption.Select
        public static TextColor eaten = TextColor.GRAY;

        @ConfigEntry(id = "stun", translation = "Stun Text Color")
        @ConfigOption.Select
        public static TextColor stun = TextColor.GRAY;

        @ConfigEntry(id = "dps", translation = "DPS Text Color")
        @ConfigOption.Select
        public static TextColor dps = TextColor.GRAY;

        @ConfigEntry(id = "skip", translation = "Skip Text Color")
        @ConfigOption.Select
        public static TextColor skip = TextColor.GRAY;

        @ConfigEntry(id = "boss", translation = "Boss Text Color")
        @ConfigOption.Select
        public static TextColor boss = TextColor.GRAY;

        @ConfigEntry(id = "overall", translation = "Overall Text Color")
        @ConfigOption.Select
        public static TextColor overall = TextColor.GRAY;

        @ConfigEntry(id = "pace", translation = "Pace Text Color")
        @ConfigOption.Select
        public static TextColor pace = TextColor.GRAY;
    }

    @ConfigOption.Separator("Notifications")

    @ConfigEntry(
            id = "kuudraNotifications",
            translation = "Kuudra Notifications"
    )
    @Comment("Configure alerts for important Kuudra-related events.")
    public static final KuudraNotifications kuudraNotifications = new KuudraNotifications();

    @ConfigObject
    public static class KuudraNotifications {

        @ConfigEntry(
                id = "kuudraNotificationBuildStarted",
                translation = "Build Started Notification"
        )
        @Comment("Show an alert when Elle asks to build the Ballista.")
        public static boolean buildStarted = true;

        @ConfigEntry(
                id = "kuudraNotificationIchorUsed",
                translation = "Ichor Used Notification"
        )
        @Comment("Show an alert when Ichor Pool is cast.")
        public static boolean ichorUsed = true;

        @ConfigEntry(
                id = "kuudraNotificationNoPre",
                translation = "No Pre Notifications"
        )
        @Comment("Show an alert for 'No Pre' party messages.")
        public static boolean noPre = true;

        @ConfigEntry(
                id = "kuudraNotificationSosReminder",
                translation = "SOS Reminder Notification"
        )
        @Comment("Show an alert 4 seconds before the stun phase begins.")
        public static boolean sosReminder = true;

        @ConfigEntry(
                id = "kuudraPhaseAlert",
                translation = "Phase Change Notification"
        )
        @Comment("Show an alert when the Kuudra phase changes.")
        public static boolean phaseChange = true;

        @ConfigEntry(
                id = "kuudraNotificationBuildDone",
                translation = "Build Done Notification"
        )
        @Comment("Show an alert when the Ballista is fully built.")
        public static boolean buildDone = true;

        @ConfigEntry(
                id = "kuudraNotificationSuppliesDone",
                translation = "Supplies Done Notification"
        )
        @Comment("Show an alert when supplies reach 6/6.")
        public static boolean suppliesDone = true;

        @ConfigEntry(
                id = "kuudraNotificationCannonball",
                translation = "Cannonball Notification"
        )
        @Comment("Show an alert when Human Cannonball is purchased.")
        public static boolean cannonBall = true;

        @ConfigEntry(
                id = "supplyPickingAlert",
                translation = "Supply Already Picking Alert"
        )
        @Comment("Show an alert if another player is already picking your supply.")
        public static boolean supplyPickingAlert = true;

        @ConfigEntry(
                id = "supplyDroppedTitle",
                translation = "Supply Dropped Notification"
        )
        @Comment("Show an alert when a supply is dropped.")
        public static boolean supplyDropped = true;

        @ConfigEntry(
                id = "supplyPickedUpTitle",
                translation = "Supply Picked Up Notification"
        )
        @Comment("Show an alert when you finish picking up a supply.")
        public static boolean supplyPickedUp = true;
    }

    @ConfigEntry(
            id = "kuudraNotificationsSound",
            translation = "Kuudra Notifications Sound"
    )
    @Comment("Play a sound when a Kuudra notification is shown.")
    public static boolean kuudraNotificationsSound = true;

    @ConfigEntry(
            id = "abilityAnnounce",
            translation = "Ability Announce"
    )
    @Comment("Announce ability usage in party chat.")
    public static final AbilityAnnounce abilityAnnounce = new AbilityAnnounce();

    @ConfigObject
    public static class AbilityAnnounce {

        @ConfigEntry(
                id = "abilityAnnounceSpiritSpark",
                translation = "Spirit Spark"
        )
        @Comment("Send \"Spirit Spark Casted!\" in party chat.")
        public static boolean spiritSpark = true;

        @ConfigEntry(
                id = "abilityAnnounceHollowedRush",
                translation = "Hollowed Rush"
        )
        @Comment("Send \"Hollowed Rush Casted!\" in party chat.")
        public static boolean hollowedRush = true;

        @ConfigEntry(
                id = "abilityAnnounceRagingWind",
                translation = "Raging Wind"
        )
        @Comment("Send \"Raging Wind Casted!\" in party chat.")
        public static boolean ragingWind = true;

        @ConfigEntry(
                id = "abilityAnnounceIchorPool",
                translation = "Ichor Pool"
        )
        @Comment("Send \"Ichor Pool Casted!\" in party chat.")
        public static boolean ichorPool = true;

        @ConfigEntry(
                id = "abilityAnnounceManaDrain",
                translation = "Mana Drain"
        )
        @Comment("Send the amount of mana drained to party chat.")
        public static boolean manaDrain = true;

        public static boolean hasSpellEnabled() {
            return spiritSpark || hollowedRush || ragingWind || ichorPool;
        }
    }

    @ConfigEntry(
            id = "personalBestTracker",
            translation = "Personal Best Tracker"
    )
    @Comment("Track your personal best Kuudra time and notify when beaten.")
    public static boolean personalBestTracker = true;

    @ConfigEntry(
            id = "phaseSplitsPBTracker",
            translation = "Phase Splits PB Tracker"
    )
    @Comment("Track your personal best time for each individual Kuudra phase (T5 Infernal only). Notifies when a phase PB is beaten.")
    public static boolean phaseSplitsPBTracker = true;

    @ConfigOption.Separator("Etherwarp Helper")

    @ConfigEntry(
            id = "etherwarpHelper",
            translation = "Etherwarp Helper"
    )
    @Comment("Render Etherwarp helper waypoints by Kuudra phase.")
    public static boolean etherwarpHelper = true;

    @ConfigButton(
            title = "Etherwarp Categories",
            text = "SELECT"
    )
    @Comment("Choose which Etherwarp categories are active. \nThis list is pulled from category names in etherwarp_config.json and updates after /iq reload.")
    @SuppressWarnings("unused")
    public static final Runnable openEtherwarpCategorySelector = () -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> mc.setScreen(new EtherwarpCategorySelectorScreen(mc.currentScreen)));
    };

    @ConfigButton(
            title = "Open Etherwarp Config",
            text = "OPEN"
    )
    @Comment("Open config/iq folder to edit etherwarp_config.json. \nSave and run /iq reload to edit in real-time.")
    @SuppressWarnings("unused")
    public static final Runnable openEtherwarpConfig = () -> {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("iq");
            Files.createDirectories(configDir);
            Util.getOperatingSystem().open(configDir.toFile());
        } catch (Exception ignored) {
        }
    };

    @ConfigOption.Separator("Visual")

    @ConfigEntry(
            id = "hideUselessArmorStands",
            translation = "Hide Useless Armor Stands"
    )
    @Comment("Hide armor stands used for visual effects that don't provide useful information.")
    public static HideUselessArmorStandsFeature.HiddenArmorStandType[] hideUselessArmorStands = new HideUselessArmorStandsFeature.HiddenArmorStandType[]{
            HideUselessArmorStandsFeature.HiddenArmorStandType.SHOP,
            HideUselessArmorStandsFeature.HiddenArmorStandType.BUILD,
            HideUselessArmorStandsFeature.HiddenArmorStandType.OTHERS
    };

    @ConfigEntry(
            id = "hideMobNametags",
            translation = "Hide Mob Nametags"
    )
    @Comment("Prevent loading nametags from Kuudra mobs.")
    public static boolean hideMobNametags = true;

    @ConfigObject
    public static class CustomSplitsBenchmarks {

        @ConfigEntry(id = "supplies", translation = "Supplies Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Supplies phase.")
        public static double supplies = 22.5;

        @ConfigEntry(id = "build", translation = "Build Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Build phase.")
        public static double build = 12;

        @ConfigEntry(id = "eaten", translation = "Eaten Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Eaten phase.")
        public static double eaten = 4.125;

        @ConfigEntry(id = "stun", translation = "Stun Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Stun phase.")
        public static double stun = 0.0;

        @ConfigEntry(id = "dps", translation = "DPS Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the DPS phase.")
        public static double dps = 3.5;

        @ConfigEntry(id = "skip", translation = "Skip Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Skip phase.")
        public static double skip = 4.6;

        @ConfigEntry(id = "boss", translation = "Boss Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Target time (seconds) for the Boss phase.")
        public static double boss = 1.875;
    }

    @ConfigEntry(
            id = "hideKuudraBossBar",
            translation = "Hide Kuudra Boss Bar"
    )
    @Comment("Hide Kuudra's vanilla boss bar during runs.")
    public static boolean hideKuudraBossBar = true;

    public enum ProfitTrackerVisibility {
        KUUDRA_AREAS, ALWAYS
    }

    public enum BazaarPricingMode {
        INSTA_SELL, SELL_ORDER
    }

    public enum ArmorValueType {
        SALVAGE, AUCTION
    }
}
