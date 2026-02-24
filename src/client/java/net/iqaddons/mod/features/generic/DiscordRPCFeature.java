package net.iqaddons.mod.features.generic;

import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.models.DiscordRichPresence;
import lombok.extern.slf4j.Slf4j;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.integration.DiscordRPCIntegration;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DiscordRPCFeature extends Feature {

    private static final String KUUDRA_NET_URL = "https://kuudra.net";
    private static final String KUUDRA_NET_LABEL = "kuudra.net";

    private static final long INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L;

    private static final int INACTIVITY_CHECK_TICKS = 100;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private final AtomicInteger sessionRuns = new AtomicInteger(0);
    private final AtomicInteger partySize = new AtomicInteger(1);

    private volatile long lastRunCompletedAt = 0L;
    private volatile long sessionStartEpochSecond = 0L;
    private volatile long runStartEpochSecond = 0L;

    private volatile boolean presenceActive = false;
    private volatile boolean insideKuudra = false;

    public DiscordRPCFeature() {
        super("discord_rpc", "Discord RPC", () -> Configuration.discordRichPresence);
    }

    @Override
    protected void onActivate() {
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
        subscribe(ClientTickEvent.class, this::onTick);

        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, this::onPartyInfoReceived);
        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, clientboundLocationPacket -> {
            log.info("Received location packet: {}", clientboundLocationPacket.toString());
        });

        if (stateManager.phase().isInRun()) {
            startSession();
            insideKuudra = true;
            updatePresenceInKuudra();
        }

        log.info("Kuudra Discord RPC feature activated");
    }

    @Override
    protected void onDeactivate() {
        clearAndDisconnect();
        resetState();
        log.info("Kuudra Discord RPC feature deactivated");
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        KuudraPhase current = event.currentPhase();
        KuudraPhase previous = event.previousPhase();

        if (!previous.isInRun() && current.isInRun()) {
            insideKuudra = true;
            runStartEpochSecond = System.currentTimeMillis() / 1000;
            if (!presenceActive) startSession();

            requestPartyInfo();
            updatePresenceInKuudra();
            return;
        }

        if (previous.isInRun() && !current.isInRun()) {
            insideKuudra = false;
            updatePresenceLobby();
            return;
        }

        if (current.isInRun()) {
            updatePresenceInKuudra();
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isCompleted()) {
            sessionRuns.incrementAndGet();
            lastRunCompletedAt = System.currentTimeMillis();
        }

        insideKuudra = false;
        requestPartyInfo();
        updatePresenceLobby();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(INACTIVITY_CHECK_TICKS)) return;

        if (!insideKuudra && presenceActive && lastRunCompletedAt > 0) {
            long elapsed = System.currentTimeMillis() - lastRunCompletedAt;
            if (elapsed >= INACTIVITY_TIMEOUT_MS) {
                log.info("No Kuudra runs in 5 minutes — hiding Discord RPC");
                clearAndDisconnect();
            }
        }
    }

    private void onPartyInfoReceived(@NotNull ClientboundPartyInfoPacket packet) {
        log.info(packet.toString());
        if (packet.isInParty()) partySize.set(packet.getMembers().size());
        else partySize.set(1);

        log.debug("Party size updated: {}", partySize.get());
        if (!insideKuudra && presenceActive) {
            updatePresenceLobby();
        }
    }

    private void startSession() {
        sessionStartEpochSecond = System.currentTimeMillis() / 1000;
        lastRunCompletedAt = System.currentTimeMillis();
        presenceActive = true;

        DiscordRPCIntegration.INSTANCE.connect(IQConstants.DISCORD_RPC_ID);

        requestPartyInfo();
        log.info("Discord RPC session started");
    }

    private void clearAndDisconnect() {
        presenceActive = false;
        DiscordRPCIntegration.INSTANCE.clearPresence();
        DiscordRPCIntegration.INSTANCE.shutdown();
    }

    private void updatePresenceInKuudra() {
        KuudraPhase phase = stateManager.phase();
        if (phase == KuudraPhase.NONE) return;

        var tier = stateManager.context().tier();
        String details = stateManager.context().tier().getDisplayName() + " Kuudra";
        String state = "Phase: " + phase.getDisplayName();

        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(details)
                .state(state)
                .startTimestamp(runStartEpochSecond)
                .smallImageKey(tier.getAssetCode())
                .smallImageText(tier.getDisplayName())
                .largeImageKey("logo")
                .largeImageText("IQ Addons")
                .activityType(ActivityType.COMPETING)
                .button(DiscordRichPresence.RPCButton.of(KUUDRA_NET_LABEL, KUUDRA_NET_URL))
                .build();

        DiscordRPCIntegration.INSTANCE.updatePresence(presence);
    }

    private void updatePresenceLobby() {
        int runs = sessionRuns.get();
        int party = partySize.get();

        String details = "Session: " + runs + " run" + (runs != 1 ? "s" : "");
        String state = "Party (" + party + "/4)";

        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(details)
                .state(state)
                .startTimestamp(sessionStartEpochSecond)
                .largeImageKey("logo")
                .largeImageText("IQ Addons")
                .smallImageKey("skyblock")
                .smallImageText("SkyBlock")
                .activityType(ActivityType.PLAYING)
                .button(DiscordRichPresence.RPCButton.of(KUUDRA_NET_LABEL, KUUDRA_NET_URL))
                .build();

        DiscordRPCIntegration.INSTANCE.updatePresence(presence);
    }

    private void resetState() {
        sessionRuns.set(0);
        partySize.set(1);

        lastRunCompletedAt = 0L;
        sessionStartEpochSecond = 0L;

        presenceActive = false;
        insideKuudra = false;
    }

    private void requestPartyInfo() {
        try {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
        } catch (Exception e) {
            log.warn("Failed to request party info", e);
        }
    }
}