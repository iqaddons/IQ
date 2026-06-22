package net.iqaddons.mod.events.dispatcher;

import net.iqaddons.mod.events.SubscriptionOwner;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.minecraft.client.Minecraft;

public abstract class EventDispatcher
        extends SubscriptionOwner
        implements LifecycleComponent
{

    protected static final Minecraft client = Minecraft.getInstance();
}
