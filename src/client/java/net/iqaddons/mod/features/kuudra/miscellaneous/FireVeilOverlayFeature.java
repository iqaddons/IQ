package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.component.DataComponents;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.iqaddons.mod.events.impl.ParticleEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.FireVeilOverlayManager;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Slf4j
public class FireVeilOverlayFeature extends Feature {

    private static final String FIRE_VEIL_ID = "FIRE_VEIL_WAND";
    private static final String FIRE_VEIL_NAME = "fire veil";
    private static final float FIRE_VEIL_RADIUS = 3.5f;
    private static final float FIRE_VEIL_CIRCLE_THICKNESS = 0.08f;
    private static final int FIRE_VEIL_CIRCLE_SEGMENTS = 60;
    private static final float FIRE_VEIL_WALL_HEIGHT = 1.0f;

    private final FireVeilOverlayManager manager = FireVeilOverlayManager.get();
    private long lastLoggedCastMs = 0L;
    private long lastRenderedCastMs = 0L;

    public FireVeilOverlayFeature() {
        super(
                "fireVeilOverlay",
                "Fire Veil Overlay",
                () -> PhaseTwoConfig.fireVeilOverlay
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(ItemUseEvent.class, this::onItemUse);
        subscribe(ParticleEvent.class, this::onParticle);
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    @Override
    protected void onDeactivate() {
        manager.reset();
        lastLoggedCastMs = 0L;
        lastRenderedCastMs = 0L;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || mc.player == null) return;
        if (!PhaseTwoConfig.FireVeilOverlayConfig.soundWhenRecast) return;

        if (manager.consumeReadySound(System.currentTimeMillis())) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0f, 1.6f);
        }
    }

    private void onItemUse(@NotNull ItemUseEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isFireVeil(event.getItemStack())) return;

        long now = System.currentTimeMillis();
        manager.recordCast(now);
        lastLoggedCastMs = now;
        lastRenderedCastMs = 0L;

        log.info(
                "[Fire Veil Overlay] Cast detected. item='{}' rootId='{}' extraId='{}' render={} color={} throughWalls={} countdown={} sound={}",
                StringUtils.stripFormatting(event.getItemStack().getHoverName().getString()),
                getRootItemId(event.getItemStack()),
                getExtraAttributesItemId(event.getItemStack()),
                PhaseTwoConfig.FireVeilOverlayConfig.render,
                Integer.toHexString(PhaseTwoConfig.FireVeilOverlayConfig.renderColor),
                PhaseTwoConfig.FireVeilOverlayConfig.renderThroughWalls,
                PhaseTwoConfig.FireVeilOverlayConfig.abilityCountdown,
                PhaseTwoConfig.FireVeilOverlayConfig.soundWhenRecast
        );
    }

    private void onParticle(@NotNull ParticleEvent event) {
        if (PhaseTwoConfig.FireVeilOverlayConfig.render == PhaseTwoConfig.FireVeilOverlayRender.DEFAULT) return;
        if (!manager.shouldHideDefaultParticles(System.currentTimeMillis())) return;
        if (event.getType() != ParticleTypes.FLAME) return;

        event.setCancelled(true);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return;
        if (!manager.isAbilityActive(System.currentTimeMillis())) return;

        if (PhaseTwoConfig.FireVeilOverlayConfig.render == PhaseTwoConfig.FireVeilOverlayRender.DEFAULT) {
            return;
        }

        Vec3 center = getInterpolatedPlayerPos(event);
        RenderColor color = RenderColor.fromArgb(PhaseTwoConfig.FireVeilOverlayConfig.renderColor);
        boolean throughWalls = PhaseTwoConfig.FireVeilOverlayConfig.renderThroughWalls;
        if (PhaseTwoConfig.FireVeilOverlayConfig.render == PhaseTwoConfig.FireVeilOverlayRender.WALL) {
            drawFireVeilWall(event, center, throughWalls, color);
            logFirstRender(center);
            return;
        }

        event.drawThickCircleOutline(center, FIRE_VEIL_RADIUS, FIRE_VEIL_CIRCLE_THICKNESS,
                FIRE_VEIL_CIRCLE_SEGMENTS, throughWalls, color);
        logFirstRender(center);
    }

    private void logFirstRender(@NotNull Vec3 center) {
        if (lastLoggedCastMs <= 0L || lastRenderedCastMs == lastLoggedCastMs) return;

        lastRenderedCastMs = lastLoggedCastMs;
        log.info(
                "[Fire Veil Overlay] First world render after cast. mode={} throughWalls={} center=({}, {}, {}) radius={} thickness={} activeMsLeft={}",
                PhaseTwoConfig.FireVeilOverlayConfig.render,
                PhaseTwoConfig.FireVeilOverlayConfig.renderThroughWalls,
                String.format(Locale.ROOT, "%.2f", center.x),
                String.format(Locale.ROOT, "%.2f", center.y),
                String.format(Locale.ROOT, "%.2f", center.z),
                FIRE_VEIL_RADIUS,
                FIRE_VEIL_CIRCLE_THICKNESS,
                manager.getRemainingMs(System.currentTimeMillis())
        );
    }

    private void drawFireVeilWall(
            @NotNull WorldRenderEvent event,
            @NotNull Vec3 center,
            boolean throughWalls,
            @NotNull RenderColor color
    ) {
        event.drawCircleWireframeWall(center, FIRE_VEIL_RADIUS, FIRE_VEIL_WALL_HEIGHT,
                FIRE_VEIL_CIRCLE_SEGMENTS, throughWalls, color);
    }

    private boolean isFireVeil(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            var itemId = nbt.getString("id");
            if (itemId.isPresent() && itemId.get().equals(FIRE_VEIL_ID)) {
                return true;
            }

            var extraAttributes = nbt.getCompound("ExtraAttributes");
            if (extraAttributes.isPresent()) {
                var extraItemId = extraAttributes.get().getString("id");
                if (extraItemId.isPresent() && extraItemId.get().equals(FIRE_VEIL_ID)) {
                    return true;
                }
            }
        }

        String name = StringUtils.stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        return name.contains(FIRE_VEIL_NAME);
    }

    private @NotNull String getRootItemId(@NotNull ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";

        return customData.copyTag().getString("id").orElse("");
    }

    private @NotNull String getExtraAttributesItemId(@NotNull ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";

        var extraAttributes = customData.copyTag().getCompound("ExtraAttributes");
        if (extraAttributes.isEmpty()) return "";

        return extraAttributes.get().getString("id").orElse("");
    }

    private @NotNull Vec3 getInterpolatedPlayerPos(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return Vec3.ZERO;

        float partialTicks = event.tickCounter().getGameTimeDeltaPartialTick(true);
        double x = mc.player.xo + (mc.player.getX() - mc.player.xo) * partialTicks;
        double y = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTicks + 0.02;
        double z = mc.player.zo + (mc.player.getZ() - mc.player.zo) * partialTicks;
        return new Vec3(x, y, z);
    }
}
