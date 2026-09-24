package net.iqaddons.mod.gametest;

import net.iqaddons.mod.nanovg.IqNanoVg;
import net.iqaddons.mod.nanovg.IqNanoVgMode;

import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic checks for NanoVG lifecycle gating and transition resets. */
public final class NanoVgDiagnosticModeTest {
    public static void main(String[] args) {
        parsesEverySupportedModeCaseInsensitively();
        invalidAndMissingValuesFallBackToFull();
        screenAndHudDecisionsMatchTheModeMatrix();
        offBlocksAllNanoVgLifecycleAndDrawingDecisions();
        clientLevelChangesDoNotDelayNanoVgRendering();
        transitionDelayConsumesExactlyTenFrames();
        System.out.println("PASS: NanoVG lifecycle regressions");
    }

    private static void clientLevelChangesDoNotDelayNanoVgRendering() {
        IqNanoVg.resetAfterRenderTransition("client level change");
        check(!IqNanoVg.shouldSkipRenderFrame(),
                "client level changes must not reset NanoVG or suppress HUD frames");
    }

    private static void transitionDelayConsumesExactlyTenFrames() {
        IqNanoVg.resetAfterRenderTransition("client resource reload");
        for (int frame = 0; frame < 10; frame++) {
            check(IqNanoVg.shouldSkipRenderFrame(), "resource reload must block frame " + frame);
        }
        check(!IqNanoVg.shouldSkipRenderFrame(), "resource reload delay must stop after exactly ten frames");
    }

    private static void parsesEverySupportedModeCaseInsensitively() {
        check(IqNanoVgMode.parse("off") == IqNanoVgMode.OFF, "off must parse");
        check(IqNanoVgMode.parse("SCREEN") == IqNanoVgMode.SCREEN, "SCREEN must parse case-insensitively");
        check(IqNanoVgMode.parse(" hud ") == IqNanoVgMode.HUD, "hud must parse with surrounding whitespace");
        check(IqNanoVgMode.parse("full") == IqNanoVgMode.FULL, "full must parse");
        check(IqNanoVgMode.parse("trace") == IqNanoVgMode.TRACE, "trace must parse");
    }

    private static void invalidAndMissingValuesFallBackToFull() {
        check(IqNanoVgMode.parse(null) == IqNanoVgMode.FULL, "missing mode must default to full");
        check(IqNanoVgMode.parse("") == IqNanoVgMode.FULL, "blank mode must default to full");
        check(IqNanoVgMode.parse("driver-workaround") == IqNanoVgMode.FULL,
                "invalid mode must fall back to full");
        check(!IqNanoVgMode.isValid("driver-workaround"), "invalid mode must be identifiable for warning logs");
    }

    private static void screenAndHudDecisionsMatchTheModeMatrix() {
        check(IqNanoVgMode.SCREEN.rendersScreen() && !IqNanoVgMode.SCREEN.rendersHud(),
                "screen mode must render only NanoVG screens");
        check(!IqNanoVgMode.HUD.rendersScreen() && IqNanoVgMode.HUD.rendersHud(),
                "hud mode must render only the NanoVG HUD");
        check(IqNanoVgMode.FULL.rendersScreen() && IqNanoVgMode.FULL.rendersHud(),
                "full mode must render screens and HUD");
        check(IqNanoVgMode.TRACE.rendersScreen() && IqNanoVgMode.TRACE.rendersHud(),
                "trace mode must preserve full rendering behavior");
    }

    private static void offBlocksAllNanoVgLifecycleAndDrawingDecisions() {
        check(!IqNanoVgMode.OFF.preparesResources(), "off must block NanoVG preparation");
        check(!IqNanoVgMode.OFF.resetsResources(), "off must block NanoVG reset");
        check(!IqNanoVgMode.OFF.rendersScreen(), "off must block NanoVG screen drawing");
        check(!IqNanoVgMode.OFF.rendersHud(), "off must block NanoVG HUD drawing");

        AtomicInteger calls = new AtomicInteger();
        IqNanoVgMode.OFF.runPreparation(calls::incrementAndGet);
        IqNanoVgMode.OFF.runReset(calls::incrementAndGet);
        IqNanoVgMode.OFF.runScreen(calls::incrementAndGet);
        IqNanoVgMode.OFF.runHud(calls::incrementAndGet);
        check(calls.get() == 0, "off must execute no NanoVG lifecycle or drawing callback");

        IqNanoVgMode.FULL.runPreparation(calls::incrementAndGet);
        IqNanoVgMode.FULL.runReset(calls::incrementAndGet);
        IqNanoVgMode.FULL.runScreen(calls::incrementAndGet);
        IqNanoVgMode.FULL.runHud(calls::incrementAndGet);
        check(calls.get() == 4, "full must execute every NanoVG lifecycle or drawing callback");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
