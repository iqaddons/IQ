package net.iqaddons.mod.lifecycle.modules;

import lombok.Getter;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.features.generic.*;
import net.iqaddons.mod.features.kuudra.alerts.*;
import net.iqaddons.mod.features.kuudra.miscellaneous.*;
import net.iqaddons.mod.features.kuudra.tracker.ChestCounterTrackerFeature;
import net.iqaddons.mod.features.kuudra.tracker.KuudraProfitTrackerFeature;
import net.iqaddons.mod.features.kuudra.tracker.PersonalBestTrackerFeature;
import net.iqaddons.mod.features.kuudra.tracker.PhaseSplitsPBTrackerFeature;
import net.iqaddons.mod.features.kuudra.waypoints.*;
import net.iqaddons.mod.lifecycle.LifecycleComponent;

@Getter
public class FeatureModule implements LifecycleComponent {

    private FeatureManager features;

    @Override
    public void start() {
        features = new FeatureManager();
        features.register(
                new PartyJoinSoundFeature(), new WaypointFeature(), new LoadoutsFeature(), new WardrobeFeature(),
                new PreventClosingLoadoutMenuFeature(), new PartyCommandsFeature(), new LimboAlertFeature(), new ArrowTrackerFeature()
        );

        features.register(
                new SupplyWaypointsFeature(), new PileWaypointsFeature(), new PearlWaypointFeature(),
                new NoPreAlertFeature(), new CratePriorityFeature(), new SecondSupplyAlertFeature(), new SupplyPlacementEfficiencyFeature(), new CustomSupplyMessageFeature(),
                new ElleHighlightFeature(), new FreshAlertFeature(), new KuudraHitboxFeature(),
                new RendDamageAlertFeature(), new IceSprayAlertFeature(), new BuildWaypointsFeature(), new StunWaypointsFeature(),
                new ManaDrainAlertFeature(), new BlockUselessPerksFeature(), new BlockPickobulusFeature(), new HideMobNametagsFeature(),
                new TeamHighlightFeature(), new FreshHighlightFeature(), new KuudraPhaseAlertFeature(), new DangerAlertFeature(),
                new KuudraHealthFeature(), new HideDamageTitleFeature(), new SupplyDroppedAlertFeature(),
                new PersonalBestTrackerFeature(), new PhaseSplitsPBTrackerFeature(), new AutoRequeueFeature(), new ChestCounterTrackerFeature(),
                new KuudraProfitTrackerFeature(), new CroesusHelperFeature(), new HideKuudraBossBarFeature(),
                new KuudraNotificationsFeature(), new BackboneAlertFeature(), new SupplyGiantHitboxAlertFeature(),
                new HideUselessArmorStandsFeature(), new AbilityAnnounceFeature(), new DiscordRPCFeature(),
                new IchorPoolWaypointFeature(), new EtherwarpWaypointsFeature(), new DpsWaypointFeature(), new SkipWaypointFeature(), new KuudraDistanceFeature(),
                new FireVeilOverlayFeature()
        );

        features.start();
    }

    @Override
    public void stop() {
        features.stop();
    }

}
