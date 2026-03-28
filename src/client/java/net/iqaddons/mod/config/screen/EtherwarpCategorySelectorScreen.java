package net.iqaddons.mod.config.screen;

import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature;
import net.iqaddons.mod.manager.EtherwarpCategoryToggleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
        super(Text.literal("Etherwarp Helper Categories"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();

        Map<String, Boolean> all = EtherwarpCategoryToggleManager.get().getAll();
        List<String> names = new ArrayList<>(all.keySet());

        int x = (width - BUTTON_WIDTH) / 2;
        int y = 40;

        for (String name : names) {
            boolean enabled = all.getOrDefault(name, true);
            addDrawableChild(ButtonWidget.builder(
                            labelFor(name, enabled),
                            button -> {
                                boolean newValue = !EtherwarpCategoryToggleManager.get().isCategoryEnabled(name);
                                EtherwarpCategoryToggleManager.get().setCategoryEnabled(name, newValue);
                                refreshEtherwarpFeature();
                                rebuildButtons();
                            })
                    .dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            y += BUTTON_HEIGHT + ROW_GAP;
            if (y > height - 60) break;
        }

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> close())
                .dimensions((width - 100) / 2, height - 28, 100, 20)
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

    private Text labelFor(@NotNull String categoryName, boolean enabled) {
        String state = enabled ? "§aON" : "§cOFF";
        return Text.literal(categoryName + " §8- " + state);
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        // Avoid calling the blur background twice in the same frame.
        context.fill(0, 0, width, height, 0xA0101010);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Toggle categories loaded from etherwarp_config.json"),
                width / 2,
                24,
                0xFFAAAAAA
        );
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}


