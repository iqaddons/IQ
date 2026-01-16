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
import net.iqaddons.mod.features.kuudra.waypoints.building.BuildWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.pearl.PearlWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.pile.PileWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.stun.StunWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.supply.SupplyWaypointsFeature;
import net.iqaddons.mod.features.widgets.BuildProgressWidget;
import net.iqaddons.mod.features.widgets.SupplyTimerWidget;
import net.iqaddons.mod.utils.hud.HudManager;
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
                new PartyJoinSoundFeature(),
                new PearlWaypointFeature(),
                new SupplyWaypointsFeature(),
                new PileWaypointsFeature(),
                new NoPreAlertFeature(scheduler),
                new SecondSupplyAlertFeature(scheduler),
                new CustomSupplyMessageFeature(),
                new ElleHighlightFeature(),
                new FreshAlertFeature(),
                new KuudraDirectionAlertFeature(),
                new KuudraHitboxFeature(),
                new RendDamageAlertFeature(),
                new AlreadyPickingAlertFeature(),
                new BuildWaypointsFeature(),
                new StunWaypointsFeature(),
                new ManaDrainAlertFeature(),
                new KuudraHealthDisplayFeature(),
                new BlockUselessPerksFeature(),
                new HideMobNametagsFeature(),
                new TeamHighlightFeature(),
                new KuudraPhaseAlertFeature(),
                new DangerAlertFeature()
        );

        features.start();
    }

    private void initializeHudWidgets() {
        HudManager hudManager = HudManager.get();
        hudManager.initialize();

        hudManager.register(
                new SupplyTimerWidget(),
                new BuildProgressWidget()
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
