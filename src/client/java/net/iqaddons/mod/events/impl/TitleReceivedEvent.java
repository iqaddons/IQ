package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Getter
@RequiredArgsConstructor
@Slf4j
public class TitleReceivedEvent implements Event, Cancellable {

    private final Text title;
    private final Text subtitle;

    private final String message;
    private final String strippedMessage;

    @Setter
    private boolean cancelled;

    public TitleReceivedEvent(@Nullable Text title, @Nullable Text subtitle) {
        this.title = title == null ? Text.empty() : title;
        this.subtitle = subtitle == null ? Text.empty() : subtitle;

        Text primaryText = this.title.getString().isEmpty()
                ? this.subtitle
                : this.title;

        this.message = primaryText.getString();
        this.strippedMessage = StringUtils.stripFormatting(message);
    }
}