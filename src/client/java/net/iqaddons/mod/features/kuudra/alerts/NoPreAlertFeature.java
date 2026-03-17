package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.iqaddons.mod.IQConstants.ELLE_HEAD_OVER_MESSAGE;
import static net.iqaddons.mod.IQConstants.ELLE_NOT_AGAIN_MESSAGE;

@Slf4j
public class NoPreAlertFeature extends KuudraFeature {

    private static final int SUPPLY_SCAN_INTERVAL_TICKS = 2;
    private static final long EARLY_CHECK_DELAY_MS = 10_000L;

    private static final Pattern PARTY_NO_PRE_PATTERN = Pattern.compile(
            "Party > (?:\\[[^]]+] )?\\w+: (?:\\[IQ] )?[Nn]o\\s+(Triangle|Equals|Slash|Shop|X Cannon|X|Square|tri|eq|xc)!?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NO_PRE_SIMPLE_PATTERN = Pattern.compile(
            "(?:[Nn]o|[Mm]issing)\\s+(triangle|equals|slash|shop|x cannon|square|tri|eq|x|xc)(?:\\s|!|$)",
            Pattern.CASE_INSENSITIVE
    );

    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final KuudraStateManager kuudraState = KuudraStateManager.get();

    private boolean supplyCheckCompleted = false;
    private boolean carrierCheckAttempted = false;
    private boolean timedCheckAttempted = false;

    public NoPreAlertFeature() {
        super(
                "noPreAlert",
                "No Pre Alert",
                () -> PhaseOneConfig.noPreAlert,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        resetForSuppliesPhase();

        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            resetForSuppliesPhase();
            log.debug("Reset supply state for new run and started No Pre Alert timer");
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(SUPPLY_SCAN_INTERVAL_TICKS)) return;
        if (supplyCheckCompleted || kuudraState.phase() != KuudraPhase.SUPPLIES) return;

        List<SupplyPosition> supplies = updateSupplyPositions();
        if (!carrierCheckAttempted && !supplies.isEmpty()) {
            carrierCheckAttempted = true;
            queueSupplyCheck("supplier giant spawn", false);
            return;
        }

        if (!timedCheckAttempted && supplyState.getElapsedTimeMillis() >= EARLY_CHECK_DELAY_MS) {
            timedCheckAttempted = true;
            queueSupplyCheck("10.0s fallback timer", false);
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(ELLE_HEAD_OVER_MESSAGE)) {
            detectPreSpotFromPlayerPosition();
            return;
        }

        if (message.contains(ELLE_NOT_AGAIN_MESSAGE)) {
            if (kuudraState.phase() == KuudraPhase.SUPPLIES) {
                queueSupplyCheck("Elle's 'Not again!' message", true);
            }

            return;
        }

        if (!message.startsWith("Party >")) return;
        Matcher matcher = PARTY_NO_PRE_PATTERN.matcher(message);
        if (matcher.find()) {
            String pileName = matcher.group(1);
            int missingPreValue = PreSpot.getMissingPreValueFromPileName(pileName);
            if (missingPreValue > 0) {
                updateMissingPre(missingPreValue, pileName);
            }
            return;
        }

        Matcher simpleMatcher = NO_PRE_SIMPLE_PATTERN.matcher(message);
        if (simpleMatcher.find()) {
            String pileName = simpleMatcher.group(1);
            int missingPreValue = PreSpot.getMissingPreValueFromPileName(pileName);
            if (missingPreValue > 0) {
                updateMissingPre(missingPreValue, pileName);
            }
        }
    }

    private void resetForSuppliesPhase() {
        supplyState.reset();
        supplyState.startSuppliesPhase();
        supplyCheckCompleted = false;
        carrierCheckAttempted = false;
        timedCheckAttempted = false;
    }

    private void queueSupplyCheck(@NotNull String trigger, boolean notifyDetectionFailure) {
        if (supplyCheckCompleted || kuudraState.phase() != KuudraPhase.SUPPLIES) return;

        mc.execute(() -> {
            if (supplyCheckCompleted || kuudraState.phase() != KuudraPhase.SUPPLIES) return;

            boolean checked = performSupplyCheck(notifyDetectionFailure);
            if (checked) {
                supplyState.markNoPreCheckCompleted();
                supplyCheckCompleted = true;
                log.debug("Triggered no-pre check from {}", trigger);
            } else {
                log.debug("Skipped no-pre check from {} because pre spot was not ready yet", trigger);
            }
        });
    }

    private void updateMissingPre(int missingPreValue, @NotNull String pileName) {
        int currentMissingPre = supplyState.getMissingPre();
        if (currentMissingPre != missingPreValue) {
            supplyState.setMissingPre(missingPreValue);
            log.debug("Detected missing pre from party chat: {} (value: {})",
                    pileName, missingPreValue);
        }
    }

    private void detectPreSpotFromPlayerPosition() {
        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getEntityPos();
        boolean detected = supplyState.tryDetectPreSpot(playerPos);
        if (detected) {
            PreSpot preSpot = supplyState.getDetectedPreSpot();
            if (preSpot != null) {
                log.debug("Locked pre spot from Elle head-over message: {}", preSpot.getDisplayName());
            }
        }
    }

    private @NotNull List<SupplyPosition> updateSupplyPositions() {
        List<SupplyPosition> supplies = EntityDetectorUtil.getSupplyCarriers().stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYaw(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(supplies);
        log.debug("Updated supply positions: {} supplies found", supplies.size());
        return supplies;
    }

    private boolean performSupplyCheck(boolean notifyDetectionFailure) {
        if (mc.player == null) return false;

        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) {
            Vec3d playerPos = mc.player.getEntityPos();
            boolean detected = supplyState.tryDetectPreSpot(playerPos);
            if (!detected) {
                if (notifyDetectionFailure) {
                    MessageUtil.ERROR.sendMessage("Could not determine your pre spot (too far away?)");
                    log.warn("Failed to detect pre spot at position: {}", playerPos);
                } else {
                    log.debug("Pre spot not ready yet at position: {}", playerPos);
                }
                return false;
            }

            preSpot = supplyState.getDetectedPreSpot();
            if (preSpot == null) {
                log.error("Pre spot is null after successful detection!");
                return false;
            }
        }

        log.debug("Detected pre spot: {}", preSpot.getDisplayName());
        updateSupplyPositions();

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            supplyState.setMissingPre(preSpot.getMissingPreValue());
            MessageUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!");
            log.debug("No pre supply detected for {}, announced to party", preSpot.getDisplayName());
        }

        if (preSpot.hasSecondaryLocation()) {
            Boolean hasSecondary = supplyState.hasSecondarySupply();
            if (hasSecondary != null && !hasSecondary) {
                MessageUtil.PARTY.sendMessage("No " + preSpot.getSecondaryName() + "!");
                log.debug("No secondary supply detected for {}, announced to party",
                        preSpot.getSecondaryName());
            } else if (hasSecondary != null) {
                log.debug("Secondary supply found for {}", preSpot.getSecondaryName());
            }
        }

        if (hasPre) {
            log.debug("Pre supply found for {}", preSpot.getDisplayName());
        }

        return true;
    }
}