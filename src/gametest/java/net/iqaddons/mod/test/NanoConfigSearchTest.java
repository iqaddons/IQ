package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.screen.model.ConfigEntryModel;
import net.iqaddons.mod.screen.nano.IqNanoConfigScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NanoConfigSearchTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        AtomicBoolean complete = new AtomicBoolean();
        context.setScreen(() -> new IqNanoConfigScreen(null, PhaseOneConfig.class));
        context.waitTicks(2);
        context.runOnClient(client -> {
            try {
                IqNanoConfigScreen screen = (IqNanoConfigScreen) client.gui.screen();
                Field queryField = IqNanoConfigScreen.class.getDeclaredField("query");
                queryField.setAccessible(true);
                Field actionsField = IqNanoConfigScreen.class.getDeclaredField("actions");
                actionsField.setAccessible(true);
                Object searchAction = ((List<?>) actionsField.get(screen)).stream()
                        .filter(action -> invokeString(action, "getId").equals("search"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Search hitbox was not rendered"));
                Object rect = invoke(searchAction, "getRect");
                double x = ((Number) invoke(rect, "getCenterX")).doubleValue();
                double y = ((Number) invoke(rect, "getCenterY")).doubleValue();
                screen.mouseClicked(new MouseButtonEvent(x, y,
                        new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);

                Field enabledField = client.textInputManager().getClass().getDeclaredField("textInputEnabled");
                enabledField.setAccessible(true);
                if (!enabledField.getBoolean(client.textInputManager())) {
                    throw new AssertionError("Search focus did not start SDL text input");
                }

                String search = "Dynamic Active Color";
                search.codePoints().forEach(codepoint -> screen.charTyped(new CharacterEvent(codepoint)));
                if (!search.contentEquals((StringBuilder) queryField.get(screen))) {
                    throw new AssertionError("Typed characters were not recorded by search");
                }
                Method method = IqNanoConfigScreen.class.getDeclaredMethod("visibleGlobalEntries");
                method.setAccessible(true);
                List<?> visible = (List<?>) method.invoke(screen);
                boolean found = false;
                for (Object row : visible) {
                    Method getEntry = row.getClass().getDeclaredMethod("getEntry");
                    getEntry.setAccessible(true);
                    if ("Dynamic Active Color".equals(((ConfigEntryModel) getEntry.invoke(row)).getLabel())) found = true;
                }
                if (!found) throw new AssertionError("Search hid a matching child inside its collapsed section");
                complete.set(true);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not inspect Nano config search results", e);
            }
        });
        context.waitFor(client -> complete.get());
        context.setScreen(() -> null);
    }

    private static Object invoke(Object target, String method) {
        try {
            Method accessor = target.getClass().getDeclaredMethod(method);
            accessor.setAccessible(true);
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect Nano search hitbox", e);
        }
    }

    private static String invokeString(Object target, String method) {
        return (String) invoke(target, method);
    }
}
