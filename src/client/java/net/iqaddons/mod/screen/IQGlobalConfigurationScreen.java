package net.iqaddons.mod.screen;

import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class IQGlobalConfigurationScreen extends IqNanoGlobalConfigScreen {

    public IQGlobalConfigurationScreen(@Nullable Screen parent, Class<?>... configClasses) {
        super(parent, configClasses);
    }

    public static int getSharedThemeIndex() {
        return IqNanoGlobalConfigScreen.Companion.getSharedThemeIndex();
    }

    public static double getSharedGuiOpacity() {
        return IqNanoGlobalConfigScreen.Companion.getSharedGuiOpacity();
    }

    public static double getSharedUiScale() {
        return IqNanoGlobalConfigScreen.Companion.getSharedUiScale();
    }

    public static boolean isSharedAnimationsEnabled() {
        return IqNanoGlobalConfigScreen.Companion.isSharedAnimationsEnabled();
    }

    public static double getSharedAnimationSpeed() {
        return IqNanoGlobalConfigScreen.Companion.getSharedAnimationSpeed();
    }

    public static boolean isSharedOutlineShadowEnabled() {
        return IqNanoGlobalConfigScreen.Companion.isSharedOutlineShadowEnabled();
    }

    public static boolean isSharedUiStatePersistenceEnabled() {
        return IqNanoGlobalConfigScreen.Companion.isSharedUiStatePersistenceEnabled();
    }

}
