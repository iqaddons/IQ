package com.pehenrii.iq;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;

@Slf4j
public class IQMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        log.info("Initializing IQ Mod...");

        log.info("IQ Mod has been initialized!");
    }

    private void registerTicks() {
    }
}
