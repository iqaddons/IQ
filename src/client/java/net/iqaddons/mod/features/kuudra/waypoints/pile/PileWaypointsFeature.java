package net.iqaddons.mod.features.kuudra.waypoints.pile;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.supply.PileLocation;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.RenderColor;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class PileWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final int BEACON_HEIGHT = 40;

    private static final RenderColor NORMAL_COLOR = new RenderColor(1.0f, 1.0f, 1.0f, 0.28f);
    private static final RenderColor NO_PRE_COLOR = new RenderColor(0.0f, 1.0f, 0.0f, 0.40f);

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public PileWaypointsFeature() {
        super(
                "pileWaypoints",
                "Pile Waypoints",
                () -> Configuration.PhaseOneConfig.pileWaypoints,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        supplyState.reset();

        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onRender));
        subscribe(EventBus.subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange));

        log.info("Pile waypoints activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Pile waypoints deactivated");
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyState.reset();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(UPDATE_INTERVAL_TICKS)) return;

        for (ArmorStandEntity stand : EntityDetectorUtil.getCompletedPileStands()) {
            Vec3d standPos = new Vec3d(stand.getX(), stand.getY(), stand.getZ());
            supplyState.markPileCompleted(standPos);
        }
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        List<PileLocation> piles = supplyState.getRemainingPiles();
        if (piles.isEmpty()) return;

        int missingPre = supplyState.getMissingPre();

        for (PileLocation pile : piles) {
            RenderColor color = pile.isNoPrePile(missingPre) ? NO_PRE_COLOR : NORMAL_COLOR;
            event.drawBeam(pile.position(), BEACON_HEIGHT, true, color);
        }
    }
}


