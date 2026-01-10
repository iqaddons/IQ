package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.supply.PreSpot;
import net.iqaddons.mod.utils.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NoPreAlertFeature extends KuudraFeature {

    private static final String ELLE_FISHING_MESSAGE = "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!";
    private static final long PRE_SPOT_DETECTION_DELAY_MS = 9000;
    private static final long SUPPLY_CHECK_DELAY_MS = 11500;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final ScheduledExecutorService scheduler;

    public NoPreAlertFeature(ScheduledExecutorService scheduler) {
        super(
                "noPreAlert",
                "No Pre Alert",
                () -> Configuration.PhaseOneConfig.noPreAlert,
                KuudraPhase.SUPPLIES
        );

        this.scheduler = scheduler;
    }

    @Override
    protected void onKuudraActivate() {
        supplyState.reset();
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
        log.info("No Pre Alert activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("No Pre Alert deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!event.getStrippedMessage().contains(ELLE_FISHING_MESSAGE)) {
            return;
        }

        supplyState.startSuppliesPhase();
        scheduler.schedule(this::detectPreSpot, PRE_SPOT_DETECTION_DELAY_MS, TimeUnit.MILLISECONDS);
        scheduler.schedule(this::checkSupplies, SUPPLY_CHECK_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void detectPreSpot() {
        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getEntityPos();
        boolean detected = supplyState.tryDetectPreSpot(playerPos);

        if (!detected) {
            mc.execute(() -> ChatUtil.ERROR.sendMessage("Could not determine your pre spot (too far away?)"));
        }
    }

    private void checkSupplies() {
        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) return;

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            mc.execute(() -> ChatUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!"));
            return;
        }

        Boolean hasSecondary = supplyState.hasSecondarySupply();
        if (hasSecondary != null && !hasSecondary && preSpot.getSecondaryName() != null) {
            mc.execute(() -> ChatUtil.PARTY.sendMessage("No " + preSpot.getSecondaryName() + "!"));
        }
    }
}
