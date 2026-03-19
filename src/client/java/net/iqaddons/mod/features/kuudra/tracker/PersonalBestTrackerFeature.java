package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.PersonalBestManager;
import net.iqaddons.mod.model.PersonalBest;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PersonalBestTrackerFeature extends Feature {

    private final PersonalBestManager personalBestManager = PersonalBestManager.get();
    private final Map<Integer, PersonalBest.SupplyTiming> supplyTimings = new HashMap<>();
    private final List<PersonalBest.FreshTiming> freshTimings = new ArrayList<>();
    private long buildPhaseStartedAt = -1L;

    public PersonalBestTrackerFeature() {
        super("personalBestTracker", "PB Tracker",
                () -> KuudraGeneralConfig.personalBestTracker);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
        subscribe(PlayerFreshEvent.class, this::onFresh);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyTimings.clear();
            freshTimings.clear();
            buildPhaseStartedAt = -1L;
        }

        if (event.currentPhase() == KuudraPhase.BUILD) {
            buildPhaseStartedAt = System.currentTimeMillis();
        }
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (event.currentSupply() <= 0 || event.currentSupply() > 6) {
            return;
        }

        supplyTimings.put(
                event.currentSupply(),
                PersonalBest.SupplyTiming.of(event.playerName(), event.currentSupply(), event.placedAt())
        );
    }

    private void onFresh(@NotNull PlayerFreshEvent event) {
        if (buildPhaseStartedAt <= 0) {
            return;
        }

        double seconds = Math.max(0.0, (event.freshAt() - buildPhaseStartedAt) / 1000.0);
        freshTimings.add(PersonalBest.FreshTiming.of(event.playerName(), seconds));
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        List<PersonalBest.SupplyTiming> runSupplyTimings = supplyTimings.values().stream()
                .sorted(java.util.Comparator.comparingInt(PersonalBest.SupplyTiming::currentSupply))
                .toList();
        List<PersonalBest.FreshTiming> runFreshTimings = List.copyOf(freshTimings);

        supplyTimings.clear();
        freshTimings.clear();
        buildPhaseStartedAt = -1L;

        if (!event.isCompleted()) return;

        long runMillis = event.totalDuration().toMillis();
        if (runMillis <= 0) return;

        long previousPbMillis = personalBestManager.getBestTimeMillis();
        if (previousPbMillis > 0 && runMillis >= previousPbMillis) {
            return;
        }

        Map<KuudraPhase, Long> splits = new EnumMap<>(KuudraPhase.class);
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            splits.put(phase, event.getPhase(phase).toMillis());
        }

        personalBestManager.updatePersonalBest(
                runMillis,
                splits,
                event.tier(),
                System.currentTimeMillis(),
                runSupplyTimings,
                runFreshTimings
        );
        if (previousPbMillis <= 0) {
            MessageUtil.sendFormattedMessage("&aNew Personal Best! &9" + formatSeconds(runMillis));
            return;
        }

        long deltaMillis = previousPbMillis - runMillis;
        MessageUtil.showTitle(
                "§a§lNew Personal Best!",
                "§c" + formatSeconds(previousPbMillis) + " §7> §9" + formatSeconds(runMillis),
                0,
                35,
                10
        );
        MessageUtil.sendFormattedMessage(
                "&aNew Personal Best! &c" + formatSeconds(previousPbMillis)
                        + " &7> &9" + formatSeconds(runMillis)
                        + " &8(-" + formatSeconds(deltaMillis) + ")"
        );
    }

    private @NotNull String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }
}