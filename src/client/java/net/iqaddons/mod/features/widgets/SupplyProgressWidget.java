package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class SupplyProgressWidget extends HudWidget {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine progressLine;

    private String currentProgress = "";

    public SupplyProgressWidget() {
        super(
                "supplyProgress",
                "Supply Progress",
                10.0f, 120.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        progressLine = HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r")
                .showWhen(() -> !currentProgress.isEmpty());

        setEnabledSupplier(() -> PhaseOneConfig.supplyProgressDisplay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.SUPPLIES);

        setExampleLines(HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r"));
    }

    @Override
    protected void onActivate() {
        currentProgress = "";
        progressLine.text("");

        clearLines();
        addLine(progressLine);

        subscribe(TitleReceivedEvent.class, event -> {
            if (currentProgress.isEmpty()) return;
            clearProgress();
        });
        subscribe(SupplyPickupEvent.class, event -> clearProgress());
        subscribe(SupplyDropEvent.class, event -> clearProgress());

        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);
    }


    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        currentProgress = event.getProgressText();
        progressLine.text(currentProgress);
        markDimensionsDirty();

        if (event.getCurrentProgress() == 100) {
            clearProgress();
            MessageUtil.showTitle("§a§lSUPPLY PICKED UP!", "", 10, 40, 10);
            mc.world.playSound(
                    mc.player, mc.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 2.0f, 1.0f
            );
        }

        event.setCancelled(true);
    }

    private void clearProgress() {
        currentProgress = "";
        progressLine.text("");
        markDimensionsDirty();
    }

    @Override
    protected void onDeactivate() {
        clearProgress();
    }
}