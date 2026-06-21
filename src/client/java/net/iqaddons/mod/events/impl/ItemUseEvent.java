package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

@Getter
@Setter
@RequiredArgsConstructor
public class ItemUseEvent implements Event, Cancellable {

    private final InteractionHand hand;
    private final ItemStack itemStack;

    private boolean cancelled;

}
