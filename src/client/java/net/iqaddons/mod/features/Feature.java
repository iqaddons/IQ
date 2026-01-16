package net.iqaddons.mod.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Getter
@RequiredArgsConstructor
public abstract class Feature {

    private final String id;
    private final String name;
    private final BooleanSupplier enabledSupplier;

    private boolean active = false;

    private final List<EventBus.Subscription<?>> subscriptions = new ArrayList<>();

    public final boolean isEnabled() {
        return enabledSupplier.getAsBoolean();
    }

    public final void activate() {
        if (active) return;
        active = true;
        onActivate();
    }

    public final void deactivate() {
        if (!active) return;
        active = false;

        subscriptions.forEach(EventBus.Subscription::unsubscribe);
        subscriptions.clear();

        onDeactivate();
    }

    protected <T extends Event> void subscribe(EventBus.Subscription<T> subscription) {
        subscriptions.add(subscription);
    }

    protected void onActivate() {}

    protected void onDeactivate() {}
}
