package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.EtherwarpCategoryToggleManager;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Feature Etherwarp Helper - Renderiza waypoints estratégicos como block highlights
 * que variam dinamicamente de acordo com a fase da run de Kuudra.
 */
@Slf4j
public class EtherwarpHelperFeature extends KuudraFeature {

    private static final double OUTLINE_STEP = 0.0035;

    private final EtherwarpConfigLoader configLoader = EtherwarpConfigLoader.get();
    private final EtherwarpCategoryToggleManager toggleManager = EtherwarpCategoryToggleManager.get();
    private volatile List<EtherwarpWaypoint> visibleWaypoints = new CopyOnWriteArrayList<>();
    private volatile KuudraPhase cachedPhase = KuudraPhase.NONE;

    public EtherwarpHelperFeature() {
        super(
                "etherwarpHelper",
                "Etherwarp Helper",
                () -> KuudraGeneralConfig.etherwarpHelper,
                KuudraPhase.SUPPLIES, KuudraPhase.BUILD, KuudraPhase.EATEN,
                KuudraPhase.STUN, KuudraPhase.DPS, KuudraPhase.SKIP
        );
    }

    @Override
    protected void onKuudraActivate() {
        log.debug("Etherwarp Helper activated");

        // Carrega configuração inicial
        reloadConfig();

        // Subscribe aos eventos
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        log.debug("Etherwarp Helper deactivated");
        visibleWaypoints = new CopyOnWriteArrayList<>();
        cachedPhase = KuudraPhase.NONE;
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        log.debug("Phase changed from {} to {} - Updating visible waypoints", event.previousPhase(), event.currentPhase());
        updateVisibleWaypoints(event.currentPhase());
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (mc.player == null || visibleWaypoints.isEmpty()) {
            return;
        }

        KuudraPhase phase = currentPhase();
        if (phase != cachedPhase) {
            updateVisibleWaypoints(phase);
        }

        Set<String> renderedThisFrame = new HashSet<>();
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (EtherwarpWaypoint waypoint : visibleWaypoints) {
            if (!waypoint.shouldShowInPhase(phase)) {
                continue;
            }

            for (int i = 0; i < waypoint.positions().size(); i++) {
                Vec3d pos = waypoint.positions().get(i);
                if (waypoint.maxRenderDistance() > 0
                        && playerPos.distanceTo(pos) > waypoint.maxRenderDistance()) {
                    continue;
                }

                if (!renderedThisFrame.add(waypoint.getUniqueId(pos))) {
                    continue;
                }

                renderWaypoint(event, waypoint, pos, i);
            }
        }
    }

    /**
     * Atualiza a lista de waypoints visíveis para a fase atual.
     */
    private void updateVisibleWaypoints(@NotNull KuudraPhase phase) {
        List<EtherwarpWaypoint> newVisibleWaypoints = new ArrayList<>();

        for (EtherwarpCategory category : configLoader.getCached()) {
            if (!category.enabled() || !toggleManager.isCategoryEnabled(category.name())) {
                continue;
            }

            for (EtherwarpWaypoint waypoint : category.waypoints()) {
                if (waypoint.shouldShowInPhase(phase)) {
                    newVisibleWaypoints.add(waypoint);
                }
            }
        }

        visibleWaypoints = new CopyOnWriteArrayList<>(newVisibleWaypoints);
        cachedPhase = phase;
        log.debug("Updated visible waypoints for phase {}: {} waypoints", phase.name(), newVisibleWaypoints.size());
    }

    /**
     * Renderiza um waypoint individual.
     */
    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull EtherwarpWaypoint waypoint, @NotNull Vec3d pos, int colorIndex) {
        Box box = waypoint.getRenderBox(pos);

        RenderColor color = RenderColor.fromHex(waypoint.getColorForIndex(colorIndex)).withOpacity(waypoint.alpha());

        if (waypoint.renderStyle() == WorldRenderUtils.RenderStyle.SOLID) {
            event.drawFilled(box, true, color);
            return;
        }

        if (waypoint.renderStyle() == WorldRenderUtils.RenderStyle.BOTH) {
            event.drawFilled(box, true, color.withOpacity(color.a * 0.5f));
        }

        drawOutlineWithThickness(event, box, color, waypoint.lineWidth());
    }

    private void drawOutlineWithThickness(@NotNull WorldRenderEvent event, @NotNull Box box, @NotNull RenderColor color, float lineWidth) {
        int layers = Math.max(1, Math.round(lineWidth));
        event.drawOutline(box, true, color);

        for (int i = 1; i < layers; i++) {
            double grow = OUTLINE_STEP * i;
            event.drawOutline(box.expand(grow), true, color);
        }
    }

    /**
     * Recarrega a configuração de waypoints.
     * Chamado pelo sistema /iq reload.
     */
    public void reloadConfig() {
        log.info("Reloading Etherwarp Helper config");
        List<EtherwarpCategory> categories = configLoader.reload();
        toggleManager.syncWithCategories(categories);
        updateVisibleWaypoints(currentPhase());
    }
}
