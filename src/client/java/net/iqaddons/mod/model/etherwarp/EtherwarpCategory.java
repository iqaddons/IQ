package net.iqaddons.mod.model.etherwarp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;

/**
 * Representa uma categoria de waypoints do Etherwarp Helper.
 * Agrupa waypoints relacionados para melhor organização e controle.
 */
public record EtherwarpCategory(
        @NotNull String name,
        boolean enabled,
        @NotNull @UnmodifiableView List<EtherwarpWaypoint> waypoints
) {

    public EtherwarpCategory {
        if (waypoints instanceof List && !waypoints.isEmpty()) {
            waypoints = Collections.unmodifiableList(waypoints);
        } else {
            waypoints = Collections.emptyList();
        }
    }

    /**
     * Retorna a quantidade de waypoints válidos nesta categoria.
     */
    public int getValidWaypointCount() {
        return (int) waypoints.stream().filter(EtherwarpWaypoint::isValid).count();
    }

    /**
     * Verifica se a categoria tem waypoints válidos.
     */
    public boolean hasValidWaypoints() {
        return getValidWaypointCount() > 0;
    }
}

