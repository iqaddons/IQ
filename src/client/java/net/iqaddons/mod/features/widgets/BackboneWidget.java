package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.BackboneAlertManager;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class BackboneWidget extends HudWidget {

    private final BackboneAlertManager manager = BackboneAlertManager.get();

    public BackboneWidget() {
        super(
                "backbone_alert",
                "Backbone Alert",
                336.5f,
                272.5f,
                2.3f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseFourConfig.backboneAlert);
        setVisibilityCondition(() -> KuudraStateManager.get().phase() == KuudraPhase.BOSS &&
                (manager.getTicksRemaining() > 0 || manager.isRendActive())
        );

        setExampleLines(HudLine.of("§8[§c||||||||§7||||||||||||§8] §b40%"));
    }

    @Override
    protected void onActivate() {
        subscribe(ClientTickEvent.class, this::onTick);
        refreshLines();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        refreshLines();
    }

    private void refreshLines() {
        clearLines();
        if (manager.isRendActive()) {
            if (mc.player == null) return;

            addLine(HudLine.of("§a§lREND NOW!"));
            mc.player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    2.0f, 1.0f
            );

            return;
        }

        addLine(HudLine.of(manager.getProgressBar() + " " + manager.getPercentString()));
    }
}
