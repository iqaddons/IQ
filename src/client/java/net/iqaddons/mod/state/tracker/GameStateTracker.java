package net.iqaddons.mod.state.tracker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.Subscribe;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.state.KuudraPhase;
import net.iqaddons.mod.state.KuudraState;
import net.iqaddons.mod.state.KuudraTier;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class GameStateTracker {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter
    private static boolean onSkyBlock = false;

    @Getter
    private static String currentArea = "";

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    public static void init() {
        log.info("GameStateTracker initialized");
    }

    @Subscribe
    public void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        checkSkyBlockStatus();
        checkArea();
    }

    @Subscribe
    public void onChat(ChatReceivedEvent event) {
        if (!onSkyBlock) return;

        var message = event.getMessage();
        KuudraState.setPhase(KuudraPhase.fromMessage(message));

        if (message.contains("KUUDRA") || message.contains("Kuudra")) {
            KuudraTier tier = KuudraTier.fromName(message);
            if (tier != KuudraTier.NONE) {
                KuudraState.setTier(tier);
            }
        }
    }

    private void checkSkyBlockStatus() {
        boolean wasSkyBlock = onSkyBlock;
        onSkyBlock = ScoreboardUtils.hasTitle("SKYBLOCK");

        if (wasSkyBlock && !onSkyBlock) {
            KuudraState.reset();
            currentArea = "";
            log.info("Left SkyBlock");
        }
    }

    private void checkArea() {
        if (!onSkyBlock) return;

        String area = ScoreboardUtils.getArea();
        if (!area.equals(currentArea)) {
            String previousArea = currentArea;
            currentArea = area;

            if (previousArea.contains("Kuudra's Hollow") && !currentArea.contains("Kuudra's Hollow")) {
                KuudraState.reset();
            }

            log.debug("Area changed: {} -> {}", previousArea, currentArea);
        }
    }
}