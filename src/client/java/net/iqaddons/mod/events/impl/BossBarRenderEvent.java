package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class BossBarRenderEvent implements Event, Cancellable {

    private final BossEvent bossBar;
    private final String title;
    private final String strippedTitle;

    @Setter
    private boolean cancelled = false;

    public BossBarRenderEvent(@NotNull BossEvent bossBar) {
        this.bossBar = bossBar;
        this.title = bossBar.getName().getString();
        this.strippedTitle = StringUtils.stripFormatting(title);
    }
}
