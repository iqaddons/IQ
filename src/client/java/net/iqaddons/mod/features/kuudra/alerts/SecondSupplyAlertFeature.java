package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.iqaddons.mod.IQConstants.ELLE_HEAD_OVER_MESSAGE;

@Slf4j
public class SecondSupplyAlertFeature extends KuudraFeature {

    private static final int SUPPLY_SCAN_INTERVAL_TICKS = 2;
    private static final long NO_PRE_FALLBACK_DELAY_MS = 10_000L;
    private static final long SECOND_ALERT_DELAY_MS = 500L;

    private static final Vec3d TRIANGLE_ZONE = new Vec3d(-67.5, 77, -122.5);
    private static final Vec3d X_ZONE = new Vec3d(-134.5, 77, -138.5);
    private static final Vec3d SLASH_ZONE = new Vec3d(-111.5, 76, -68.5);
    private static final double ZONE_RADIUS = 20.0;

    private volatile boolean inTriangle = false;
    private volatile boolean inX = false;
    private volatile boolean inSlash = false;
    private volatile boolean secondSupplyCheckCompleted = false;
    private volatile boolean carrierScheduleAttempted = false;
    private volatile boolean timedScheduleAttempted = false;

    private volatile Long scheduledCheckAtMillis = null;
    private volatile String scheduledTrigger = null;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public SecondSupplyAlertFeature() {
        super(
                "secondSupplyAlert",
                "Second Supply Alert",
                () -> PhaseOneConfig.secondSupplyAlert,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        resetState();
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        resetState();
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra() || event.isExitingKuudra()) {
            resetState();
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(ELLE_HEAD_OVER_MESSAGE)) {
            detectZoneFromPlayerPosition();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(SUPPLY_SCAN_INTERVAL_TICKS)) return;
        if (secondSupplyCheckCompleted || currentPhase() != KuudraPhase.SUPPLIES) return;

        detectZoneFromPlayerPosition();
        syncScheduleWithNoPreAlert();

        if (scheduledCheckAtMillis == null) {
            List<SupplyPosition> supplies = updateSupplyPositions();
            if (!carrierScheduleAttempted && !supplies.isEmpty()) {
                carrierScheduleAttempted = true;
                scheduleSecondSupplyCheck(System.currentTimeMillis() + SECOND_ALERT_DELAY_MS,
                        "supplier giant spawn fallback");
            } else if (!timedScheduleAttempted && supplyState.getElapsedTimeMillis() >= NO_PRE_FALLBACK_DELAY_MS) {
                timedScheduleAttempted = true;
                scheduleSecondSupplyCheck(System.currentTimeMillis() + SECOND_ALERT_DELAY_MS,
                        "10.0s fallback timer");
            }
        }

        if (scheduledCheckAtMillis != null && System.currentTimeMillis() >= scheduledCheckAtMillis) {
            secondSupplyCheckCompleted = true;
            String trigger = scheduledTrigger != null ? scheduledTrigger : "scheduled delay";
            mc.execute(() -> {
                checkSecondSupply();
                log.debug("Triggered second-supply check from {}", trigger);
            });
        }
    }

    private void syncScheduleWithNoPreAlert() {
        Long noPreCheckAtMillis = supplyState.getLastNoPreCheckAtMillis();
        if (noPreCheckAtMillis == null) return;

        scheduleSecondSupplyCheck(noPreCheckAtMillis + SECOND_ALERT_DELAY_MS,
                "No Pre Alert + 0.5s");
    }

    private void scheduleSecondSupplyCheck(long scheduledAtMillis, @NotNull String trigger) {
        if (scheduledCheckAtMillis == null || scheduledCheckAtMillis != scheduledAtMillis) {
            scheduledCheckAtMillis = scheduledAtMillis;
            scheduledTrigger = trigger;
            log.debug("Scheduled second-supply check from {} at {}", trigger, scheduledAtMillis);
        }
    }

    private void checkSecondSupply() {
        List<SupplyPosition> supplies = updateSupplyPositions();

        if (!hasTrackedZone()) {
            log.debug("Skipping second-supply alert because player zone could not be determined");
            return;
        }

        for (SupplyPosition supply : supplies) {
            String alert = getSecondSupplyAlert(supply);
            if (alert != null) {
                mc.execute(() -> MessageUtil.PARTY.sendMessage("[IQ] " + alert));
                return;
            }
        }
    }

    private @NotNull List<SupplyPosition> updateSupplyPositions() {
        List<SupplyPosition> supplies = EntityDetectorUtil.getSupplyCarriers().stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYaw(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(supplies);
        return supplies;
    }

    private void detectZoneFromPlayerPosition() {
        if (mc.player == null || hasTrackedZone()) return;

        Vec3d playerPos = mc.player.getEntityPos();
        if (isNear(playerPos, TRIANGLE_ZONE)) {
            inTriangle = true;
            log.debug("Player detected in Triangle zone");
        } else if (isNear(playerPos, X_ZONE)) {
            inX = true;
            log.debug("Player detected in X zone");
        } else if (isNear(playerPos, SLASH_ZONE)) {
            inSlash = true;
            log.debug("Player detected in Slash zone");
        }
    }

    private boolean hasTrackedZone() {
        return inTriangle || inX || inSlash;
    }

    private @Nullable String getSecondSupplyAlert(@NotNull SupplyPosition supply) {
        double x = supply.position().x;
        double z = supply.position().z;

        if (inTriangle && x > -90 && z < -128) {
            return formatAlert("Shop", supply);
        }

        if (inX && x < -127 && z > -134.5 && z < -108.5) {
            return formatAlert("X Cannon", supply);
        }

        if (inSlash && x < -128 && z > -95) {
            return formatAlert("Square", supply);
        }

        return null;
    }

    private @NotNull String formatAlert(@NotNull String name, @NotNull SupplyPosition supply) {
        return String.format("%s x: %.2f, y: 75, z: %.2f",
                name,
                supply.position().x,
                supply.position().z
        );
    }

    private boolean isNear(@NotNull Vec3d pos1, @NotNull Vec3d pos2) {
        return pos1.squaredDistanceTo(pos2) < ZONE_RADIUS * ZONE_RADIUS;
    }

    private void resetState() {
        inTriangle = false;
        inX = false;
        inSlash = false;
        secondSupplyCheckCompleted = false;
        carrierScheduleAttempted = false;
        timedScheduleAttempted = false;
        scheduledCheckAtMillis = null;
        scheduledTrigger = null;
    }
}