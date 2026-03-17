package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public class SupplyTimerWidget extends HudWidget {

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private final List<SupplyPickupEntry> pickupHistory = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean persistUntilInstanceChange = false;

    public SupplyTimerWidget() {
        super(
                "supplyTimer",
                "Supply Timer",
                6.5f, 115.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseOneConfig.supplyTimers);
        setVisibilityCondition(() -> {
            var phase = KuudraStateManager.get().phase();
            boolean inTrackedPhases = KuudraPhase.isOneOf(
                    KuudraPhase.SUPPLIES, KuudraPhase.BUILD, KuudraPhase.EATEN,
                    KuudraPhase.STUN,  KuudraPhase.DPS, KuudraPhase.SKIP,
                    KuudraPhase.BOSS, KuudraPhase.COMPLETED
            ).test(phase);
            return inTrackedPhases || persistUntilInstanceChange;
        });

        setExampleLines(List.of(
                HudLine.of("§b§lSupply Times §8[§a4§8/§a6§8]"),
                HudLine.of("§bDarkJota §8(1/6) §f§l14.85s"),
                HudLine.of("§aPeHenrii §8(2/6) §f§l15.23s"),
                HudLine.of("§bckac10 §8(3/6) §f§l15.39s"),
                HudLine.of("§amennytb §8(4/6) §f§l16.04s"),
                HudLine.of("§bDarkJota §8(5/6) §9§l21.55s"),
                HudLine.of("§bckac10 §8(6/6) §9§l22.48s")
        ));
    }

    @Override
    protected void onActivate() {
        if (KuudraStateManager.get().phase() == KuudraPhase.SUPPLIES) {
            beginNewRunWindow();
        }

        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);

        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        resetLocalState();
    }

    private void resetLocalState() {
        pickupHistory.clear();
    }

    private void beginNewRunWindow() {
        persistUntilInstanceChange = true;
        supplyState.startSuppliesPhase();
        resetLocalState();
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.currentPhase() == KuudraPhase.SUPPLIES) {
            beginNewRunWindow();
            updateDisplay();
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) {
            persistUntilInstanceChange = false;
            resetLocalState();
            updateDisplay();
        }
    }

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        boolean stillInKuudraInstance = event.onSkyBlock() && event.newArea().contains(KUUDRA_AREA_ID);
        if (stillInKuudraInstance) {
            return;
        }

        persistUntilInstanceChange = false;
        resetLocalState();
        updateDisplay();
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        pickupHistory.add(new SupplyPickupEntry(
                event.playerName(),
                supplyState.getTimeColor(),
                event.currentSupply(),
                event.placedAt()
        ));

        updateDisplay();
    }

    private void updateDisplay() {
        clearLines();

        int totalCollected = supplyState.getSuppliesCollected();
        if (totalCollected == 0 && !pickupHistory.isEmpty()) {
            totalCollected = pickupHistory.size();
        }

        addLine(HudLine.of(String.format(
                "§b§lSupply Times §8[%s%d§8/§a6§8]",
                totalCollected >= 6 ? "§a" : "§e",
                totalCollected
        )));

        if (pickupHistory.isEmpty()) {
            addLine(HudLine.of("§7No placed supplies yet..."));
            markDimensionsDirty();
            return;
        }

        for (SupplyPickupEntry entry : pickupHistory) {
            addLine(HudLine.of(String.format(
                    "%s §8(%d/6) %s%.2fs",
                    entry.playerName(), entry.supplyNumber(),
                    entry.color, entry.pickupAt()
            )));
        }

        markDimensionsDirty();
    }

    private record SupplyPickupEntry(
            String playerName,
            String color,
            int supplyNumber,
            double pickupAt
    ) {
    }
}