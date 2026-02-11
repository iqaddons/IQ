package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.iqaddons.mod.IQConstants.ELLE_HEAD_OVER_MESSAGE;
import static net.iqaddons.mod.IQConstants.ELLE_NOT_AGAIN_MESSAGE;

@Slf4j
public class NoPreAlertFeature extends KuudraFeature {

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
        if (message.contains(ELLE_HEAD_OVER_MESSAGE)) {
            detectPreSpotFromPlayerPosition();
            return;
        }

        if (message.contains(ELLE_NOT_AGAIN_MESSAGE)) {
            if (kuudraState.phase() == KuudraPhase.SUPPLIES) {
                mc.execute(this::performSupplyCheck);
                log.info("Triggered no-pre check from Elle's 'Not again!' message");
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

    private void updateMissingPre(int missingPreValue, @NotNull String pileName) {
        int currentMissingPre = supplyState.getMissingPre();
        if (currentMissingPre != missingPreValue) {
            supplyState.setMissingPre(missingPreValue);
            log.info("Detected missing pre from party chat: {} (value: {})",
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
                log.info("Locked pre spot from Elle head-over message: {}", preSpot.getDisplayName());
            }
        }
    }

    private void updateSupplyPositions() {
        if (mc.world == null) return;

        List<SupplyPosition> supplies = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof GiantEntity giant) {
                Vec3d pos = giant.getEntityPos();
                SupplyPosition supply = SupplyPosition.fromGiant(
                        pos.x,
                        pos.z,
                        giant.getYaw(),
                        giant.getId()
                );
                supplies.add(supply);
            }
        }

        supplyState.updateSupplyPositions(supplies);
        log.info("Updated supply positions: {} supplies found", supplies.size());
    }

    private void performSupplyCheck() {
        if (mc.player == null) return;

        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) {
            Vec3d playerPos = mc.player.getEntityPos();
            boolean detected = supplyState.tryDetectPreSpot(playerPos);
            if (!detected) {
                MessageUtil.ERROR.sendMessage("Could not determine your pre spot (too far away?)");
                log.warn("Failed to detect pre spot at position: {}", playerPos);
                return;
            }

            preSpot = supplyState.getDetectedPreSpot();
            if (preSpot == null) {
                log.error("Pre spot is null after successful detection!");
                return;
            }
        }

        log.info("Detected pre spot: {}", preSpot.getDisplayName());
        updateSupplyPositions();

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            supplyState.setMissingPre(preSpot.getMissingPreValue());
            MessageUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!");
            log.info("No pre supply detected for {}, announced to party", preSpot.getDisplayName());
        }

        if (preSpot.hasSecondaryLocation()) {
            Boolean hasSecondary = supplyState.hasSecondarySupply();
            if (hasSecondary != null && !hasSecondary) {
                MessageUtil.PARTY.sendMessage("No " + preSpot.getSecondaryName() + "!");
                log.info("No secondary supply detected for {}, announced to party",
                        preSpot.getSecondaryName());
            } else if (hasSecondary != null) {
                log.info("Secondary supply found for {}", preSpot.getSecondaryName());
            }
        }

        if (hasPre) {
            log.info("Pre supply found for {}", preSpot.getDisplayName());
        }
    }
}