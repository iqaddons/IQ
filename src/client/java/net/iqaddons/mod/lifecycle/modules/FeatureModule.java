package net.iqaddons.mod.lifecycle.modules;

import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.features.generic.*;
import net.iqaddons.mod.features.kuudra.alerts.*;
import net.iqaddons.mod.features.kuudra.miscellaneous.*;
import net.iqaddons.mod.features.kuudra.tracker.ChestCounterTrackerFeature;
import net.iqaddons.mod.features.kuudra.tracker.KuudraProfitTrackerFeature;
import net.iqaddons.mod.features.kuudra.tracker.PersonalBestTrackerFeature;
import net.iqaddons.mod.features.kuudra.waypoints.*;
import net.iqaddons.mod.lifecycle.LifecycleComponent;

import java.util.concurrent.ScheduledExecutorService;

@RequiredArgsConstructor
public class FeatureModule implements LifecycleComponent {

    private final ScheduledExecutorService scheduler;

    private FeatureManager features;

    @Override
    public void start() {
        features = new FeatureManager();
        features.register(
                new PartyJoinSoundFeature(), new WaypointFeature(), new WardrobeFeature(),
                new PartyCommandsFeature(), new LimboAlertFeature()
        );

        features.register(
                new PearlWaypointFeature(), new SupplyWaypointsFeature(), new PileWaypointsFeature(),
                new NoPreAlertFeature(), new SecondSupplyAlertFeature(scheduler), new CustomSupplyMessageFeature(),
                new ElleHighlightFeature(), new FreshAlertFeature(), new KuudraHitboxFeature(),
                new RendDamageAlertFeature(), new BuildWaypointsFeature(), new StunWaypointsFeature(),
                new ManaDrainAlertFeature(), new BlockUselessPerksFeature(), new HideMobNametagsFeature(),
                new TeamHighlightFeature(), new KuudraPhaseAlertFeature(), new DangerAlertFeature(),
                new KuudraHealthFeature(), new HideDamageTitleFeature(), new SupplyDroppedAlertFeature(),
                new PersonalBestTrackerFeature(), new AutoRequeueFeature(), new ChestCounterTrackerFeature(),
                new KuudraProfitTrackerFeature(), new CroesusHelperFeature(), new HideKuudraBossBarFeature(),
                new KuudraNotificationsFeature(), new BackboneAlertFeature(), new SupplyGiantHitboxAlertFeature(),
                new HideUselessArmorStandsFeature(), new AbilityAnnounceFeature(), new DiscordRPCFeature()
        );

        features.start();
    }

    @Override
    public void stop() {
        features.stop();
    }
}
