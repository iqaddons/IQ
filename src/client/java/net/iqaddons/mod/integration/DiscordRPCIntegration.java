package net.iqaddons.mod.integration;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper around the Discord IPC client for Rich Presence updates.
 * <p>
 * Portions of this code are from the SkyBlock-RPC mod.
 */
@Slf4j
public final class DiscordRPCIntegration {

    public static final DiscordRPCIntegration INSTANCE = new DiscordRPCIntegration();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private @Nullable DiscordRpc rpc;

    public void connect(long clientId) {
        if (initialized.get()) {
            log.debug("Discord RPC already initialized");
            return;
        }

        try {
            rpc = new DiscordRpc();

            RPCEventHandler handler = new RPCEventHandler() {
                @Override
                public void ready(User user) {
                    log.info("Discord RPC ready — connected as {}", user.getUsername());
                }

                @Override
                public void disconnected(ErrorCode errorCode, String message) {
                    log.warn("Discord RPC disconnected: {} - {}", errorCode, message);
                }

                @Override
                public void errored(ErrorCode errorCode, String message) {
                    log.error("Discord RPC error: {} - {}", errorCode, message);
                }
            };

            rpc.init(String.valueOf(clientId), handler, false);
            initialized.set(true);
            log.info("Discord RPC initialized with client ID: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to initialize Discord RPC", e);
            rpc = null;
            initialized.set(false);
        }
    }

    /**
     * Updates the Discord Rich Presence.
     *
     * @param presence the built {@link DiscordRichPresence} to display
     */
    public void updatePresence(@NotNull DiscordRichPresence presence) {
        if (!initialized.get() || rpc == null) return;

        try {
            rpc.updatePresence(presence);
        } catch (Exception e) {
            log.warn("Failed to update Discord Rich Presence", e);
        }
    }

    /**
     * Clears the current Rich Presence from the user's profile.
     */
    public void clearPresence() {
        if (!initialized.get() || rpc == null) return;

        try {
            rpc.updatePresence(DiscordRichPresence.builder().build());
        } catch (Exception e) {
            log.warn("Failed to clear Discord Rich Presence", e);
        }
    }

    /**
     * Shuts down the Discord RPC connection and clears the presence.
     */
    public void shutdown() {
        if (!initialized.compareAndSet(true, false)) return;

        try {
            if (rpc != null) {
                rpc.updatePresence(DiscordRichPresence.builder().build());
                rpc.shutdown();
            }

            log.info("Discord RPC shut down");
        } catch (Exception e) {
            log.warn("Error shutting down Discord RPC", e);
        } finally {
            rpc = null;
        }
    }
}