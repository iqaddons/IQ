package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.hud.HudEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.concurrent.atomic.AtomicBoolean;

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
            completed.set(true);
        });
        context.waitFor(client -> completed.get());
        context.setScreen(() -> null);
    }
}
