package net.iqaddons.mod.config.preset;

import lombok.experimental.UtilityClass;
import net.iqaddons.mod.config.loader.PearlWaypointConfigLoader;
import net.iqaddons.mod.model.pearl.PearlWaypoint;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.model.pearl.WaypointArea;
import net.iqaddons.mod.utils.BoundingBox2D;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class BuiltInPearlWaypoints {

    private static final RenderColor CONFIG_COLOR = new RenderColor(0, 0, 0, 0);
    private static final Vec3 HIDDEN_TARGET = new Vec3(0.0, 0.0, 0.0);
    private static final float FLAT_SIZE = 0.224f;
    private static final float DOUBLE_SIZE = 0.2176f;
    private static final double AIM_DISTANCE = 13.0;
    private static final double DOUBLE_PEARL_LANDING_OFFSET_TICKS = 10.0;

    private static final Vec3 PILE_SHOP = new Vec3(-98.0, 79.0, -113.0);
    private static final Vec3 PILE_TRIANGLE = new Vec3(-94.0, 79.0, -106.0);
    private static final Vec3 PILE_EQUALS = new Vec3(-106.0, 79.0, -99.0);
    private static final Vec3 PILE_SLASH = new Vec3(-98.0, 79.0, -99.0);
    private static final Vec3 PILE_X_CANNON = new Vec3(-110.0, 79.0, -106.0);
    private static final Vec3 PILE_X = new Vec3(-106.0, 79.0, -113.0);

    private static final Vec3 DOUBLE_X_CANNON = new Vec3(-129.5, 79.0, -113.5);
    private static final Vec3 DOUBLE_SQUARE = new Vec3(-140.5, 77.0, -87);
    private static final Vec3 DOUBLE_SHOP = new Vec3(-73.5, 79.0, -133);

    private static final Vec3 STAND_X = new Vec3(-135.0, 77.0, -139.0);
    private static final Vec3 STAND_X_CANNON = new Vec3(-131.0, 78.0, -115.0);
    private static final Vec3 STAND_X_CANNON_COAL = new Vec3(-135.0, 78.0, -129.0);
    private static final Vec3 STAND_SQUARE = new Vec3(-141.0, 78.0, -91.0);
    private static final Vec3 STAND_SQUARE_BOTTOM = new Vec3(-142.0, 77.0, -87.0);
    private static final Vec3 STAND_SLASH = new Vec3(-114.0, 77.0, -69.0);
    private static final Vec3 STAND_EQUALS = new Vec3(-66.0, 76.0, -87.0);
    private static final Vec3 STAND_TRIANGLE = new Vec3(-68.0, 77.0, -123.0);
    private static final Vec3 STAND_SHOP = new Vec3(-86.0, 78.0, -129.0);
    private static final Vec3 STAND_SHOP_LONG = new Vec3(-71.0, 79.0, -135.0);

    public static @NotNull List<WaypointArea> getAreas() {
        return PearlWaypointConfigLoader.get().load();
    }

    public static @NotNull List<WaypointArea> getDefaultAreas() {
        return List.of(
                area("x", -150, -153, -124, -133, List.of(
                        flatPearl("X - X", PILE_X, null, null),
                        doublePearl("X - Square", DOUBLE_SQUARE, null, null, STAND_X, "X Block")
                )),
                area("x cannon", -150, -133, -124, -103, List.of(
                        flatPearl("X Cannon - X Cannon", PILE_X_CANNON, null, null, STAND_X_CANNON, "X Cannon Block"),
                        skyPearl("X Cannon - Sky X Cannon", PILE_X_CANNON, null, null),
                        standBlockOnly("X Cannon Coal Block", STAND_X_CANNON_COAL)
                )),
                area("square", -150, -94, -124, -78, List.of(
                        flatPearl("Square - X", PILE_X, 1, null),
                        flatPearl("Square - X Cannon", PILE_X_CANNON, 2, null),
                        flatPearl("Square - Slash", PILE_EQUALS, 5, null),
                        flatPearl("Square - Equals", PILE_SLASH, 4, null),
                        flatPearl("Square - Triangle", PILE_TRIANGLE, 6, null, STAND_SQUARE, "Square Block"),
                        flatPearl("Square - Shop", PILE_SHOP, 7, null),
                        standBlockOnly("Square Bottom Block", STAND_SQUARE_BOTTOM)
                )),
                area("slash", -124, -92, -105, -62, List.of(
                        flatPearl("Slash - Slash", PILE_SLASH, null, null, STAND_SLASH, "Slash Block"),
                        doublePearl("Slash - X Cannon", DOUBLE_X_CANNON, null, 2),
                        doublePearl("Slash - Square", DOUBLE_SQUARE, null, 3)
                )),
                area("equals", -84, -105, -54, -62, List.of(
                        flatPearl("Equals - Equals", PILE_EQUALS, null, null, STAND_EQUALS, "Equals Block"),
                        doublePearl("Equals - Shop", DOUBLE_SHOP, null, null)
                )),
                area("triangle", -78, -126, -54, -105, List.of(
                        flatPearl("Triangle - Triangle", PILE_TRIANGLE, null, null, STAND_TRIANGLE, "Triangle Block"),
                        doublePearl("Triangle - Shop", DOUBLE_SHOP, null, null)
                )),
                area("shop", -96, -153, -54, -126, List.of(
                        flatPearl("Shop - Shop", PILE_SHOP, null, null, STAND_SHOP, "Shop Block"),
                        skyPearl("Shop - Sky Shop", PILE_SHOP, null, null),
                        standBlockOnly("Shop Long Block", STAND_SHOP_LONG)
                ))
        );
    }

    private static @NotNull WaypointArea area(
            @NotNull String name,
            double x1,
            double z1,
            double x2,
            double z2,
            @NotNull List<PearlWaypoint> waypoints
    ) {
        return new WaypointArea(
                name,
                BoundingBox2D.fromCorners(x1, z1, x2, z2),
                waypoints,
                null,
                null
        );
    }

    private static @NotNull PearlWaypoint flatPearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre
    ) {
        return flatPearl(label, target, preSupply, hideForPre, null);
    }

    private static @NotNull PearlWaypoint flatPearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre,
            Vec3 standBlock
    ) {
        return waypoint(label, target, PearlTrajectoryType.FLAT, 0.0, preSupply, hideForPre, FLAT_SIZE, true, standBlock);
    }

    private static @NotNull PearlWaypoint flatPearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre,
            Vec3 standBlock,
            @NotNull String standBlockLabel
    ) {
        return waypoint(label, target, PearlTrajectoryType.FLAT, 0.0, preSupply, hideForPre, FLAT_SIZE, true, standBlock, standBlockLabel);
    }

    private static @NotNull PearlWaypoint skyPearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre
    ) {
        return waypoint(label, target, PearlTrajectoryType.SKY, 0.0, preSupply, hideForPre, FLAT_SIZE, true, null);
    }

    private static @NotNull PearlWaypoint standBlockOnly(@NotNull String label, @NotNull Vec3 standBlock) {
        return waypoint("", HIDDEN_TARGET, PearlTrajectoryType.FLAT, 0.0, null, null, FLAT_SIZE, false, standBlock);
    }

    private static @NotNull PearlWaypoint doublePearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre
    ) {
        return doublePearl(label, target, preSupply, hideForPre, null);
    }

    private static @NotNull PearlWaypoint doublePearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre,
            Vec3 standBlock
    ) {
        return waypoint(label, target, PearlTrajectoryType.DOUBLE_HIGH, DOUBLE_PEARL_LANDING_OFFSET_TICKS, preSupply, hideForPre, DOUBLE_SIZE, true, standBlock);
    }

    private static @NotNull PearlWaypoint doublePearl(
            @NotNull String label,
            @NotNull Vec3 target,
            Integer preSupply,
            Integer hideForPre,
            Vec3 standBlock,
            @NotNull String standBlockLabel
    ) {
        return waypoint(label, target, PearlTrajectoryType.DOUBLE_HIGH, DOUBLE_PEARL_LANDING_OFFSET_TICKS, preSupply, hideForPre, DOUBLE_SIZE, true, standBlock, standBlockLabel);
    }

    private static @NotNull PearlWaypoint waypoint(
            @NotNull String label,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType trajectoryType,
            double landingOffsetTicks,
            Integer preSupply,
            Integer hideForPre,
            float size,
            boolean alert,
            Vec3 standBlock
    ) {
        return new PearlWaypoint(
                target,
                CONFIG_COLOR,
                standBlock,
                target,
                trajectoryType,
                AIM_DISTANCE,
                null,
                landingOffsetTicks,
                preSupply,
                hideForPre,
                size,
                label,
                alert
        );
    }

    private static @NotNull PearlWaypoint waypoint(
            @NotNull String label,
            @NotNull Vec3 target,
            @NotNull PearlTrajectoryType trajectoryType,
            double landingOffsetTicks,
            Integer preSupply,
            Integer hideForPre,
            float size,
            boolean alert,
            Vec3 standBlock,
            @NotNull String standBlockLabel
    ) {
        return waypoint(label, target, trajectoryType, landingOffsetTicks, preSupply, hideForPre, size, alert, standBlock);
    }
}
