package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.supply.SupplyPosition;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SecondSupplyAlertFeature extends KuudraFeature {

    private static final String ELLE_HEAD_OVER_MESSAGE = "[NPC] Elle: Head over to the main platform";
    private static final String ELLE_NOT_AGAIN_MESSAGE = "[NPC] Elle: Not again!";

    private static final Vec3d TRIANGLE_ZONE = new Vec3d(-67.5, 77, -122.5);
    private static final Vec3d X_ZONE = new Vec3d(-134.5, 77, -138.5);
    private static final Vec3d SLASH_ZONE = new Vec3d(-111.5, 76, -68.5);
    private static final double ZONE_RADIUS = 20.0;

    private volatile boolean inTriangle = false;
    private volatile boolean inX = false;
    private volatile boolean inSlash = false;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private final ScheduledExecutorService scheduler;

    public SecondSupplyAlertFeature(ScheduledExecutorService scheduler) {
        super(
                "secondSupplyAlert",
                "Second Supply Alert",
                () -> PhaseOneConfig.secondSupplyAlert,
                KuudraPhase.SUPPLIES
        );

        this.scheduler = scheduler;
    }

    @Override
    protected void onKuudraActivate() {
        resetState();
        subscribe(ChatReceivedEvent.class, this::onChat);
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
            if (mc.player == null) return;

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
            return;
        }

        if (message.contains(ELLE_NOT_AGAIN_MESSAGE)) {
            scheduler.schedule(this::checkSecondSupply, 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void checkSecondSupply() {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();

        for (SupplyPosition supply : supplies) {
            String alert = getSecondSupplyAlert(supply);
            if (alert != null) {
                mc.execute(() -> MessageUtil.PARTY.sendMessage(alert));
                return;
            }
        }
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
    }
}