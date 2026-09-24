package net.iqaddons.mod.events.impl;

import lombok.Getter;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@Getter
public class ActionBarReceivedEvent implements Event {

    private final Component text;
    private final String message;
    private final String strippedMessage;

    public ActionBarReceivedEvent(@NotNull Component text) {
        this.text = text;
        String rawMessage = text.getString();
        this.message = rawMessage != null ? rawMessage : "";
        this.strippedMessage = this.message.isBlank() ? "" : StringUtils.stripFormatting(this.message);
    }
}
