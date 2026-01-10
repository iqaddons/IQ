package net.iqaddons.mod.events;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
