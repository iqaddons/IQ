package net.iqaddons.mod;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ModInitializer;

@Slf4j
public class IQMod implements ModInitializer {

    @Override
    public void onInitialize() {
        log.info("IQ Mod has been initialized!");
    }
}
