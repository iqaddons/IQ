package net.iqaddons.mod.lifecycle.modules;

import lombok.Getter;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.iqaddons.mod.manager.KuudraStateManager;

@Getter
public class KuudraModule implements LifecycleComponent {

    private KuudraStateManager kuudraStateManager;

    @Override
    public void start() {
        kuudraStateManager = new KuudraStateManager();
        kuudraStateManager.start();
    }

    @Override
    public void stop() {}
}
