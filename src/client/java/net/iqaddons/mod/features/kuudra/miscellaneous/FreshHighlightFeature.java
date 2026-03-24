package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityGlowUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.iqaddons.mod.utils.EntityGlowUtil.PRIORITY_FRESH;

@Slf4j
public class FreshHighlightFeature extends KuudraFeature {

    private static final long FRESH_DURATION_MS = 10_000;

    private final Map<Integer, Long> freshPlayers = new ConcurrentHashMap<>();

    public FreshHighlightFeature() {
        super(
                "freshHighlightIndependent",
                "Fresh Highlight",
                () -> PhaseTwoConfig.freshHighlightIndependent,
                KuudraPhase.BUILD
        );
    }

    @Override
    protected void onKuudraActivate() {
        freshPlayers.clear();

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        freshPlayers.keySet().forEach(id -> EntityGlowUtil.removeGlowing(id, PRIORITY_FRESH));
        freshPlayers.clear();
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        int entityId = event.playerEntityId();
        freshPlayers.put(entityId, System.currentTimeMillis());

        RenderColor freshColor = RenderColor.fromArgb(PhaseTwoConfig.freshHighlightColor);
        EntityGlowUtil.setGlowing(entityId, freshColor, PRIORITY_FRESH);

        log.debug("Applied fresh teammate color to {} (id: {})", event.playerName(), entityId);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> iterator = freshPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            long elapsed = now - entry.getValue();

            if (elapsed >= FRESH_DURATION_MS) {
                EntityGlowUtil.removeGlowing(entry.getKey(), PRIORITY_FRESH);
                iterator.remove();
            }
        }
    }
}


