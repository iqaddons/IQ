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
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@Slf4j
public class PileWaypointsFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final int BEACON_HEIGHT = 40;
    private static final float PLACE_AREA_HITBOX_THICKNESS = 0.08f;
    private static final int PLACE_AREA_CIRCLE_SEGMENTS = 60;

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
        supplyState.resetRemainingPiles();

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        supplyState.clearRemainingPiles();
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyState.resetRemainingPiles();
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

            if (PhaseOneConfig.PileWaypointsConfig.placeAreaHitbox) {
                renderPlaceAreaHitbox(event, pile);
            }

            if (PhaseOneConfig.PileWaypointsConfig.pileWaypointNames) {
                event.drawText(pile.position().add(0.5, 2.5, 0.5),
                        Component.literal(pile.name().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD), 0.05f,
                        true, RenderColor.fromArgb(PhaseOneConfig.PileWaypointsConfig.pileWaypointNameColor)
                );
            }
        }
    }

    private void renderPlaceAreaHitbox(@NotNull WorldRenderEvent event, @NotNull PileLocation pile) {
        double radius = PileLocation.PLACE_AREA_RADIUS;
        Vec3 center = pile.getPlaceAreaCenter();
        boolean playerInside = mc.player != null && pile.isInsidePlaceArea(mc.player.position());
        RenderColor color = RenderColor.fromArgb(playerInside
                ? PhaseOneConfig.PileWaypointsConfig.placeAreaHitboxActiveColor
                : PhaseOneConfig.PileWaypointsConfig.placeAreaHitboxColor
        );
        boolean throughWalls = PhaseOneConfig.PileWaypointsConfig.placeAreaHitboxThroughWalls;

        event.drawFilledCircle(center, (float) radius, PLACE_AREA_CIRCLE_SEGMENTS,
                throughWalls, color.withOpacity(playerInside ? 0.28f : 0.16f));
        event.drawThickCircleOutline(center, (float) radius,
                playerInside ? PLACE_AREA_HITBOX_THICKNESS * 1.5f : PLACE_AREA_HITBOX_THICKNESS,
                PLACE_AREA_CIRCLE_SEGMENTS, throughWalls, color);
    }

}
