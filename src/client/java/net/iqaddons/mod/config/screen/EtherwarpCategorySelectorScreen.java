package net.iqaddons.mod.config.screen;

import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature;
import net.iqaddons.mod.manager.EtherwarpCategoryToggleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EtherwarpCategorySelectorScreen extends Screen {

    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 4;

    private final @Nullable Screen parent;

    public EtherwarpCategorySelectorScreen(@Nullable Screen parent) {
        super(Component.literal("Etherwarp Helper Categories"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        Map<String, Boolean> all = EtherwarpCategoryToggleManager.get().getAll();
        List<String> names = new ArrayList<>(all.keySet());

        int x = (width - BUTTON_WIDTH) / 2;
        int y = 40;

        for (String name : names) {
            boolean enabled = all.getOrDefault(name, true);
            addRenderableWidget(Button.builder(
                            labelFor(name, enabled),
                            button -> {
                                boolean newValue = !EtherwarpCategoryToggleManager.get().isCategoryEnabled(name);
                                EtherwarpCategoryToggleManager.get().setCategoryEnabled(name, newValue);
                                refreshEtherwarpFeature();
                                rebuildButtons();
                            })
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            y += BUTTON_HEIGHT + ROW_GAP;
            if (y > height - 60) break;
        }

        addRenderableWidget(Button.builder(
                        Component.literal("Done"),
                        button -> onClose())
                .bounds((width - 100) / 2, height - 28, 100, 20)
                .build());
    }

    private void refreshEtherwarpFeature() {
        IQModClient client = IQModClient.get();
        if (client == null || client.getFeatureManager() == null) return;

        EtherwarpHelperFeature feature = client.getFeatureManager().get(EtherwarpHelperFeature.class);
        if (feature != null) {
            feature.reloadConfig();
        }
    }

    private Component labelFor(@NotNull String categoryName, boolean enabled) {
        String state = enabled ? "§aON" : "§cOFF";
        return Component.literal(categoryName + " §8- " + state);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        // Avoid calling the blur background twice in the same frame.
        context.fill(0, 0, width, height, 0xA0101010);
        super.extractRenderState(context, mouseX, mouseY, a);

        context.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        context.centeredText(
                font,
                Component.literal("Toggle categories loaded from etherwarp_config.json"),
                width / 2,
                24,
                0xFFAAAAAA
        );
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


