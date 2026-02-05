package net.iqaddons.mod.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class Feature {

    private final String id;
    private final String name;
    private final BooleanSupplier enabledSupplier;

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final List<EventBus.Subscription<?>> subscriptions = new ArrayList<>();

    public final boolean isEnabled() {
        return enabledSupplier.getAsBoolean();
    }

    public final boolean isActive() {
        return active.get();
    }

    public final boolean activate() {
        if (!active.compareAndSet(false, true)) {
            log.debug("Feature {} already active", name);
            return false;
        }

        log.debug("Activating feature: {}", name);
        try {
            onActivate();
        } catch (Exception e) {
            log.error("Error activating feature {}", name, e);
            active.set(false);
            cleanupSubscriptions();
            return false;
        }

        return true;
    }

    public final boolean deactivate() {
        if (!active.compareAndSet(true, false)) {
            log.debug("Feature {} already inactive", name);
            return false;
        }

        log.debug("Deactivating feature: {}", name);
        cleanupSubscriptions();

        try {
            onDeactivate();
        } catch (Exception e) {
            log.error("Error deactivating feature {}", name, e);
        }

        return true;
    }

    protected <T extends Event> void subscribe(
            @NotNull Class<T> eventClass,
            @NotNull Consumer<T> handler
    ) {
        EventBus.Subscription<T> subscription = EventBus.subscribe(eventClass, handler);
        subscriptions.add(subscription);
    }

    protected void subscribe(@NotNull EventBus.Subscription<?> subscription) {
        subscriptions.add(subscription);
    }

    private void cleanupSubscriptions() {
        for (EventBus.Subscription<?> subscription : subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing", e);
            }
        }

        subscriptions.clear();
    }

    protected void onActivate() {}

    protected void onDeactivate() {}
}