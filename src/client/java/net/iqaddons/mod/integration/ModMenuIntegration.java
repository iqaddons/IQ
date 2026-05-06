package net.iqaddons.mod.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.*;
import net.iqaddons.mod.screen.IQConfigScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> new IQConfigScreen(screen,
                Configuration.class,
                KuudraGeneralConfig.class,
                PhaseOneConfig.class, PhaseTwoConfig.class,
                PhaseThreeConfig.class, PhaseFourConfig.class
        );
    }
}
