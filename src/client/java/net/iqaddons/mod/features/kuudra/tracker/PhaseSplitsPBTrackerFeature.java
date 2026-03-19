package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.PhaseSplitsPBManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.kuudra.KuudraTier;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class PhaseSplitsPBTrackerFeature extends Feature {

    private final PhaseSplitsPBManager pbManager = PhaseSplitsPBManager.get();
    private int currentRunFreshCount = 0;

    public PhaseSplitsPBTrackerFeature() {
        super("phaseSplitsPBTracker", "Phase Splits PB Tracker",
                () -> KuudraGeneralConfig.phaseSplitsPBTracker);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(PlayerFreshEvent.class, this::onFresh);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            currentRunFreshCount = 0;
            return;
        }

        KuudraPhase finishedPhase = event.previousPhase();
        if (!finishedPhase.isInRun()) return;
        if (KuudraStateManager.get().context().tier() != KuudraTier.INFERNAL) return;

        long millis = event.phaseDurationMillis();
        if (millis <= 0) return;

        long previousPb = pbManager.getBestPhaseMillis(finishedPhase);
        boolean isNewPB = finishedPhase == KuudraPhase.BUILD
                ? pbManager.tryUpdatePhase(finishedPhase, millis, currentRunFreshCount)
                : pbManager.tryUpdatePhase(finishedPhase, millis);
        if (!isNewPB) return;

        if (previousPb > 0) {
            long delta = previousPb - millis;
            MessageUtil.sendFormattedMessage(
                    "&aNew Phase Personal Best! &3" + finishedPhase.getDisplayName()
                            + ": &c" + formatSeconds(previousPb)
                            + " &7> &9" + formatSeconds(millis)
                            + " &8(-" + formatSeconds(delta) + ")"
            );
        } else {
            MessageUtil.sendFormattedMessage(
                    "&aNew Phase Personal Best! &3" + finishedPhase.getDisplayName()
                            + ": &9" + formatSeconds(millis)
            );
        }
    }

    private void onFresh(@NotNull PlayerFreshEvent event) {
        if (KuudraStateManager.get().phase() == KuudraPhase.BUILD) {
            currentRunFreshCount++;
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        currentRunFreshCount = 0;
    }

    @NotNull String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }
}

