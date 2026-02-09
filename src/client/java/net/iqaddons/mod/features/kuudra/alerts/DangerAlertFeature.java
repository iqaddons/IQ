package net.iqaddons.mod.features.kuudra.alerts;

import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DangerAlertFeature extends KuudraFeature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int DANGER_CHECK_INTERVAL_TICKS = 5;

    public DangerAlertFeature() {
        super(
                "dangerZoneAlert",
                "Danger Zone Alert",
                () -> PhaseFourConfig.dangerZoneAlert,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ClientTickEvent.class, this::onTick);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(DANGER_CHECK_INTERVAL_TICKS)) return;
        if (mc.player == null || mc.world == null) return;

        DangerLevel currentDangerLevel = null;
        for (int i = 0; i < 5; i++) {
            BlockPos blockPos = mc.player.getBlockPos().down(i);
            DangerLevel level = getDangerLevel(blockPos);
            if (level != null) {
                currentDangerLevel = level;
                break;
            }
        }

        if (currentDangerLevel != null) {
            if (currentDangerLevel.shouldJump()) {
                MessageUtil.showTitle("§e§lJUMP!", "", 0, 10, 5);

                mc.world.playSound(
                        mc.player, mc.player.getBlockPos(),
                        SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                        SoundCategory.PLAYERS, 2.0f, 2.0f
                );
            } else {
                MessageUtil.showTitle("§c§lDANGER", "", 0, 10, 5);
            }
        }
    }

    private @Nullable DangerLevel getDangerLevel(BlockPos blockPos) {
        BlockState state = mc.world.getBlockState(blockPos);
        Block block = state.getBlock();

        return switch (block) {
            case Block b when b == Blocks.GREEN_TERRACOTTA -> DangerLevel.GREEN;
            case Block b when b == Blocks.LIME_TERRACOTTA -> DangerLevel.LIME;
            case Block b when b == Blocks.YELLOW_TERRACOTTA -> DangerLevel.YELLOW;
            case Block b when b == Blocks.ORANGE_TERRACOTTA -> DangerLevel.ORANGE;
            case Block b when b == Blocks.RED_TERRACOTTA -> DangerLevel.RED;
            default -> null;
        };
    }

    @RequiredArgsConstructor
    private enum DangerLevel {
        GREEN(false),
        LIME(false),
        YELLOW(true),
        ORANGE(true),
        RED(true);

        private final boolean shouldJump;

        public boolean shouldJump() {
            return shouldJump;
        }
    }
}
