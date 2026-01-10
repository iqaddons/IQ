package net.iqaddons.mod.events;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
        var subscription = new Subscription<>(eventType, handler);
        SUBSCRIPTIONS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(subscription);

        log.debug("Subscribed to event: {}", eventType.getSimpleName());
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

        for (var subscription : subscriptions) {
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
            Consumer<T> handler
    ) {

        public void unsubscribe() {
            EventBus.unsubscribe(this);
        }
    }

}