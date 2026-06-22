package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.config.screen.EtherwarpCategorySelectorScreen;
import net.iqaddons.mod.model.profit.CrimsonFaction;
import net.iqaddons.mod.utils.TextColor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

@Category(
        value = "Kuudra Utilities"
)
public class KuudraGeneralConfig {

    @ConfigOption.Separator("Kuudra Splits")

    @ConfigEntry(
            id = "customSplits",
            translation = "Kuudra Splits"
    )
    @Comment("Display an overlay with timings for each Kuudra phase.")
    public static boolean customSplits = true;

    @ConfigEntry(
            id = "splitColorConfig",
            translation = "Split Time Colors"
    )
    @Comment("Customize the color of each phase of Custom Splits.")
    public static final SplitColorConfig splitColorConfig = new SplitColorConfig();

    @ConfigObject
    public static class SplitColorConfig {
        @ConfigEntry(id = "title", translation = "Title Text Color")
        @ConfigOption.Select
        public static TextColor title = TextColor.AQUA;

        @ConfigEntry(id = "supplies", translation = "Supplies Text Color")
        @ConfigOption.Select
        public static TextColor supplies = TextColor.DARK_AQUA;

        @ConfigEntry(id = "build", translation = "Build Text Color")
        @ConfigOption.Select
        public static TextColor build = TextColor.DARK_AQUA;

        @ConfigEntry(id = "eaten", translation = "Eaten Text Color")
        @ConfigOption.Select
        public static TextColor eaten = TextColor.DARK_AQUA;

        @ConfigEntry(id = "stun", translation = "Stun Text Color")
        @ConfigOption.Select
        public static TextColor stun = TextColor.DARK_AQUA;

        @ConfigEntry(id = "dps", translation = "DPS Text Color")
        @ConfigOption.Select
        public static TextColor dps = TextColor.DARK_AQUA;

        @ConfigEntry(id = "skip", translation = "Skip Text Color")
        @ConfigOption.Select
        public static TextColor skip = TextColor.DARK_AQUA;

        @ConfigEntry(id = "boss", translation = "Boss Text Color")
        @ConfigOption.Select
        public static TextColor boss = TextColor.DARK_AQUA;

        @ConfigEntry(id = "overall", translation = "Overall Text Color")
        @ConfigOption.Select
        public static TextColor overall = TextColor.DARK_AQUA;

        @ConfigEntry(id = "pace", translation = "Pace Text Color")
        @ConfigOption.Select
        public static TextColor pace = TextColor.DARK_AQUA;
    }

    @ConfigEntry(
            id = "customSplitsBenchmarks",
            translation = "Pace Splits Benchmarks"
    )
    @Comment("Pace is the run time to beat so just set the best possible times in each phase.")
    public static final CustomSplitsBenchmarks customSplitsBenchmarks = new CustomSplitsBenchmarks();

    @ConfigOption.Separator("Profit Tracking")

    @ConfigEntry(
            id = "kuudraProfitTracker",
            translation = "Kuudra Profit Tracker"
    )
    @Comment("Track profit and loss after each Kuudra run and display it on screen.")
    public static boolean kuudraProfitTracker = true;

    @ConfigEntry(
            id = "profitTrackerColorConfig",
            translation = "Profit Tracker Colors"
    )
    @Comment("Customize the color of each line in the Profit Tracker widget.")
    public static final ProfitTrackerColorConfig profitTrackerColorConfig = new ProfitTrackerColorConfig();

    @ConfigEntry(
            id = "profitTrackerConfig",
            translation = "Profit Tracker Config"
    )
    @Comment("Customize visibility, pricing, faction, and value calculations for the Profit Tracker.")
    public static final ProfitTrackerConfig profitTrackerConfig = new ProfitTrackerConfig();

    @ConfigOption.Separator("Kuudra Chests")

    @ConfigEntry(
            id = "chestCounterTracker",
            translation = "Chest Counter Tracker"
    )
    @Comment("Track percent toward the 60 chest cap.")
    public static boolean chestCounterTracker = true;

    @ConfigEntry(
            id = "chestCounterPartyAnnouncements",
            translation = "Chest Counter Party Announcements"
    )
    @Comment("Send reminders to party chat at milestones and when nearing the cap.")
    public static boolean chestCounterPartyAnnouncements = true;

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

    @ConfigOption.Separator("Kuudra Requeuing")

    @ConfigEntry(
            id = "autoRequeue",
            translation = "Auto Requeue"
    )
    @Comment("Automatically start a new Kuudra run after the boss is defeated.")
    public static boolean autoRequeue = true;

