package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.iqaddons.mod.utils.hud.element.HudAnchor;
import net.iqaddons.mod.utils.hud.element.HudWidget;
import net.iqaddons.mod.utils.render.HudRenderer;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuildProgressWidget extends HudWidget {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("Protect Elle:\\s*(\\d+)%");
    private static final String FRESH_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed";

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private int currentProgress = 0;
    private long phaseStartTime = 0;
    private int freshCount = 0;

    private EventBus.Subscription<ChatReceivedEvent> chatSubscription;
    private EventBus.Subscription<ClientTickEvent> tickSubscription;

    private final HudLine titleLine;
    private final HudLine progressLine;
    private final HudLine freshLine;
    private final HudLine etaLine;

    public BuildProgressWidget() {
        super(
                "buildProgress",
                "Build Progress",
                10.0f, 80.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§6§lBuild Progress");
        progressLine = HudLine.of("§7Progress: §a0%");
        freshLine = HudLine.of("§7Fresh: §b0")
                .showWhen(() -> freshCount > 0);
        etaLine = HudLine.of("§7ETA: §e--");

        setEnabledSupplier(() -> PhaseTwoConfig.buildHelper);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.BUILD);

        setExampleLines(List.of(
                HudLine.of("§6§lBuild Progress"),
                HudLine.of("§7Progress: §a75%"),
                HudLine.of("§7Fresh: §b3"),
                HudLine.of("§7ETA: §e12s")
        ));
    }

    @Override
    protected void onActivate() {
        currentProgress = 0;
        phaseStartTime = System.currentTimeMillis();
        freshCount = 0;

        clearLines();
        addLine(titleLine);
        addLine(progressLine);
        addLine(freshLine);
        addLine(etaLine);

        chatSubscription = EventBus.subscribe(ChatReceivedEvent.class, this::onChat);
        tickSubscription = EventBus.subscribe(ClientTickEvent.class, this::onTick);

        log.info("Build Progress Widget activated");
    }

    @Override
    protected void onDeactivate() {
        if (chatSubscription != null) {
            chatSubscription.unsubscribe();
            chatSubscription = null;
        }
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }

        log.info("Build Progress Widget deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        if (message.contains(FRESH_MESSAGE)) {
            freshCount++;
            updateDisplay();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(10)) return;

        ScoreboardUtils.findLine("Protect Elle")
                .ifPresent(line -> {
                    String stripped = ScoreboardUtils.stripFormatting(line);
                    Matcher matcher = PROGRESS_PATTERN.matcher(stripped);
                    if (matcher.find()) {
                        int newProgress = Integer.parseInt(matcher.group(1));
                        if (newProgress != currentProgress) {
                            currentProgress = newProgress;
                            updateDisplay();
                        }
                    }
                });
    }

    private void updateDisplay() {
        String progressColor = getProgressColor(currentProgress);
        progressLine.text(String.format("§7Progress: %s%d%%", progressColor, currentProgress));
        freshLine.text(String.format("§7Fresh: §b%d", freshCount));

        String eta = calculateETA();
        etaLine.text(String.format("§7ETA: §e%s", eta));

        markDimensionsDirty();
    }

    private @NotNull String getProgressColor(int progress) {
        if (progress >= 80) return "§a";
        if (progress >= 60) return "§2";
        if (progress >= 40) return "§e";
        if (progress >= 20) return "§6";
        return "§c";
    }

    private @NotNull String calculateETA() {
        if (currentProgress <= 0 || phaseStartTime <= 0) {
            return "--";
        }

        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed <= 0) return "--";

        double progressPerMs = currentProgress / (double) elapsed;
        if (progressPerMs <= 0) return "--";

        int remaining = 100 - currentProgress;
        long remainingMs = (long) (remaining / progressPerMs);
        double remainingSeconds = remainingMs / 1000.0;

        if (remainingSeconds > 120) {
            return String.format("%.0fm", remainingSeconds / 60);
        }
        return String.format("%.0fs", remainingSeconds);
    }

    @Override
    public void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (shouldRender() && isActive() && currentProgress > 0) {
            float absX = getAbsoluteX();
            float absY = getAbsoluteY();
            int height = getScaledHeight();

            int barY = (int) (absY + height + 2);
            int barWidth = Math.max(getScaledWidth(), 100);

            HudRenderer.drawProgressBarAuto(
                    context,
                    (int) absX, barY,
                    barWidth, 6,
                    currentProgress / 100.0f
            );
        }
    }
}