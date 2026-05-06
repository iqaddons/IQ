package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.CratePriorityConfigLoader;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.CratePriorityHudEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.PreSpot;
import net.iqaddons.mod.utils.NoPreMessageParser;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.iqaddons.mod.IQConstants.ELLE_HEAD_OVER_MESSAGE;

@Slf4j
public class CratePriorityFeature extends KuudraFeature {

    private static final int SCAN_INTERVAL_TICKS = 2;

    private static final int MISSING_X = 1;
    private static final int MISSING_X_CANNON = 2;
    private static final int MISSING_SQUARE = 3;
    private static final int MISSING_SLASH = 4;
    private static final int MISSING_EQUALS = 5;
    private static final int MISSING_TRIANGLE = 6;
    private static final int MISSING_SHOP = 7;

    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final CratePriorityConfigLoader cratePriorityConfig = CratePriorityConfigLoader.get();

    private int pendingMissingPre = 0;
    private @NotNull String lastDecisionKey = "";

    public CratePriorityFeature() {
        super(
                "cratePriority",
                "Crate Priority",
                () -> PhaseOneConfig.cratePriority,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        resetState();
        cratePriorityConfig.load();

        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
    }

    @Override
    protected void onKuudraDeactivate() {
        resetState();
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra() || event.isExitingKuudra()) {
            resetState();
        }
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        mc.execute(() -> handleChatMessage(message));
    }

    private void handleChatMessage(@NotNull String message) {
        if (currentPhase() != KuudraPhase.SUPPLIES) return;

        if (message.contains(ELLE_HEAD_OVER_MESSAGE)) {
            resolveCurrentPreSpot();
            tryDispatchPriority("elle chat");
            return;
        }

        if (!message.startsWith("Party >")) return;

        NoPreMessageParser.ParsedNoPreCall parsed = NoPreMessageParser.parse(message);
        if (parsed == null) {
            return;
        }

        pendingMissingPre = parsed.missingPreValue();
        tryDispatchPriority("party no-pre");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(SCAN_INTERVAL_TICKS)) return;
        if (currentPhase() != KuudraPhase.SUPPLIES || pendingMissingPre <= 0) return;

        tryDispatchPriority("tick retry");
    }

    private void tryDispatchPriority(@NotNull String source) {
        if (pendingMissingPre <= 0 || currentPhase() != KuudraPhase.SUPPLIES) {
            return;
        }

        PreSpot currentPre = resolveCurrentPreSpot();
        if (currentPre == null) {
            return;
        }

        String destination = getDestination(currentPre, pendingMissingPre);
        if (destination == null) {
            log.debug("No crate priority mapping for currentPre={} missingPre={}", currentPre, pendingMissingPre);
            return;
        }

        String decisionKey = currentPre.name() + ":" + pendingMissingPre + ":" + destination;
        if (decisionKey.equals(lastDecisionKey)) {
            return;
        }

        lastDecisionKey = decisionKey;

        int durationTicks = Math.max(20, Math.min(200, PhaseOneConfig.CratePriorityConfig.cratePriorityDurationSeconds * 20));
        EventBus.post(new CratePriorityHudEvent("Go " + destination, durationTicks));
        log.debug("Crate priority update from {} => pre={}, missing={}, action=Go {}",
                source, currentPre, pendingMissingPre, destination);
    }

    private @Nullable PreSpot resolveCurrentPreSpot() {
        PreSpot detected = supplyState.getDetectedPreSpot();
        if (detected != null) {
            return detected;
        }

        if (mc.player == null) {
            return null;
        }

        Vec3d playerPos = mc.player.getEntityPos();
        if (!supplyState.tryDetectPreSpot(playerPos)) {
            return null;
        }

        return supplyState.getDetectedPreSpot();
    }

    private @Nullable String getDestination(@NotNull PreSpot currentPre, int missingPre) {
        String overrideDestination = cratePriorityConfig.getDestinationOverride(currentPre, missingPre);
        if (overrideDestination != null) {
            return overrideDestination;
        }

        return getDefaultDestination(currentPre, missingPre);
    }

    private @Nullable String getDefaultDestination(@NotNull PreSpot currentPre, int missingPre) {
        return switch (currentPre) {
            case X -> switch (missingPre) {
                case MISSING_X -> "Shop";
                case MISSING_X_CANNON -> "Square";
                case MISSING_SLASH, MISSING_SQUARE, MISSING_EQUALS, MISSING_TRIANGLE, MISSING_SHOP -> "X Cannon";
                default -> null;
            };
            case SLASH -> switch (missingPre) {
                case MISSING_SLASH -> "Shop";
                case MISSING_SQUARE -> "X Cannon";
                case MISSING_X, MISSING_X_CANNON, MISSING_EQUALS, MISSING_TRIANGLE, MISSING_SHOP -> "Square";
                default -> null;
            };
            case EQUALS -> switch (missingPre) {
                case MISSING_EQUALS, MISSING_SQUARE, MISSING_X_CANNON -> "Shop";
                case MISSING_SHOP, MISSING_TRIANGLE, MISSING_X, MISSING_SLASH -> "Square";
                default -> null;
            };
            case TRIANGLE -> switch (missingPre) {
                case MISSING_TRIANGLE, MISSING_SQUARE, MISSING_X_CANNON -> "Shop";
                case MISSING_X, MISSING_SHOP -> "X Cannon";
                case MISSING_SLASH, MISSING_EQUALS -> "Square";
                default -> null;
            };
        };
    }

    private void resetState() {
        pendingMissingPre = 0;
        lastDecisionKey = "";
    }
}

