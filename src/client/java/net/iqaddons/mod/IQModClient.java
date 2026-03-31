package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.iqaddons.mod.commands.IQCommand;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.dispatcher.KuudraEventsDispatcher;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.integration.DiscordRPCIntegration;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.iqaddons.mod.lifecycle.modules.FeatureModule;
import net.iqaddons.mod.lifecycle.modules.KuudraModule;
import net.iqaddons.mod.lifecycle.modules.WidgetModule;
import net.iqaddons.mod.utils.update.ModrinthUpdateChecker;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqmod";
    private static IQModClient instance;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    private Configurator configurator;
    private @Nullable FeatureManager featureManager;

    private final List<LifecycleComponent> components = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        instance = this;

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);

        FeatureModule featureModule = new FeatureModule();
        initializeModules(
                new KuudraModule(), new KuudraEventsDispatcher(),
                featureModule, new WidgetModule()
        );
        this.featureManager = featureModule.getFeatures();

        IQKeyBindings.register();
        registerCommands();
        ModrinthUpdateChecker.INSTANCE.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            components.forEach(LifecycleComponent::stop);

            DiscordRPCIntegration.INSTANCE.shutdown();
            ModrinthUpdateChecker.INSTANCE.shutdown();
        });

        log.info("IQ Mod has been initialized!");
    }

    private void initializeModules(LifecycleComponent @NotNull ... components) {
        for (LifecycleComponent component : components) {
            this.components.add(component);
            component.start();
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                IQCommand.register(dispatcher)
        );
    }

    public static IQModClient get() {
        return instance;
    }
}
