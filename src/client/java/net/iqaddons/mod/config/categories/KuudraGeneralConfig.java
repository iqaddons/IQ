package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import net.iqaddons.mod.model.profit.CrimsonFaction;

@Category(
        value = "Kuudra Utilities"
)
public class KuudraGeneralConfig {

    @ConfigEntry(
            id = "autoRequeue",
            translation = "Auto Requeue"
    )
    @Comment("Automatically requeue a Kuudra run after boss death.")
    public static boolean autoRequeue = false;

    @ConfigEntry(
            id = "requeueDelay",
            translation = "Auto Requeue Delay"
    )
    @Comment("Delay in ticks before auto-requeue (1-50)")
    @ConfigOption.Range(min = 1, max = 50)
    @ConfigOption.Slider
    public static int requeueDelay = 15;

    @ConfigOption.Separator("Profit Tracking")

    @ConfigEntry(
            id = "kuudraProfitTracker",
            translation = "Kuudra Profit Tracker"
    )
    @Comment("Track your profit/loss after each Kuudra run and display it")
    public static boolean kuudraProfitTracker = false;

    @ConfigEntry(
            id = "profitTrackerVisibility",
            translation = "Profit Tracker Visibility"
    )
    @ConfigOption.Select
    @Comment("Choose whether the profit tracker appears only in Kuudra-related areas or all the time")
    public static ProfitTrackerVisibility profitTrackerVisibility = ProfitTrackerVisibility.KUUDRA_AREAS;

    @ConfigEntry(
            id = "bazaarPricingMode",
            translation = "Bazaar Pricing Mode"
    )
    @ConfigOption.Select
    @Comment("Choose if Bazaar prices use Insta Sell or Sell Order for chest value and profit tracking")
    public static BazaarPricingMode bazaarPricingMode = BazaarPricingMode.INSTA_SELL;

    @ConfigEntry(
            id = "crimsonIsleFaction",
            translation = "Crimson Isle Faction"
    )
    @ConfigOption.Select
    @Comment("Set your Crimson Isle faction for better profit calculations")
    public static CrimsonFaction crimsonIsleFaction = CrimsonFaction.MAGE;


    @ConfigEntry(
            id = "armorValueType",
            translation = "Armor Value Type"
    )
    @ConfigOption.Select
    @Comment("Choose whether to value armor based on its salvage value or auction house value in the profit tracker")
    public static ArmorValueType armorValueType = ArmorValueType.SALVAGE;

    @ConfigEntry(
            id = "kuudraPetBonus",
            translation = "Kuudra Pet Bonus"
    )
    @ConfigOption.Range(min = 0, max = 20)
    @ConfigOption.Slider
    @Comment("Calculate the bonus from your Kuudra pet level in the profit tracker")
    public static int kuudraPetBonus = 0;

    @ConfigEntry(
            id = "chestValueWidget",
            translation = "Chest Value Display"
    )
    @Comment("Display the value of each chest you open in an overlay")
    public static boolean chestValueWidget = true;

    @ConfigEntry(
            id = "chestCounterTracker",
            translation = "Chest Counter Tracker"
    )
    @Comment("Track Kuudra runs toward your 60 chest cap")
    public static boolean chestCounterTracker = true;

    @ConfigEntry(
            id = "chestCounterPartyAnnouncements",
            translation = "Chest Counter Party Announcements"
    )
    @Comment("Send 10-run and cap reminders to party chat")
    public static boolean chestCounterPartyAnnouncements = true;

    @ConfigEntry(
            id = "customSplits",
            translation = "Custom Splits"
    )
    @Comment("Render a overlay with all the Kuudra phases times")
    public static boolean customSplits = true;

    @ConfigEntry(
            id = "customSplitsBenchmarks",
            translation = "Custom Splits Benchmarks"
    )
    @Comment("Configure target split times used for pace in the Custom Splits widget")
    public static final CustomSplitsBenchmarks customSplitsBenchmarks = new CustomSplitsBenchmarks();

    @ConfigObject
    public static class CustomSplitsBenchmarks {

        @ConfigEntry(id = "supplies", translation = "Supplies Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double supplies = 23.5;

        @ConfigEntry(id = "build", translation = "Build Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double build = 14.3;

        @ConfigEntry(id = "eaten", translation = "Eaten Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double eaten = 5.0;

        @ConfigEntry(id = "stun", translation = "Stun Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double stun = 0.0;

        @ConfigEntry(id = "dps", translation = "DPS Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double dps = 3.5;

        @ConfigEntry(id = "boss", translation = "Boss Benchmark")
        @ConfigOption.Range(min = 0, max = 60)
        @ConfigOption.Slider
        public static double boss = 4.5;
    }

    @ConfigEntry(
            id = "manaDrainNotify",
            translation = "Mana Drain Notify"
    )
    @Comment(" Send the amount of mana drained to party chat")
    public static boolean manaDrainNotify = true;

    @ConfigEntry(
            id = "personalBestTracker",
            translation = "Personal Best Tracker"
    )
    @Comment("Track your best Kuudra run and notify when you beat your PB")
    public static boolean personalBestTracker = true;

    @ConfigEntry(
            id = "kuudraPhaseAlert",
            translation = "Phase Alert"
    )
    @Comment("Alert when Kuudra phase changes")
    public static boolean kuudraPhaseAlert = true;

    @ConfigEntry(
            id = "hideMobNametags",
            translation = "Hide Mob Nametags"
    )
    @Comment("Prevent Kuudra mobs nametags from loading")
    public static boolean hideMobNametags = false;

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
