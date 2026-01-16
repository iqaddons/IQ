package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ScreenClickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class BlockUselessPerksFeature extends KuudraFeature {

    private static final Set<String> BLOCKED_PERKS = Set.of(
            "Steady Hands",
            "Bomberman",
            "Auto Revive",
            "Human Cannonball",
            "Elle's Lava Rod",
            "Elle's Pickaxe"
    );

    private static final Pattern BLOCKED_PATTERN = Pattern.compile(
            "(" + String.join("|", BLOCKED_PERKS) + ")( [IV]+)?",
            Pattern.CASE_INSENSITIVE
    );

    private static BlockUselessPerksFeature instance;

    public BlockUselessPerksFeature() {
        super(
                "blockUselessPerks",
                "Block Useless Perks",
                () -> PhaseThreeConfig.blockUselessPerks,
                KuudraPhase.STUN, KuudraPhase.DPS, KuudraPhase.BOSS
        );
        instance = this;
    }

    @Override
    protected void onKuudraActivate() {
        EventBus.subscribe(ScreenClickEvent.class, this::onScreenRender);
        log.info("Block Useless Perks activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Block Useless Perks deactivated");
    }

    private void onScreenRender(@NotNull ScreenClickEvent event) {
        String title = event.getScreen().getTitle().getString();
        if (!title.contains("Perk Menu")) return;

        String itemName = event.getSlot().getStack().getName().getString();
        if (shouldBlockPerk(itemName)) {
            event.setCancelled(true);
        }
    }

    public static boolean shouldBlockPerk(@NotNull String itemName) {
        if (instance == null || !instance.isActive()) {
            return false;
        }

        String stripped = itemName.replaceAll("§.", "");
        return BLOCKED_PATTERN.matcher(stripped).find();
    }
}
