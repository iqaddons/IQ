package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
@Slf4j
public class TitleReceivedEvent implements Event, Cancellable {

    private final Component title;
    private final Component subtitle;

    private final String message;
    private final String strippedMessage;

    @Setter
    private boolean cancelled;

    public TitleReceivedEvent(@Nullable Component title, @Nullable Component subtitle) {
        this.title = title == null ? Component.empty() : title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;

        Component primaryText = this.title.getString().isEmpty()
                ? this.subtitle
                : this.title;

        this.message = primaryText.getString();
        this.strippedMessage = StringUtils.stripFormatting(message);
    }
}