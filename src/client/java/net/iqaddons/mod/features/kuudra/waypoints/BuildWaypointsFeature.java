package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuildWaypointsFeature extends KuudraFeature {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("PROGRESS:\\s*(?:§.)?(\\d+)%");

    private static final RenderColor COLOR_0_20 = new RenderColor(168, 0, 0, 255);
    private static final RenderColor COLOR_21_40 = new RenderColor(255, 0, 0, 255);
    private static final RenderColor COLOR_41_60 = new RenderColor(255, 135, 0, 255);
    private static final RenderColor COLOR_61_80 = new RenderColor(46, 130, 0, 255);
    private static final RenderColor COLOR_81_100 = new RenderColor(125, 218, 88, 255);

    private final List<BuildPile> buildPiles = new CopyOnWriteArrayList<>();

    public BuildWaypointsFeature() {
        super(
                "buildOverlay",
                "Build Overlay",
                () -> PhaseTwoConfig.buildHelper,
                KuudraPhase.BUILD
        );
    }

    @Override
    protected void onKuudraActivate() {
        buildPiles.clear();

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
        subscribe(ArmorStandRenderEvent.class, this::onArmorStandRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        buildPiles.clear();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(2)) return;

        List<BuildPile> newPiles = EntityDetectorUtil.getAllArmorStands()
                .stream()
                .filter(this::isProgressStand)
                .map(this::createBuildPile)
                .filter(Objects::nonNull)
                .toList();

        buildPiles.clear();
        buildPiles.addAll(newPiles);
    }

    private void onArmorStandRender(@NotNull ArmorStandRenderEvent event) {
        if (!PhaseTwoConfig.hideDefaultBuildPileText) return;
        if (!PhaseTwoConfig.hideDefaultBuildPileTextConfig.anyEnabled()) return;

        var state = event.getRenderState();
        if (state == null || state.nameTag == null) return;

        String stripped = state.nameTag.getString().replaceAll("§.", "");

        boolean shouldHide = false;

        if (PhaseTwoConfig.hideDefaultBuildPileTextConfig.progress &&
                stripped.contains("PROGRESS:") && stripped.contains("%")) {
            shouldHide = true;
        }

        if (PhaseTwoConfig.hideDefaultBuildPileTextConfig.sneakPunch &&
                stripped.contains("SNEAK") && stripped.contains("PUNCH")) {
            shouldHide = true;
        }

        if (PhaseTwoConfig.hideDefaultBuildPileTextConfig.supplyPile &&
                stripped.contains("SUPPLY PILE")) {
            shouldHide = true;
        }

        if (shouldHide) {
            event.setCancelled(true);
        }
    }

    private boolean isProgressStand(@NotNull ArmorStand stand) {
        if (!stand.hasCustomName() || stand.getCustomName() == null) {
            return false;
        }

        String name = stand.getCustomName().getString();
        return name.contains("PROGRESS:") && name.contains("%");
    }

    private @Nullable BuildPile createBuildPile(@NotNull ArmorStand stand) {
        String name = Objects.requireNonNull(stand.getCustomName()).getString();
        int progress = extractProgress(name);
        if (progress < 0) return null;

        return new BuildPile(
                new Vec3(stand.getX(), stand.getY(), stand.getZ()),
                name,
                progress
        );
    }

    private int extractProgress(@NotNull String name) {
        String stripped = name.replaceAll("§.", "");
        Matcher matcher = PROGRESS_PATTERN.matcher(stripped);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                String numbers = stripped.replaceAll("[^0-9]", "");
                if (!numbers.isEmpty()) {
                    try {
                        return Integer.parseInt(numbers);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return -1;
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        for (BuildPile pile : buildPiles) {
            var progressColor = getColorForProgress(pile.progress);

            Vec3 beaconPos = new Vec3(pile.position.x - 0.5, pile.position.y, pile.position.z - 0.5);
            event.drawStyledWithBeam(AABB.unitCubeFromLowerCorner(beaconPos), 25, false,
                    progressColor.withOpacity(PhaseTwoConfig.buildHelperOpacity), WorldRenderUtils.RenderStyle.BOTH
            );

            Vec3 textPos = new Vec3(pile.position.x, pile.position.y + 2.0, pile.position.z);
            event.drawText(textPos, Component.literal(pile.displayName), 0.05f, true, progressColor);
        }
    }

    private RenderColor getColorForProgress(int progress) {
        if (progress <= 20) return COLOR_0_20;
        if (progress <= 40) return COLOR_21_40;
        if (progress <= 60) return COLOR_41_60;
        if (progress <= 80) return COLOR_61_80;
        return COLOR_81_100;
    }

    private record BuildPile(
            Vec3 position,
            String displayName,
            int progress
    ) {}
}



