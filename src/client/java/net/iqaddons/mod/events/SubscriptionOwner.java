package net.iqaddons.mod.events;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public abstract class SubscriptionOwner {

    private final List<EventBus.Subscription<?>> subscriptions = new CopyOnWriteArrayList<>();

    protected final <T extends Event> EventBus.@NotNull Subscription<T> subscribe(
            @NotNull Class<T> eventClass,
            @NotNull Consumer<T> handler
    ) {
        EventBus.Subscription<T> subscription = EventBus.subscribe(eventClass, handler);
        subscriptions.add(subscription);
        return subscription;
    }

    protected final void trackSubscription(@NotNull EventBus.Subscription<?> subscription) {
        subscriptions.add(subscription);
    }

    protected final void clearSubscriptions() {
        for (EventBus.Subscription<?> subscription : subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing", e);
            }
        }

        subscriptions.clear();
    }
}
