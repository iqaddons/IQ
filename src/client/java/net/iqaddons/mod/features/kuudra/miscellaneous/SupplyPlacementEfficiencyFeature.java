package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class SupplyPlacementEfficiencyFeature extends KuudraFeature {

    private long lastPickupMillis = -1L;

    public SupplyPlacementEfficiencyFeature() {
        super(
                "supplyPlacementEfficiency",
                "Supply Placement Efficiency",
                () -> PhaseOneConfig.supplyPlacementEfficiency,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        lastPickupMillis = -1L;

        subscribe(SupplyPickupEvent.class, this::onSupplyPickup);
        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
        subscribe(SupplyDropEvent.class, this::onSupplyDrop);
    }

    @Override
    protected void onKuudraDeactivate() {
        lastPickupMillis = -1L;
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        lastPickupMillis = event.pickupAt();
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (lastPickupMillis < 0L || !isLocalPlayer(event.playerName())) {
            return;
        }

        double elapsedSeconds = (System.currentTimeMillis() - lastPickupMillis) / 1000.0;
        MessageUtil.PARTY.sendMessage(String.format(
                Locale.US,
                "[IQ] Supply placed %.2fs after pickup. %s",
                elapsedSeconds,
                getEfficiencyRating(elapsedSeconds)
        ));
        lastPickupMillis = -1L;
    }

    private @NotNull String getEfficiencyRating(double elapsedSeconds) {
        if (elapsedSeconds <= 0.10) return "PERFECT!";
        if (elapsedSeconds <= 0.30) return "EXCELLENT!";
        if (elapsedSeconds <= 0.50) return "GREAT!";
        if (elapsedSeconds <= 0.70) return "GOOD!";
        if (elapsedSeconds <= 1.00) return "LATE!";
        return "VERY LATE!";
    }

    private void onSupplyDrop(@NotNull SupplyDropEvent event) {
        if (isLocalPlayer(event.playerName())) {
            lastPickupMillis = -1L;
        }
    }

    private boolean isLocalPlayer(@Nullable String playerName) {
        if (mc.player == null || playerName == null || playerName.isBlank()) {
            return false;
        }

        return StringUtils.stripFormatting(playerName).equalsIgnoreCase(mc.player.getName().getString());
    }
}
