package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category(
        value = "Kuudra Utilities"
)
public class KuudraGeneralConfig {

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
}
