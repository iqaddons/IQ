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
import net.iqaddons.mod.features.kuudra.*;
import net.iqaddons.mod.features.kuudra.alerts.*;
import net.iqaddons.mod.features.kuudra.waypoints.BuildWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.PileWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.StunWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.SupplyWaypointsFeature;
import net.iqaddons.mod.features.widgets.*;
import net.iqaddons.mod.manager.lifecycle.KuudraLifecycleManager;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.utils.tracking.KuudraTracker;
import net.iqaddons.mod.utils.tracking.SkyBlockTracker;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqmod";
    private static IQModClient instance;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    private Configurator configurator;
    private SkyBlockTracker skyBlockTracker;
    private KuudraTracker kuudraTracker;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "IQ-Mod-Scheduler");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void onInitializeClient() {
        instance = this;

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);

        initializeTrackers();
        initializeFeatures();
        initializeHudWidgets();

        IQKeyBindings.register();
        registerCommands();

        KuudraLifecycleManager.get().start();

        log.info("IQ Mod has been initialized!");
    }

    private void initializeTrackers() {
        skyBlockTracker = new SkyBlockTracker();
        skyBlockTracker.start();

        kuudraTracker = new KuudraTracker(skyBlockTracker);
        kuudraTracker.start();
    }

    private void initializeFeatures() {
        FeatureManager features = FeatureManager.get();
        features.register(
                new PartyJoinSoundFeature(), new PearlWaypointFeature(), new SupplyWaypointsFeature(),
                new PileWaypointsFeature(), new NoPreAlertFeature(), new SecondSupplyAlertFeature(scheduler),
                new CustomSupplyMessageFeature(), new ElleHighlightFeature(), new FreshAlertFeature(),
                new KuudraDirectionAlertFeature(), new KuudraHitboxFeature(), new RendDamageAlertFeature(),
                new AlreadyPickingAlertFeature(), new BuildWaypointsFeature(), new StunWaypointsFeature(),
                new ManaDrainAlertFeature(), new BlockUselessPerksFeature(), new HideMobNametagsFeature(),
                new TeamHighlightFeature(), new KuudraPhaseAlertFeature(), new DangerAlertFeature()
        );

        features.start();
    }

    private void initializeHudWidgets() {
        HudManager hudManager = HudManager.get();
        hudManager.initialize();

        hudManager.register(
                new SupplyTimerWidget(), new BuildProgressWidget(),
                new CustomSplitsWidget(), new FreshCountdownWidget(),
                new KuudraHealthWidget()
        );
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
