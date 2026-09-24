package net.iqaddons.mod.nanovg;

import java.util.Locale;

/** Controls which parts of IQ's global NanoVG hook are allowed to run. */
public enum IqNanoVgMode {
    OFF(false, false, false, false),
    SCREEN(true, true, true, false),
    HUD(true, true, false, true),
    FULL(true, true, true, true),
    TRACE(true, true, true, true);

    public static final String PROPERTY = "iq.nanovg.mode";

    private final boolean preparesResources;
    private final boolean resetsResources;
    private final boolean rendersScreen;
    private final boolean rendersHud;

    IqNanoVgMode(boolean preparesResources, boolean resetsResources, boolean rendersScreen, boolean rendersHud) {
        this.preparesResources = preparesResources;
        this.resetsResources = resetsResources;
        this.rendersScreen = rendersScreen;
        this.rendersHud = rendersHud;
    }

    public boolean preparesResources() {
        return preparesResources;
    }

    public boolean resetsResources() {
        return resetsResources;
    }

    public boolean rendersScreen() {
        return rendersScreen;
    }

    public boolean rendersHud() {
        return rendersHud;
    }

    public boolean tracesGl() {
        return this == TRACE;
    }

    public void runPreparation(Runnable action) {
        if (preparesResources) action.run();
    }

    public void runReset(Runnable action) {
        if (resetsResources) action.run();
    }

    public void runScreen(Runnable action) {
        if (rendersScreen) action.run();
    }

    public void runHud(Runnable action) {
        if (rendersHud) action.run();
    }

    public static IqNanoVgMode parse(String raw) {
        if (raw == null || raw.isBlank()) return FULL;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FULL;
        }
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return true;
        try {
            valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
