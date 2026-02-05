package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.state.supply.PreSpot;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class NoPreAlertFeature extends KuudraFeature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String ELLE_FISHING_MESSAGE = "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!";
    private static final long PRE_SPOT_DETECTION_DELAY_MS = 9000;
    private static final long SUPPLY_CHECK_DELAY_MS = 11500;

    private static final Pattern PARTY_NO_PRE_PATTERN = Pattern.compile(
            "Party > (?:\\[[^]]+] )?\\w+: [Nn]o (Triangle|Equals|Slash|Shop|X Cannon|Square|tri|eq|x)!?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NO_PRE_SIMPLE_PATTERN = Pattern.compile(
            "(?:[Nn]o|[Mm]issing)\\s+(triangle|equals|slash|shop|x cannon|square|tri|eq|x)(?:\\s|!|$)",
            Pattern.CASE_INSENSITIVE
    );

    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final ScheduledExecutorService scheduler;

    public NoPreAlertFeature(ScheduledExecutorService scheduler) {
        super(
                "noPreAlert",
                "No Pre Alert",
                () -> PhaseOneConfig.noPreAlert,
                KuudraPhase.SUPPLIES
        );

        this.scheduler = scheduler;
    }

    @Override
    protected void onKuudraActivate() {
        supplyState.reset();

        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyState.reset();
            log.debug("Reset supply state for new run");
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(ELLE_FISHING_MESSAGE)) {
            supplyState.startSuppliesPhase();
            scheduler.schedule(this::detectPreSpot, PRE_SPOT_DETECTION_DELAY_MS, TimeUnit.MILLISECONDS);
            scheduler.schedule(this::checkSupplies, SUPPLY_CHECK_DELAY_MS, TimeUnit.MILLISECONDS);
            return;
        }

        detectNoPreFromPartyChat(message);
    }

    private void detectNoPreFromPartyChat(@NotNull String message) {
        if (!message.startsWith("Party >")) return;

        Matcher matcher = PARTY_NO_PRE_PATTERN.matcher(message);
        if (matcher.find()) {
            String preSpotName = matcher.group(1);
            PreSpot detectedPre = PreSpot.fromMessage(preSpotName);

            if (detectedPre != null) updateMissingPre(detectedPre);
            return;
        }

        Matcher simpleMatcher = NO_PRE_SIMPLE_PATTERN.matcher(message);
        if (simpleMatcher.find()) {
            String preSpotName = simpleMatcher.group(1);
            PreSpot detectedPre = PreSpot.fromMessage(preSpotName);

            if (detectedPre != null) updateMissingPre(detectedPre);
        }
    }

    private void updateMissingPre(@NotNull PreSpot preSpot) {
        int missingPreValue = preSpot.getMissingPreValue();
        int currentMissingPre = supplyState.getMissingPre();

        if (currentMissingPre != missingPreValue) {
            supplyState.setMissingPre(missingPreValue);
            log.debug("Detected missing pre from party chat: {} (value: {})",
                    preSpot.getDisplayName(), missingPreValue);
        }
    }

    private void detectPreSpot() {
        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getEntityPos();
        boolean detected = supplyState.tryDetectPreSpot(playerPos);

        if (!detected) {
            mc.execute(() -> MessageUtil.ERROR.sendMessage("Could not determine your pre spot (too far away?)"));
        }
    }

    private void checkSupplies() {
        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) return;

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            supplyState.setMissingPre(preSpot.getMissingPreValue());
            mc.execute(() -> MessageUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!"));
            log.debug("No pre supply detected for {}, missingPre set to {}",
                    preSpot.getDisplayName(), preSpot.getMissingPreValue());
            return;
        }

        Boolean hasSecondary = supplyState.hasSecondarySupply();
        if (hasSecondary != null && !hasSecondary && preSpot.getSecondaryName() != null) {
            mc.execute(() -> MessageUtil.PARTY.sendMessage("No " + preSpot.getSecondaryName() + "!"));
        }
    }
}