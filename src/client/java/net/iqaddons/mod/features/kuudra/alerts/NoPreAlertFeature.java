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
import net.iqaddons.mod.utils.ServerUtils;
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
    private static final long MAX_LAG_BUDGET_MS = 4_000L;
    private static final long PING_BASELINE_MS = 120L;
    private static final long FALLBACK_CONFIRM_DELAY_MS = 350L;
    private static final int MIN_EMPTY_SCANS_FOR_FALLBACK = 3;
    private static final long SUPPLY_SPAWN_DESYNC_GRACE_MS = 250L;
    private static final int PARTIAL_SPAWN_MAX_SUPPLIES = 2;

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
    private boolean fallbackConfirmationPending = false;
    private long fallbackConfirmationStartMs = 0L;
    private long firstSupplySeenAtMs = 0L;
    private boolean graceRecheckAttempted = false;
    private int consecutiveEmptyScans = 0;
    private boolean preSpotDetectionAnnounced = false;

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
        if (!supplies.isEmpty()) {
            long now = System.currentTimeMillis();
            if (firstSupplySeenAtMs == 0L) {
                firstSupplySeenAtMs = now;
            }

            consecutiveEmptyScans = 0;
            fallbackConfirmationPending = false;
            if (!carrierCheckAttempted) {
                carrierCheckAttempted = true;
                queueSupplyCheck("supplier giant spawn", false);
            } else if (!graceRecheckAttempted
                    && now - firstSupplySeenAtMs >= SUPPLY_SPAWN_DESYNC_GRACE_MS) {
                graceRecheckAttempted = true;
                queueSupplyCheck("spawn desync grace recheck", false);
            }
            return;
        }

        firstSupplySeenAtMs = 0L;
        graceRecheckAttempted = false;
        consecutiveEmptyScans++;

        if (!timedCheckAttempted) {
            long elapsedMs = supplyState.getElapsedTimeMillis();
            long adaptiveFallbackDelayMs = getAdaptiveFallbackDelayMs();

            if (elapsedMs >= adaptiveFallbackDelayMs) {
                if (!fallbackConfirmationPending) {
                    fallbackConfirmationPending = true;
                    fallbackConfirmationStartMs = System.currentTimeMillis();
                    log.debug("Starting no-pre fallback confirmation (elapsed={}ms, delay={}ms, emptyScans={})",
                            elapsedMs, adaptiveFallbackDelayMs, consecutiveEmptyScans);
                    return;
                }

                long confirmationElapsed = System.currentTimeMillis() - fallbackConfirmationStartMs;
                if (confirmationElapsed >= FALLBACK_CONFIRM_DELAY_MS
                        && consecutiveEmptyScans >= MIN_EMPTY_SCANS_FOR_FALLBACK) {
                    timedCheckAttempted = true;
                    fallbackConfirmationPending = false;
                    queueSupplyCheck("adaptive fallback timer", false);
                }
            }
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
        fallbackConfirmationPending = false;
        fallbackConfirmationStartMs = 0L;
        firstSupplySeenAtMs = 0L;
        graceRecheckAttempted = false;
        consecutiveEmptyScans = 0;
        preSpotDetectionAnnounced = false;
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
                log.debug("Deferred no-pre check from {} until state is stable", trigger);
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
                announceDetectedPreSpot(preSpot, "Elle head-over message");
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

            announceDetectedPreSpot(preSpot, "supply check");
        }

        log.debug("Detected pre spot: {}", preSpot.getDisplayName());
        List<SupplyPosition> supplies = updateSupplyPositions();

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            if (shouldWaitForSpawnDesync(supplies)) {
                log.debug("Deferring no-pre alert for {} due to spawn desync grace (supplies={})",
                        preSpot.getDisplayName(), supplies.size());
                return false;
            }

            supplyState.setMissingPre(preSpot.getMissingPreValue());
            MessageUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!");
            log.debug("No pre supply detected for {}, announced to party", preSpot.getDisplayName());
        }

        if (preSpot.hasSecondaryLocation()) {
            Boolean hasSecondary = supplyState.hasSecondarySupply();
            if (hasSecondary != null && !hasSecondary) {
                if (shouldWaitForSpawnDesync(supplies)) {
                    log.debug("Deferring secondary alert for {} due to spawn desync grace (supplies={})",
                            preSpot.getSecondaryName(), supplies.size());
                    return false;
                }

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

    private void announceDetectedPreSpot(@NotNull PreSpot preSpot, @NotNull String source) {
        if (preSpotDetectionAnnounced) {
            return;
        }

        preSpotDetectionAnnounced = true;
        MessageUtil.sendFormattedMessage("&fPre Spot detected: &e" + preSpot.getDisplayName() + "&f!");
        log.debug("Announced detected pre spot {} from {}", preSpot.getDisplayName(), source);
    }

    private boolean shouldWaitForSpawnDesync(@NotNull List<SupplyPosition> supplies) {
        if (supplies.isEmpty() || supplies.size() > PARTIAL_SPAWN_MAX_SUPPLIES) {
            return false;
        }

        if (firstSupplySeenAtMs == 0L) {
            return false;
        }

        long elapsedSinceFirstSupplyMs = System.currentTimeMillis() - firstSupplySeenAtMs;
        return elapsedSinceFirstSupplyMs < SUPPLY_SPAWN_DESYNC_GRACE_MS;
    }

    private long getAdaptiveFallbackDelayMs() {
        float averageTps = ServerUtils.getAverageTps();
        long averagePingMs = ServerUtils.getAveragePing().toMillis();

        long tpsBudgetMs = Math.max(0L, Math.round((20.0f - averageTps) * 220.0f));
        long pingBudgetMs = averagePingMs > PING_BASELINE_MS
                ? (averagePingMs - PING_BASELINE_MS) * 2L
                : 0L;

        long lagBudgetMs = Math.min(MAX_LAG_BUDGET_MS, tpsBudgetMs + pingBudgetMs);
        return EARLY_CHECK_DELAY_MS + lagBudgetMs;
    }
}
