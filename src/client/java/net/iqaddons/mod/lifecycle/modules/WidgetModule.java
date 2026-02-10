package net.iqaddons.mod.lifecycle.modules;

import lombok.Getter;
import net.iqaddons.mod.features.widgets.*;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.lifecycle.LifecycleComponent;

public class WidgetModule implements LifecycleComponent {

    private HudManager hudManager;

    @Override
    public void start() {
        hudManager = new HudManager();
        hudManager.initialize();

        hudManager.register(
                new SupplyTimerWidget(), new BuildProgressWidget(),
                new CustomSplitsWidget(), new FreshCountdownWidget(),
                new KuudraHealthWidget(), new FreshersTimerWidget(),
                new SupplyProgressWidget()
        );
    }

    @Override
    public void stop() {
        hudManager.stop();
    }
}
