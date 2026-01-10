package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class ChatReceivedEvent implements Event, Cancellable {

    private final Text text;
    private final String message;
    private final String strippedMessage;

    @Setter
    private boolean cancelled;

    public ChatReceivedEvent(@NotNull Text text) {
        this.text = text;
        this.message = text.getString();
        this.strippedMessage = stripFormatting(message);
    }

    public boolean contains(@NotNull String str) {
        return message.toLowerCase().contains(str.toLowerCase());
    }

    public boolean startsWith(String prefix) {
        return message.startsWith(prefix);
    }

    private static String stripFormatting(String text) {
        return text == null ? "" : text.replaceAll("§[0-9a-fk-or]", "");
    }
}
