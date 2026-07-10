package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.generic.ArrowTrackerFeature;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ArrowTrackerWidget extends HudWidget {

    private final HudLine arrowLine = HudLine.of("§fNo Arrows");

    public ArrowTrackerWidget() {
        super(
                "arrowTrackerWidget",
                "Arrow Tracker",
                550.0f, 490.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> Configuration.arrowTracker);
        setVisibilityCondition(this::shouldBeVisible);

        setExampleLines(HudLine.of("§f2304x §eFlint Arrow"));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLine(arrowLine);

        subscribe(ClientTickEvent.class, event -> {
            if (event.isNthTick(5)) {
                updateArrowLine();
            }
        });

        updateArrowLine();
    }

    private boolean shouldBeVisible() {
        ArrowTrackerFeature feature = getArrowTrackerFeature();
        if (feature == null || !feature.isActive()) {
            return false;
        }

        // Check visibility setting
        Configuration.ArrowTrackerVisibility visibility = Configuration.arrowTrackerVisibility;

        if (visibility == Configuration.ArrowTrackerVisibility.ALWAYS) {
            // Always show if we have ever tracked valid data
            return feature.hasTrackedData();
        } else {
            // Only show when holding bow
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return false;

            var mainHandItem = mc.player.getMainHandItem();
            var offHandItem = mc.player.getOffhandItem();

            boolean holdingBow = isBow(mainHandItem) || isBow(offHandItem);

            return holdingBow && (feature.isOutOfArrows() ||
                    (!feature.getCurrentArrowType().equals("Unknown") && feature.getCurrentArrowCount() > 0));
        }
    }

    private boolean isBow(@NotNull net.minecraft.world.item.ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        String itemId = itemStack.getItem().getDescriptionId();
        return itemId.contains("bow");
    }

    private void updateArrowLine() {
        ArrowTrackerFeature feature = getArrowTrackerFeature();
        if (feature == null || !feature.isActive()) {
            return; // Don't update if feature is not active
        }

        if (feature.isOutOfArrows()) {
            arrowLine.text("§cOut of Arrows");
            markDimensionsDirty();
            return;
        }

        int count = feature.getCurrentArrowCount();
        String type = feature.getCurrentArrowType();

        if (!type.equals("Unknown") && count > 0) {
            // Format: "{count}x {type}" with dynamic color only for count
            // Arrow type is always yellow
            String colorCode = getArrowColor(count);
            arrowLine.text(String.format("%s%dx §e%s", colorCode, count, type));
        }
        // If unknown, keep last displayed value (don't reset)

        markDimensionsDirty();
    }

    private @NotNull String getArrowColor(int count) {
        // Máximo de flechas = 2880
        // Tier 1: 2160-2880 (75-100%) = Verde
        // Tier 2: 1440-2159 (50-75%) = Amarelo
        // Tier 3: 720-1439 (25-50%) = Laranja
        // Tier 4: 50-719 (2-25%) = Vermelho
        // Tier 5: <50 = Vermelho escuro

        if (count < 50) return "§4"; // Dark red - critical
        if (count < 720) return "§c"; // Red - very low
        if (count < 1440) return "§6"; // Orange - low
        if (count < 2160) return "§e"; // Yellow - medium
        return "§a"; // Green - plenty
    }

    private ArrowTrackerFeature getArrowTrackerFeature() {
        try {
            IQModClient client = IQModClient.get();
            if (client == null || client.getFeatureManager() == null) {
                return null;
            }
            return client.getFeatureManager().get(ArrowTrackerFeature.class);
        } catch (Exception e) {
            log.debug("Could not get ArrowTrackerFeature", e);
            return null;
        }
    }
}

