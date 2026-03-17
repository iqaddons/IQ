package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public class FreshersTimerWidget extends HudWidget {

    private final List<PlayerFreshedEntry> freshEntries = Collections.synchronizedList(new ArrayList<>());

    private final KuudraStateManager kuudraManager = KuudraStateManager.get();
    private volatile boolean persistUntilInstanceChange = false;

    public FreshersTimerWidget() {
        super("freshers_timer",
                "Freshers Timer",
                6.5f, 200.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseTwoConfig.freshTimers);
        setVisibilityCondition(() -> {
            var phase = kuudraManager.phase();
            boolean inTrackedPhases = KuudraPhase.isOneOf(
                    KuudraPhase.BUILD, KuudraPhase.EATEN, KuudraPhase.STUN,
                    KuudraPhase.DPS, KuudraPhase.SKIP, KuudraPhase.BOSS,
                    KuudraPhase.COMPLETED
            ).test(phase);
            return inTrackedPhases || persistUntilInstanceChange;
        });

        setExampleLines(List.of(
                HudLine.of("§b§lFresh Timers §8[§e2§8]"),
                HudLine.of("§bckac10 §8- §f§l5.23s"),
                HudLine.of("§aPeHenrii §8- §a§l7.85s")
        ));
    }

    @Override
    protected void onActivate() {
        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);

        if (kuudraManager.phase() == KuudraPhase.BUILD) {
            beginNewRunWindow();
        }

        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        freshEntries.clear();
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        if (event.selfFresh()) return;
        var context = kuudraManager.context();
        if (context.phase() != KuudraPhase.BUILD) {
            return;
        }

        freshEntries.add(new PlayerFreshedEntry(
                event.playerName(),
                context.phaseDuration().toMillis())
        );

        updateDisplay();
    }

    private void beginNewRunWindow() {
        persistUntilInstanceChange = true;
        freshEntries.clear();
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.currentPhase() == KuudraPhase.BUILD) {
            beginNewRunWindow();
            updateDisplay();
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) {
            persistUntilInstanceChange = false;
            freshEntries.clear();
            updateDisplay();
        }
    }

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        boolean stillInKuudraInstance = event.onSkyBlock() && event.newArea().contains(KUUDRA_AREA_ID);
        if (stillInKuudraInstance) {
            return;
        }

        persistUntilInstanceChange = false;
        freshEntries.clear();
        updateDisplay();
    }

    private void updateDisplay() {
        clearLines();
        int freshers = freshEntries.size();
        addLine(HudLine.of(String.format("§b§lFresh Timers §8[%s%d§8]",
                getFresherCountColor(freshers), freshers))
        );

        if (freshEntries.isEmpty()) {
            addLine(HudLine.of("§7No freshers..."));
            return;
        }

        for (PlayerFreshedEntry entry : freshEntries) {
            double timeInSeconds = entry.freshTime / 1000.0;

            addLine(HudLine.of(String.format("%s §8- %s§l%.2fs",
                    entry.playerName(), getTimeColor(timeInSeconds), timeInSeconds)
            ));
        }
    }

    @Contract(pure = true)
    private @NotNull String getTimeColor(double time) {
        if (time <= 5) return "§9";
        if (time <= 7) return "§a";
        if (time <= 9) return "§6";
        return "§c";
    }

    @Contract(pure = true)
    private @NotNull String getFresherCountColor(int count) {
        return switch (count) {
            case 0 -> "§c";
            case 1 -> "§6";
            case 2 -> "§e";
            case 3 -> "§a";
            case 4 -> "§9";
            default -> "§b";
        };
    }

    private record PlayerFreshedEntry(
            String playerName,
            long freshTime
    ) {}
}
