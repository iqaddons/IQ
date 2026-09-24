package net.iqaddons.mod.screen;

import net.iqaddons.mod.screen.nano.IqNanoConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class IQConfigScreen extends IqNanoConfigScreen {

    @SuppressWarnings("unused")
    public static IQConfigScreen atCategory(@Nullable Screen parent, String categoryId, Class<?>... configClasses) {
        return new IQConfigScreen(parent, categoryId, configClasses);
    }

    @SuppressWarnings("unused")
    public static IQConfigScreen atEntry(@Nullable Screen parent, String categoryId, String entryLabel, Class<?>... configClasses) {
        return new IQConfigScreen(parent, categoryId, entryLabel, configClasses);
    }

    public IQConfigScreen(@Nullable Screen parent, Class<?>... configClasses) {
        super(parent, configClasses);
    }

    public IQConfigScreen(@Nullable Screen parent, @Nullable String categoryId, Class<?>... configClasses) {
        super(parent, categoryId, configClasses);
    }

    public IQConfigScreen(@Nullable Screen parent, @Nullable String categoryId, @Nullable String entryLabel, Class<?>... configClasses) {
        super(parent, categoryId, entryLabel, configClasses);
    }
}
