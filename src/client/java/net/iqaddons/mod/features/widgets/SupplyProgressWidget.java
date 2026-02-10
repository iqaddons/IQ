package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public class SupplyProgressWidget extends HudWidget {

    private static final Pattern SUPPLY_PROGRESS_PATTERN = Pattern.compile("^\\[[| ]+]\\s*\\d+%$");

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

        progressLine = HudLine.of("§8[§a|||||||||||||§f|||||||§8] §b69%§r");

        setEnabledSupplier(() -> PhaseOneConfig.supplyProgressDisplay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.SUPPLIES && !currentProgress.isEmpty());

        setExampleLines(List.of(progressLine));
    }

    @Override
    protected void onActivate() {
        currentProgress = "";
        progressLine.text("");

        clearLines();
        addLine(progressLine);

        subscribe(TitleReceivedEvent.class, this::onTitleReceived);
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onTitleReceived(@NotNull TitleReceivedEvent event) {
        if (stateManager.phase() != KuudraPhase.SUPPLIES) return;

        String stripped = event.getStrippedMessage();
        if (!SUPPLY_PROGRESS_PATTERN.matcher(stripped).matches()) return;

        currentProgress = event.getMessage();
        progressLine.text(currentProgress);
        markDimensionsDirty();

        event.setCancelled(true);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        if (message.contains("You retrieved some of Elle's supplies from the Lava!")) {
            clearProgress();
            return;
        }

        if (message.contains("You moved and the Chest slipped out of your hands!")) {
            clearProgress();
        }
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