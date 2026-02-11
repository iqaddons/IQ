package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class TitleReceivedEvent implements Event, Cancellable {

    private final Text title;
    private final Text subtitle;

    private final String message;
    private final String strippedMessage;

    @Setter
    private boolean cancelled;

    public TitleReceivedEvent(@NotNull Text title, @NotNull Text subtitle) {
        this.title = title;
        this.subtitle = subtitle;

        this.message = TextFormatUtil.toLegacyString(title);
        this.strippedMessage = StringUtils.stripFormatting(message);
    }
}