package net.iqaddons.mod.events;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@NoArgsConstructor
public final class EventBus {

    private static final Map<Class<?>, List<EventHandler>> HANDLERS = new ConcurrentHashMap<>();
    private static final Set<Object> REGISTERED_LISTENERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(Object listener) {
        if (listener == null || REGISTERED_LISTENERS.contains(listener)) {
            return;
        }

        REGISTERED_LISTENERS.add(listener);
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;

            if (method.getParameterCount() != 1) {
                log.warn("@Subscribe method {} must have exactly one parameter", method.getName());
                continue;
            }

            Class<?> eventType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(eventType)) {
                log.warn("@Subscribe method {} parameter must extend Event", method.getName());
                continue;
            }

            method.setAccessible(true);
            EventHandler handler = new EventHandler(listener, method, annotation.priority());

            HANDLERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
            HANDLERS.get(eventType).sort(Comparator.comparingInt(EventHandler::priority).reversed());

            log.debug("Registered handler: {}.{} for {}",
                    listener.getClass().getSimpleName(), method.getName(), eventType.getSimpleName());
        }
    }

    public static void unregister(Object listener) {
        if (listener == null || !REGISTERED_LISTENERS.remove(listener)) {
            return;
        }

        HANDLERS.values().forEach(handlers ->
                handlers.removeIf(h -> h.listener() == listener)
        );

        log.debug("Unregistered listener: {}", listener.getClass().getSimpleName());
    }

    @Contract("_ -> param1")
    public static <T extends Event> @NotNull T post(@NotNull T event) {
        List<EventHandler> handlers = HANDLERS.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return event;
        }

        for (EventHandler handler : handlers) {
            try {
                handler.invoke(event);
            } catch (Exception e) {
                log.error("Error invoking event handler {}.{}",
                        handler.listener().getClass().getSimpleName(),
                        handler.method().getName(), e);
            }
        }

        return event;
    }

    public static void clear() {
        HANDLERS.clear();
        REGISTERED_LISTENERS.clear();
    }

    private record EventHandler(
            Object listener,
            Method method,
            int priority
    ) {

        void invoke(Event event) throws Exception {
            method.invoke(listener, event);
        }
    }
}