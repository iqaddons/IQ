package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class ChatReceivedEvent implements Event, Cancellable {

    private final Component text;
    private final String message;
    private final String strippedMessage;

    @Setter
    private boolean cancelled;

    public ChatReceivedEvent(@NotNull Component text) {
        this.text = text;
        // Extract and normalize message immediately to avoid race conditions on Netty thread
        String rawMessage = text.getString();
        this.message = rawMessage != null ? rawMessage : "";
        this.strippedMessage = this.message.isBlank() ? "" : StringUtils.stripFormatting(this.message);
    }

    public boolean contains(@NotNull String str) {
        return message.toLowerCase().contains(str.toLowerCase());
    }

    public boolean startsWith(String prefix) {
        return message.startsWith(prefix);
    }

}
