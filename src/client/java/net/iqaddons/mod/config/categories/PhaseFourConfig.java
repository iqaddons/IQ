package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigObject;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category(
        value = "Phase 4 - Boss Fight"
)
public class PhaseFourConfig {

    @ConfigOption.Separator("Boss Alerts")
    @ConfigEntry(
            id = "rendDamageAlert",
            translation = "Rend Damage"
    )
    @Comment("Show an alert when any teammate deals Rend damage")
    public static boolean rendDamageAlert = true;

    @ConfigEntry(
            id = "kuudraDirectionAlert",
            translation = "Kuudra Direction Alert"
    )
    @Comment("Show an alert indicating which side Kuudra will spawn on")
    public static boolean kuudraDirectionAlert = true;


    @ConfigEntry(
            id = "iceSprayAlert",
            translation = "Ice Spray Alert"
    )
    @Comment("Announce your Ice Spray usage time in party chat during the boss phase")
    public static boolean iceSprayAlert = true;

    @ConfigEntry(
            id = "backboneAlert",
            translation = "Backbone Alert"
    )
    @Comment("Track Bonemerang backbone timing with a HUD percent bar and Rend alert")
    public static boolean backboneAlert = true;

    @ConfigEntry(
            id = "backboneAlertSound",
            translation = "Backbone Alert Sound"
    )
    @Comment("Play a sound when the Backbone Alert is active")
    public static boolean backboneAlertSound = true;

    @ConfigEntry(
            id = "backboneAdvanceTicks",
            translation = "Backbone Advance (in ticks)"
    )
    @ConfigOption.Range(min = -10, max = 10)
    @ConfigOption.Slider
    @Comment("Adjust Backbone timing in ticks. (0 = recommended, negative = slow, positive = fast)")
    public static int backboneAlertAdvanceTicks = 0;

    @ConfigEntry(
            id = "dangerZoneAlert",
            translation = "Danger Zone Alert"
    )
    @Comment("Show an alert when you enter a tentacle's danger zone")
    public static boolean dangerZoneAlert = false;

    @ConfigEntry(
            id = "ichorPoolArea",
            translation = "Render Ichor Pool Area"
    )
    @Comment("Render the area of effect of the ichor pools on the ground")
    public static boolean ichorPoolArea = true;

    @ConfigEntry(
            id = "kuudraDistanceDisplay",
            translation = "Kuudra Distance Display"
    )
    @Comment("Show your distance from Kuudra inside the Magma Cube")
    public static boolean kuudraDistanceDisplay = false;

    @ConfigEntry(
            id = "kuudraDistanceConfig",
            translation = "Kuudra Distance Config"
    )
    @Comment("Configure the green distance range and Throw Bone alert")
    public static final KuudraDistanceConfig kuudraDistanceConfig = new KuudraDistanceConfig();

    @ConfigObject
    public static class KuudraDistanceConfig {

        @ConfigEntry(
                id = "throwBoneAlert",
                translation = "Throw Bone Alert"
        )
        @Comment("Show a small \"Throw Bone\" title when your Kuudra distance is in the green range")
        public static boolean throwBoneAlert = true;

        @ConfigEntry(
                id = "greenMinDistance",
                translation = "Green Min Distance"
        )
        @ConfigOption.Range(min = 0, max = 30)
        @ConfigOption.Slider
        @Comment("Minimum horizontal distance from Kuudra that counts as green")
        public static double greenMinDistance = 12.0d;

        @ConfigEntry(
                id = "greenMaxDistance",
                translation = "Green Max Distance"
        )
        @ConfigOption.Range(min = 0, max = 30)
        @ConfigOption.Slider
        @Comment("Maximum horizontal distance from Kuudra that counts as green")
        public static double greenMaxDistance = 14.0d;
    }

    @ConfigOption.Separator("VISUALS")

    @ConfigEntry(
            id = "hideDamageTitle",
            translation = "Hide Kuudra Damage Title"
    )
    @Comment("Hide Kuudra's default damage title (e.g. ☠ 240M/240M❤)")
    public static boolean hideDamageTitle = true;

}
