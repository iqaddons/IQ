package net.iqaddons.mod.nanovg;

/** Counts whole Minecraft frames during which NanoVG rendering is suppressed. */
public final class NanoVgFrameDelay {
    private int remaining;

    public synchronized void reset(int frames) {
        remaining = Math.max(0, frames);
    }

    public synchronized boolean consumeFrame() {
        if (remaining <= 0) return false;
        remaining--;
        return true;
    }
}