    @ConfigEntry(
            id = "autoRequeueConfig",
            translation = "Auto Requeue Config"
    )
    @Comment("Configure Auto Requeue options: delay, auto-stop and run time threshold.")
    public static final AutoRequeueConfig autoRequeueConfig = new AutoRequeueConfig();

    @ConfigObject
    public static class AutoRequeueConfig {

        @ConfigEntry(
                id = "requeueDelay",
                translation = "Auto Requeue Delay"
        )
        @Comment("Delay before requeueing (in ticks).")
        @ConfigOption.Range(min = 1, max = 50)
        @ConfigOption.Slider
        public static int requeueDelay = 20;

        @ConfigEntry(
                id = "autoStopAutoRequeue",
                translation = "Auto Stop Auto Requeue"
        )
        @Comment("Pause Auto Requeue when a completed run hits your configured overall time target.")
        public static boolean autoStopAutoRequeue = false;

        @ConfigEntry(
                id = "autoStopAutoRequeueOverallSeconds",
                translation = "Auto Stop Run Time (s)"
        )
        @Comment("Set the completed run time that will stop Auto Requeue (Current WR: 52s).")
        @ConfigOption.Range(min = 1, max = 180)
        @ConfigOption.Slider
        public static double autoStopAutoRequeueOverallSeconds = 51.0;
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
                id = "kuudraNotificationsSound",
                translation = "Kuudra Notifications Sound"
        )
        @Comment("Play a sound when a Kuudra notification is shown.")
        public static boolean kuudraNotificationsSound = true;

        @ConfigEntry(
                id = "kuudraNotificationBuildStarted",
                translation = "Build Started Notification"
        )
        @Comment("Show an alert when Elle asks to build the Ballista.")
        public static boolean buildStarted = false;

        @ConfigEntry(
                id = "kuudraNotificationIchorUsed",
                translation = "Ichor Used Notification"
        )
        @Comment("Show an alert when Ichor Pool is cast.")
        public static boolean ichorUsed = false;

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
        public static boolean phaseChange = false;

