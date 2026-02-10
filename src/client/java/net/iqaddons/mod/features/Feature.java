package net.iqaddons.mod.features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.SubscriptionOwner;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class Feature extends SubscriptionOwner {

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String id;
    private final String name;
    private final BooleanSupplier enabledSupplier;

    private final AtomicBoolean active = new AtomicBoolean(false);

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
            clearSubscriptions();
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
        clearSubscriptions();

        try {
            onDeactivate();
        } catch (Exception e) {
            log.error("Error deactivating feature {}", name, e);
        }

        return true;
    }

    protected void onActivate() {}

    protected void onDeactivate() {}
}