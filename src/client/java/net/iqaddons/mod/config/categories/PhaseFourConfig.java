package net.iqaddons.mod.config.categories;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

@Category(
        value = "Phase 4 - Boss Fight"
)
public class PhaseFourConfig {

    @ConfigEntry(
            id = "kuudraDirectionAlert",
            translation = "Kuudra Direction Alert"
    )
    @Comment("Alert which side Kuudra will appear on")
    public static boolean kuudraDirectionAlert = true;

    @ConfigEntry(
            id = "rendDamageAlert",
            translation = "Rend Damage"
    )
    @Comment("Track when any teammate deals Rend Damage")
    public static boolean rendDamageAlert = true;

    @ConfigEntry(
            id = "dangerZoneAlert",
            translation = "Danger Zone Alert"
    )
    @Comment("Alert when you are in Tentacle's Danger Zone")
    public static boolean dangerZoneAlert = true;

}