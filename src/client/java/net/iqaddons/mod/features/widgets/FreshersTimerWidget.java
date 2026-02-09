package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.state.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FreshersTimerWidget extends HudWidget {

    private final List<PlayerFreshedEntry> freshEntries = Collections.synchronizedList(new ArrayList<>());

    public FreshersTimerWidget() {
        super("freshers-timer",
                "Freshers Timer",
                840, 30,
                1.0f,
                HudAnchor.TOP_RIGHT
        );

        setEnabledSupplier(() -> PhaseTwoConfig.freshTimers);
        setVisibilityCondition(() -> KuudraStateManager.get().phase() == KuudraPhase.BUILD);

        setExampleLines(List.of(
                HudLine.of("§b§lFresh Timers §7[2]"),
                HudLine.of("§fckac10 §8- §f5.23s"),
                HudLine.of("§fPeHenrii §8- §a7.85s")
        ));
    }

    @Override
    protected void onActivate() {

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);

        freshEntries.clear();
        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        freshEntries.clear();
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        freshEntries.add(new PlayerFreshedEntry(event.playerName(), event.freshAt()));

        log.info("Player {} got fresh at {}ms", event.playerName(), event.freshAt());
        updateDisplay();
    }

    private void updateDisplay() {
        addLine(HudLine.of(String.format("§b§lFresh Timers §b%d[%d]",
                freshEntries.size(), freshEntries.size()))
        );

        if (freshEntries.isEmpty()) {
            addLine(HudLine.of("§7No fresh timers yet"));
            return;
        }

        for (PlayerFreshedEntry entry : freshEntries) {
            double timeInSeconds = entry.freshTime / 1000.0;

            addLine(HudLine.of(String.format("§f%s %s§l%.2fs",
                    entry.playerName(), getTimeColor(timeInSeconds), timeInSeconds)
            ));
        }
    }

    @Contract(pure = true)
    private static @NotNull String getTimeColor(double time) {
        if (time <= 5) return "§f";
        if (time <= 7) return "§a";
        if (time <= 9) return "§6";
        return "§c";
    }

    private record PlayerFreshedEntry(
            String playerName,
            long freshTime
    ) {}
}
