package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Getter
@Setter
@RequiredArgsConstructor
public class ItemUseEvent implements Event, Cancellable {

    private final Hand hand;
    private final ItemStack itemStack;

    private boolean cancelled;

}
