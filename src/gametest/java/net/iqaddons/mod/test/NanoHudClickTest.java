package net.iqaddons.mod.gametest;

import net.iqaddons.mod.hud.component.HudLine;

import java.util.concurrent.atomic.AtomicInteger;

/** Regression coverage for coordinate-based clicks in the modern HUD renderer. */
public final class NanoHudClickTest {
    public static void main(String[] args) {
        AtomicInteger clicks = new AtomicInteger();
        HudLine line = HudLine.of("[Session]").onClick(clicks::incrementAndGet);

        try {
            var updateBounds = HudLine.class.getMethod(
                    "updateNanoHitBounds", float.class, float.class, float.class, float.class
            );
            var clickAt = HudLine.class.getMethod("runNanoClickActionAt", double.class, double.class);

            updateBounds.invoke(line, 40.0f, 60.0f, 30.0f, 10.0f);
            check((boolean) clickAt.invoke(line, 55.0, 65.0),
                    "Nano HUD must accept a click inside the rendered line bounds");
            check(clicks.get() == 1, "Nano HUD click must run the line action exactly once");
            check(!(boolean) clickAt.invoke(line, 75.0, 65.0),
                    "Nano HUD must reject a click outside the rendered line bounds");
            check(clicks.get() == 1, "Outside clicks must not run the line action");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Nano HUD coordinate click contract is missing", e);
        }

        System.out.println("PASS: Nano HUD coordinate click regression");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
