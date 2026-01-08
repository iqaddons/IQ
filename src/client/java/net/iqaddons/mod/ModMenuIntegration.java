package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.iqaddons.mod.config.Configuration;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> ResourcefulConfigScreen.make(IQModClient.get().getConfigurator(), Configuration.class)
                .withParent(screen)
                .build();
    }
}