        @ConfigEntry(
                id = "kuudraNotificationBuildDone",
                translation = "Build Done Notification"
        )
        @Comment("Show an alert when the Ballista is fully built.")
        public static boolean buildDone = false;

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
                id = "placedSupplyTitle",
                translation = "Placed Supply"
        )
        @Comment("Show an alert when you place a supply.")
        public static boolean placedSupply = true;

        @ConfigEntry(
                id = "supplyPickedUpTitle",
                translation = "Supply Picked Up Notification"
        )
        @Comment("Show an alert when you finish picking up a supply.")
        public static boolean supplyPickedUp = true;
    }

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
            translation = "Phase Personal Best Tracker"
    )
    @Comment("Track your personal best time for each individual T5 Kuudra phase.")
    public static boolean phaseSplitsPBTracker = true;

    @ConfigOption.Separator("ETHERWARP WAYPOINTS")

    @ConfigEntry(
            id = "etherwarpHelper",
            translation = "Etherwarp Waypoints Helper"
    )
    @Comment("Render Etherwarp helper waypoints by Kuudra phase.")
    public static boolean etherwarpHelper = true;

    @ConfigButton(
            title = "Etherwarp Categories",
            text = "SELECT"
    )
    @Comment("Choose which Etherwarp categories are active.")
    @SuppressWarnings("unused")
    public static final Runnable openEtherwarpCategorySelector = () -> {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new EtherwarpCategorySelectorScreen(mc.screen)));
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
            Util.getPlatform().openFile(configDir.toFile());
        } catch (Exception ignored) {
        }
    };

    @ConfigOption.Separator("Visuals")

    @ConfigEntry(
            id = "hideUselessArmorStands",
            translation = "Hide Useless Armor Stands"
    )
    @Comment("Hide armor stands used for visual effects that don't provide useful information.")
    public static boolean hideUselessArmorStands = true;

    @ConfigEntry(
            id = "hideUselessArmorStandsConfig",
            translation = "Hide Useless Armor Stands Config"
    )
    @Comment("")
    public static final HideUselessArmorStandsConfig hideUselessArmorStandsConfig = new HideUselessArmorStandsConfig();

    @ConfigObject
    public static class HideUselessArmorStandsConfig {

        @ConfigEntry(id = "build", translation = "Build Area")
        @Comment("Hide the dropped balista pieces around the build area.")
        public static boolean build = true;

        @ConfigEntry(id = "rightCannon", translation = "Right Cannon")
        @Comment("Hide the armour stand from right cannon.")
        public static boolean rightCannon = false;

        @ConfigEntry(id = "leftCannon", translation = "Left Cannon")
        @Comment("Hide the armour stand from left cannon.")
        public static boolean leftCannon = false;

        @ConfigEntry(id = "shop", translation = "Shop Area")
        @Comment("Hide the shop armour stand near Triangle.")
        public static boolean shop = true;

        @ConfigEntry(id = "others", translation = "Other Areas")
        @Comment("Hide other armour stands like bonemerangs, pets, etc.")
        public static boolean others = false;

        public static boolean anyEnabled() {
            return build || rightCannon || leftCannon || shop || others;
        }
    }

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
        @Comment("Preset best-possible Supplies split used in Pace calculation.")
        public static double supplies = 22.5;

        @ConfigEntry(id = "build", translation = "Build Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible Build split used in Pace calculation.")
        public static double build = 12.5;

        @ConfigEntry(id = "eaten", translation = "Eaten Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible Eaten split used in Pace calculation.")
        public static double eaten = 4.4;

        @ConfigEntry(id = "stun", translation = "Stun Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible Stun split used in Pace calculation.")
        public static double stun = 0.0;

        @ConfigEntry(id = "dps", translation = "DPS Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible DPS split used in Pace calculation.")
        public static double dps = 3.5;

        @ConfigEntry(id = "skip", translation = "Skip Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible Skip split used in Pace calculation.")
        public static double skip = 4.6;

        @ConfigEntry(id = "boss", translation = "Boss Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        @Comment("Preset best-possible Boss split used in Pace calculation.")
        public static double boss = 2.0;
    }

    @ConfigEntry(
            id = "hideKuudraBossBar",
            translation = "Hide Kuudra Boss Bar"
    )
    @Comment("Hide Kuudra's vanilla boss bar during runs.")
    public static boolean hideKuudraBossBar = false;

    @ConfigObject
    public static class ProfitTrackerConfig {
        @ConfigEntry(id = "profitTrackerVisibility", translation = "Profit Tracker Visibility")
        @ConfigOption.Select
        @Comment("Control when the profit tracker is visible.")
        public static ProfitTrackerVisibility profitTrackerVisibility = ProfitTrackerVisibility.KUUDRA_AREAS;

        @ConfigEntry(id = "renderDuringRun", translation = "Render Widget During Run")
        @Comment("Show the widget while a run is active. When disabled, the widget only appears after the run is completed.")
        public static boolean renderDuringRun = false;

        @ConfigEntry(id = "bazaarPricingMode", translation = "Bazaar Pricing Mode")
        @ConfigOption.Select
        @Comment("Choose how Bazaar prices are calculated for profit and chest value.")
        public static BazaarPricingMode bazaarPricingMode = BazaarPricingMode.SELL_ORDER;

        @ConfigEntry(id = "crimsonIsleFaction", translation = "Crimson Isle Faction")
        @ConfigOption.Select
        @Comment("Select your Crimson Isle faction to improve profit calculations.")
        public static CrimsonFaction crimsonIsleFaction = CrimsonFaction.MAGE;

        @ConfigEntry(id = "armorValueType", translation = "Armor Value Type")
        @ConfigOption.Select
        @Comment("Choose how armor value is calculated in the profit tracker.")
        public static ArmorValueType armorValueType = ArmorValueType.SALVAGE;

        @ConfigEntry(id = "kuudraPetBonus", translation = "Kuudra Pet Bonus")
        @ConfigOption.Range(min = 0, max = 20)
        @ConfigOption.Slider
        @Comment("Apply your Kuudra pet level bonus to profit calculations.")
        public static int kuudraPetBonus = 20;
    }

    @ConfigObject
    public static class ProfitTrackerColorConfig {

        @ConfigEntry(id = "titleColor", translation = "Title Color")
        @ConfigOption.Select
        @Comment("Color of the 'Profit Tracker' title text.")
        public static TextColor titleColor = TextColor.AQUA;

        @ConfigEntry(id = "profitLabelColor", translation = "Profit Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Profit:' label.")
        public static TextColor profitLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "runsLabelColor", translation = "Runs Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Runs:' label.")
        public static TextColor runsLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "chestsLabelColor", translation = "Chests Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Chests:' label.")
        public static TextColor chestsLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "rerollsLabelColor", translation = "Rerolls Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Rerolls:' label.")
        public static TextColor rerollsLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "avgTimeLabelColor", translation = "Avg Time Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Avg Time:' label.")
        public static TextColor avgTimeLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "timeLabelColor", translation = "Time Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Time:' label.")
        public static TextColor timeLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "rateLabelColor", translation = "Rate Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Rate:' label.")
        public static TextColor rateLabelColor = TextColor.WHITE;

        @ConfigEntry(id = "trackingLabelColor", translation = "Tracking Label Color")
        @ConfigOption.Select
        @Comment("Color of the 'Tracking:' label.")
        public static TextColor trackingLabelColor = TextColor.WHITE;
    }

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
