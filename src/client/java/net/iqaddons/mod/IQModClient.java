package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.client.ConfigScreen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.tracking.SkyBlockTracker;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqmod";
    private static IQModClient instance;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    private Configurator configurator;
    private SkyBlockTracker skyBlockTracker;

    @Override
    public void onInitializeClient() {
        instance = this;

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);

        initializeTrackers();

        IQKeyBindings.register();
        registerCommand();

        log.info("IQ Mod has been initialized!");
    }

    private void initializeTrackers() {
        skyBlockTracker = new SkyBlockTracker();
        skyBlockTracker.start();
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                dispatcher.register(literal("iq").executes(source -> {
                    IQKeyBindings.openConfigScreen(mc);
                    return 1;
                }))
        );
    }

    public static IQModClient get() {
        return instance;
    }

}
