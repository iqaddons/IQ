package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.PileConfigLoader;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class PileWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final int BEACON_HEIGHT = 40;

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public PileWaypointsFeature() {
        super(
                "pileWaypoints",
                "Pile Waypoints",
                () -> PhaseOneConfig.pileWaypoints,
                KuudraPhase.SUPPLIES
        );

        PileConfigLoader.get().load();
    }

    @Override
    protected void onKuudraActivate() {
        supplyState.reset();

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        supplyState.getRemainingPiles().clear();
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

        List<ArmorStand> completedStands = EntityDetectorUtil.getCompletedPileStands();
        for (ArmorStand stand : completedStands) {
            Vec3 standPos = new Vec3(stand.getX(), stand.getY(), stand.getZ());
            supplyState.markPileCompleted(standPos);
        }
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        List<PileLocation> piles = supplyState.getRemainingPiles();
        if (piles.isEmpty()) return;

        int missingPre = supplyState.getMissingPre();
        for (PileLocation pile : piles) {
            RenderColor color = pile.isNoPrePile(missingPre)
                    ? RenderColor.fromArgb(PhaseOneConfig.PileWaypointsConfig.noPrePileColor)
                    : RenderColor.fromArgb(PhaseOneConfig.PileWaypointsConfig.normalPileColor);

            event.drawStyledWithBeam(AABB.unitCubeFromLowerCorner(pile.position()), BEACON_HEIGHT,
                    false, color, WorldRenderUtils.RenderStyle.BOTH
            );

            if (PhaseOneConfig.PileWaypointsConfig.pileWaypointNames) {
                event.drawText(pile.position().add(0, 2.5, 0),
                        Component.literal(pile.name()), 0.05f,
                        true, color.withOpacity(100)
                );
            }
        }
    }
}
