package net.iqaddons.mod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class KuudraStunTracker {

    public static final Vec3 ENTER_POS = new Vec3(-161, 49, -186);
    private static final double STOMACH_Y_THRESHOLD = 55.0;
    private static final double STOMACH_HORIZONTAL_RADIUS = 40.0;

    private final Minecraft mc = Minecraft.getInstance();

    private volatile boolean stunPhase = false;
    private volatile boolean eaten = false;
    private volatile boolean stunnedThisRun = false;

    public void onChat(@NotNull String message) {
        if (message.contains("You purchased Human Cannonball!")) {
            stunPhase = true;
            return;
        }

        if (message.contains("destroyed one of Kuudra's pods!")) {
            stunPhase = false;
            eaten = false;
        }
    }

    public boolean onTick() {
        if (mc.player == null) return false;
        if (!stunPhase || !isInStomach()) return false;

        stunPhase = false;
        eaten = true;
        boolean newlyStunned = !stunnedThisRun;
        stunnedThisRun = true;
        return newlyStunned;
    }

    public void resetRun() {
        stunPhase = false;
        eaten = false;
        stunnedThisRun = false;
    }

    public boolean isStunner() {
        return stunPhase || eaten || isInStomach();
    }

    public boolean isInStomach() {
        if (mc.player == null) return false;
        if (mc.player.getY() >= STOMACH_Y_THRESHOLD) return false;

        double dx = mc.player.getX() - ENTER_POS.x();
        double dz = mc.player.getZ() - ENTER_POS.z();
        return dx * dx + dz * dz <= STOMACH_HORIZONTAL_RADIUS * STOMACH_HORIZONTAL_RADIUS;
    }

    public boolean isStunPhase() {
        return stunPhase;
    }

    public boolean isEaten() {
        return eaten;
    }

    public boolean wasStunnedThisRun() {
        return stunnedThisRun;
    }
}
