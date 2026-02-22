package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category(
        value = "Phase 4 - Boss Fight"
)
public class PhaseFourConfig {

    @ConfigEntry(
            id = "hideDamageTitle",
            translation = "Hide Kuudra Damage Title"
    )
    @Comment("Hide Kuudra's default damage title (e.g. ☠ 240M/240M❤).")
    public static boolean hideDamageTitle = true;

    @ConfigOption.Separator("Boss Alerts")
    @ConfigEntry(
            id = "kuudraDirectionAlert",
            translation = "Kuudra Direction Alert"
    )
    @Comment("Show an alert indicating which side Kuudra will spawn on.")
    public static boolean kuudraDirectionAlert = true;

    @ConfigEntry(
            id = "rendDamageAlert",
            translation = "Rend Damage"
    )
    @Comment("Show an alert when any teammate deals Rend damage.")
    public static boolean rendDamageAlert = true;

    @ConfigEntry(
            id = "backboneAlert",
            translation = "Backbone Alert"
    )
    @Comment("Track Bonemerang backbone timing with a HUD progress bar and Rend alert.")
    public static boolean backboneAlert = true;

    @ConfigEntry(
            id = "backboneAlertSound",
            translation = "Backbone Alert Sound"
    )
    @Comment("Play a sound when the Backbone Alert is active.")
    public static boolean backboneAlertSound = true;

    @ConfigEntry(
            id = "dangerZoneAlert",
            translation = "Danger Zone Alert"
    )
    @Comment("Show an alert when you enter a tentacle's danger zone.")
    public static boolean dangerZoneAlert = false;

}