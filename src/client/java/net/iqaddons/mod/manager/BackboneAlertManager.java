package net.iqaddons.mod.manager;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
public final class BackboneAlertManager {

    private static final int BAR_SIZE = 20;

    private static final BackboneAlertManager INSTANCE = new BackboneAlertManager();

    private int ticksRemaining = 0;
    private int startingTicks = 0;
    private int rendTicksRemaining = 0;
    private int cooldownTicks = 0;

    public static BackboneAlertManager get() {
        return INSTANCE;
    }

    public void startBackboneTimer(int ticks) {
        startingTicks = ticks;
        ticksRemaining = ticks;
        rendTicksRemaining = 0;
    }

    public boolean isRendActive() {
        return rendTicksRemaining > 0;
    }

    public boolean shouldDisplay() {
        return ticksRemaining > 0 || isRendActive();
    }

    @Contract(" -> new")
    public @NotNull BackboneAlertManager.BoneResult tick() {
        boolean shouldTriggerRend = false;

        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                rendTicksRemaining = 20;
                shouldTriggerRend = true;
            }
        }

        if (cooldownTicks > 0) cooldownTicks--;
        if (rendTicksRemaining > 0) rendTicksRemaining--;

        return new BoneResult(shouldTriggerRend);
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public @NotNull String getProgressBar() {
        if (startingTicks <= 0) return "";

        int elapsedTicks = startingTicks - ticksRemaining;
        float percent = Math.max(0f, Math.min(1f, elapsedTicks / (float) startingTicks));

        int filledBars = Math.round(percent * BAR_SIZE);
        int emptyBars = BAR_SIZE - filledBars;

        String color = percent > 0.85f ? "§a" : percent > 0.6f ? "§6" : "§c";
        String filled = (color + "|").repeat(Math.max(0, filledBars));
        String empty = "§f|".repeat(Math.max(0, emptyBars));

        return "§8[" + filled + empty + "§8]";
    }

    public @NotNull String getPercentString() {
        if (startingTicks <= 0) return "§b0%";

        int elapsedTicks = startingTicks - ticksRemaining;
        float percent = Math.max(0f, Math.min(1f, elapsedTicks / (float) startingTicks));
        return "§b" + Math.round(percent * 100f) + "%";
    }

    public void reset() {
        ticksRemaining = 0;
        startingTicks = 0;
        rendTicksRemaining = 0;
        cooldownTicks = 0;
    }

    public record BoneResult(boolean triggerRendNow) {}
}
