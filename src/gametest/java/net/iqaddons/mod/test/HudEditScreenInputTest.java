package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.hud.HudEditScreen;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Guards the SDL mouse-button contract used by the 26.3 HUD editor. */
public final class HudEditScreenInputTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        AtomicBoolean completed = new AtomicBoolean();
        context.setScreen(HudEditScreen::new);
        context.waitTicks(2);
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof HudEditScreen screen)) {
                throw new AssertionError("HUD editor did not open");
            }

            double buttonX = screen.width / 2.0;
            double buttonY = screen.height - 24.0 - 16.0;
            var click = new MouseButtonEvent(
                    buttonX,
                    buttonY,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)
            );

            if (!screen.mouseClicked(click, false)) {
                throw new AssertionError("HUD editor rejected the SDL left-button event");
            }
            if (screen.getPendingHudStyleSwapFrames() <= 0) {
                throw new AssertionError("HUD style toggle was not scheduled by the SDL left-button event");
            }

            AtomicInteger widgetClicks = new AtomicInteger();
            TestWidget widget = new TestWidget(
                    HudLine.of("Clickable").onClick(widgetClicks::incrementAndGet)
            );
            if (!widget.onClick(1.0, 1.0, InputConstants.MOUSE_BUTTON_LEFT)) {
                throw new AssertionError("Vanilla HUD rejected the SDL left-button event");
            }
            if (widgetClicks.get() != 1) {
                throw new AssertionError("Vanilla HUD did not run the line click action exactly once");
            }
            completed.set(true);
        });
        context.waitFor(client -> completed.get());
        context.setScreen(() -> null);
    }

    private static final class TestWidget extends HudWidget {
        private TestWidget(HudLine line) {
            super("vanillaClickTest", "Vanilla click test", 0.0f, 0.0f, 1.0f, HudAnchor.TOP_LEFT);
            addLine(line);
        }
    }
}
