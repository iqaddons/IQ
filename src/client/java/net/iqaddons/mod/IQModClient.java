package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.iqaddons.mod.commands.IQCommand;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.features.generic.PartyJoinSoundFeature;
import net.iqaddons.mod.features.generic.WaypointFeature;
import net.iqaddons.mod.features.kuudra.*;
import net.iqaddons.mod.features.kuudra.alerts.*;
import net.iqaddons.mod.features.kuudra.waypoints.*;
import net.iqaddons.mod.features.widgets.*;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.iqaddons.mod.lifecycle.modules.FeatureModule;
import net.iqaddons.mod.lifecycle.modules.WidgetModule;
import net.iqaddons.mod.manager.lifecycle.KuudraLifecycleManager;
import net.iqaddons.mod.utils.tracking.KuudraTracker;
import net.iqaddons.mod.utils.tracking.SkyBlockTracker;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqmod";
    private static IQModClient instance;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "IQ-Mod-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private Configurator configurator;

    private final List<LifecycleComponent> components = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        instance = this;

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);

        initializeModules(
                new FeatureModule(scheduler), new WidgetModule()
        );

        IQKeyBindings.register();
        registerCommands();

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
