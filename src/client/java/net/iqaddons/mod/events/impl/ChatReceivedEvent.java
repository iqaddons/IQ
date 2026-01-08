package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.Event;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class ChatReceivedEvent extends Event {

    private final Text text;
    private final String message;

    public ChatReceivedEvent(@NotNull Text text) {
        this.text = text;
        this.message = text.getString();
    }

    public boolean contains(@NotNull String str) {
        return message.toLowerCase().contains(str.toLowerCase());
    }

    public boolean startsWith(String prefix) {
        return message.startsWith(prefix);
    }
}
