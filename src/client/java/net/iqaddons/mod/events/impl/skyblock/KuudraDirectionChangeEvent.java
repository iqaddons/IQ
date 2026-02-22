package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.KuudraLocationUtil;

public record KuudraDirectionChangeEvent(
        KuudraLocationUtil.SpawnDirection oldDirection,
        KuudraLocationUtil.SpawnDirection newDirection
) implements Event {
}
