package net.iqaddons.mod.features;

public interface Feature {
    String getId();
    String getName();

    boolean isEnabled();
    void setEnabled(boolean enabled);

    void onEnable();
    void onDisable();

}
