package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
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

    public PhaseSplitsPBTrackerFeature() {
        super("phaseSplitsPBTracker", "Phase Splits PB Tracker",
                () -> KuudraGeneralConfig.phaseSplitsPBTracker);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            return;
        }

        if (!event.isRunCompleted()) return;

        KuudraPhase finishedPhase = event.previousPhase();
        if (finishedPhase != KuudraPhase.BOSS) return;
        if (KuudraStateManager.get().context().tier() != KuudraTier.INFERNAL) return;

        long millis = event.phaseDurationMillis();
        if (millis <= 0) return;

        long previousPb = pbManager.getBestPhaseMillis(finishedPhase);
        boolean isNewPB = pbManager.tryUpdatePhase(finishedPhase, millis);
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


    @NotNull String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }
}

