package net.iqaddons.mod.events;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@NoArgsConstructor
public final class EventBus {

    private static final Map<Class<? extends Event>, List<Subscription<?>>> SUBSCRIPTIONS = new ConcurrentHashMap<>();

    @NotNull
    public static <T extends Event> Subscription<T> subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return subscribe(eventType, handler, EventPriority.NORMAL);
    }

    @NotNull
    public static <T extends Event> Subscription<T> subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull EventPriority priority
    ) {
        var subscription = new Subscription<>(eventType, handler, priority);
        var subscriptions = SUBSCRIPTIONS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        subscriptions.add(subscription);
        subscriptions.sort(Comparator.comparingInt(s -> s.priority().ordinal()));

        log.debug("Subscribed to event: {} with priority {}", eventType.getSimpleName(), priority);
        return subscription;
    }

    public static void unsubscribe(@NotNull Subscription<?> subscription) {
        var subscriptions = SUBSCRIPTIONS.get(subscription.eventClass);
        if (subscriptions != null) {
            subscriptions.remove(subscription);
            log.debug("Unsubscribed from event: {}", subscription.eventClass.getSimpleName());
        }
    }

    @Contract("_ -> param1")
    @SuppressWarnings("unchecked")
    public static <T extends Event> @NotNull T post(@NotNull T event) {
        var subscriptions = SUBSCRIPTIONS.get(event.getClass());
        if (subscriptions == null || subscriptions.isEmpty()) {
            return event;
        }

        boolean isCancellable = event instanceof Cancellable;
        for (var subscription : subscriptions) {
            if (isCancellable && ((Cancellable) event).isCancelled()) {
                break;
            }

            try {
                ((Subscription<T>) subscription).handler().accept(event);
            } catch (Exception e) {
                log.error("Error while handling event: {}", event.getClass().getSimpleName(), e);
            }
        }

        return event;
    }

    public record Subscription<T extends Event>(
            Class<T> eventClass,
            Consumer<T> handler,
            EventPriority priority
    ) {

        public void unsubscribe() {
            EventBus.unsubscribe(this);
        }
    }

}