package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.PersonalBestManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class PersonalBestTrackerFeature extends Feature {

    private static final Identifier PERSONALBEST_SOUND_ID = Identifier.of("iq", "pb_new_record");

    private final PersonalBestManager personalBestManager = PersonalBestManager.get();

    public PersonalBestTrackerFeature() {
        super("personalBestTracker", "PB Tracker",
                () -> KuudraGeneralConfig.personalBestTracker);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (!event.completed()) return;

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

        personalBestManager.updatePersonalBest(runMillis, splits);
        if (previousPbMillis <= 0) {
            MessageUtil.sendFormattedMessage("§aNew Personal Best set: &f" + formatSeconds(runMillis));
            playSound();
            return;
        }

        MessageUtil.showTitle("§a§lNew Personal Best!", formatSeconds(previousPbMillis) + " > " + formatSeconds(runMillis), 0, 35, 10);
        MessageUtil.sendFormattedMessage(String.format(
                "§aYou just beat a new Personal Best! Previous one was &f%s&a, now is &f%s&a.",
                formatSeconds(previousPbMillis),
                formatSeconds(runMillis)
        ));

        playSound();
    }

    private @NotNull String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }

    private void playSound() {
        if (mc.getSoundManager() == null) {
            return;
        }

        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(PERSONALBEST_SOUND_ID), 1.0F, 1.0F));
    }
}