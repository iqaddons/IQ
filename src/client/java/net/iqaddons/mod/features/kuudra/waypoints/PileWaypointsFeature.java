package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.PileLocation;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
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
                () -> PhaseOneConfig.pileWaypoints,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        supplyState.reset();

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);

        log.info("Pile waypoints activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        supplyState.getRemainingPiles().clear();
        log.info("Pile waypoints deactivated");
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyState.reset();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(UPDATE_INTERVAL_TICKS)) return;

        List<ArmorStandEntity> completedStands = EntityDetectorUtil.getCompletedPileStands();
        for (ArmorStandEntity stand : completedStands) {
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

            event.drawStyledWithBeam(Box.from(pile.position()), BEACON_HEIGHT,
                    false, color, WorldRenderUtils.RenderStyle.BOTH);
            event.drawText(pile.position().add(0, 2.5, 0),
                    Text.literal(pile.name()), 0.1f,
                    false, color.withOpacity(100)
            );
        }
    }
}


