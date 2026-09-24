package net.iqaddons.mod.gametest;

import net.iqaddons.mod.features.kuudra.waypoints.SupplyWaypointsFeature;
import net.iqaddons.mod.features.kuudra.alerts.SupplyGiantHitboxAlertFeature;
import net.iqaddons.mod.features.kuudra.alerts.BackboneAlertFeature;
import net.iqaddons.mod.features.generic.LoadoutsFeature;
import net.iqaddons.mod.features.generic.WardrobeFeature;
import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.model.spot.PileLocation;
import net.iqaddons.mod.utils.pearl.PearlTrajectorySolver;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Small, deterministic regressions for the feature logic ported from 26.2. */
public final class PortedFeatureLogicTest {
    public static void main(String[] args) {
        pilePlacementAreaUsesTwoBlockCircle();
        supplyRangeUsesEyeBasedVanillaReach();
        supplyAlertClassifiesEyesBeforeBody();
        flatPearlsLandOnBlockCenters();
        backboneAlertKeepsPublicBaseAdvance();
        loadoutAndWardrobeNeverCloseContainers();
        System.out.println("PASS: ported feature logic regressions");
    }

    private static void loadoutAndWardrobeNeverCloseContainers() {
        assertCannotCloseContainer(LoadoutsFeature.class);
        assertCannotCloseContainer(WardrobeFeature.class);
    }

    private static void assertCannotCloseContainer(Class<?> type) {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getResourceAsStream(resource)) {
            check(stream != null, "Compiled class is missing: " + type.getName());
            String bytecode = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            check(!bytecode.contains("closeContainer"), type.getSimpleName() + " must not close container screens");
        } catch (IOException e) {
            throw new AssertionError("Could not inspect " + type.getName(), e);
        }
    }

    private static void backboneAlertKeepsPublicBaseAdvance() {
        try {
            Field field = BackboneAlertFeature.class.getDeclaredField("DEFAULT_BACKBONE_ADVANCE_TICKS");
            field.setAccessible(true);
            check(field.getInt(null) == 2, "Backbone alert must preserve the public two-tick base advance");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Backbone base-advance constant is missing", e);
        }
    }

    private static void pilePlacementAreaUsesTwoBlockCircle() {
        PileLocation pile = new PileLocation("test", new Vec3(10, 79, 20), 0);
        Vec3 center = new Vec3(10.5, 79.1, 20.5);
        check(invokeBoolean(pile, "isInsidePlaceArea", new Class<?>[]{Vec3.class}, center.add(2.0, 0, 0)),
                "Circle boundary must be inside");
        check(!invokeBoolean(pile, "isInsidePlaceArea", new Class<?>[]{Vec3.class}, center.add(2.01, 0, 0)),
                "Point outside the place circle must be rejected");
    }

    private static void supplyRangeUsesEyeBasedVanillaReach() {
        try {
            Method method = SupplyWaypointsFeature.class.getDeclaredMethod(
                    "getInteractionRangeSection", AABB.class, Vec3.class, double.class);
            method.setAccessible(true);
            AABB box = new AABB(3.5, 0, -0.5, 4.5, 10, 0.5);
            AABB section = (AABB) method.invoke(null, box, new Vec3(0, 5, 0), 4.0);
            double verticalReach = Math.sqrt(4.0 * 4.0 - 3.5 * 3.5);
            check(close(section.minY, 5.0 - verticalReach) && close(section.maxY, 5.0 + verticalReach),
                    "Supply range must be clipped from eye distance using vanilla reach");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Supply interaction range helper is missing", e);
        }
    }

    private static void flatPearlsLandOnBlockCenters() {
        Vec3 target = new Vec3(-98.5, 79, -112.5);
        for (double range : new double[]{8, 20, 35, 50}) {
            for (int bearing = 0; bearing < 360; bearing += 45) {
                double angle = Math.toRadians(bearing);
                Vec3 origin = target.add(Math.sin(angle) * range + 0.02, 1.5, Math.cos(angle) * range);
                PearlAimSolution solution = PearlTrajectorySolver.solve(origin, target, PearlTrajectoryType.FLAT);
                check(solution != null, "Reachable flat pearl solution is missing");
                assertCenteredLanding(origin, target, solution);
            }
        }
    }

    private static void supplyAlertClassifiesEyesBeforeBody() {
        try {
            Method method = SupplyGiantHitboxAlertFeature.class.getDeclaredMethod(
                    "classifyAlertLevel", AABB.class, Vec3.class, AABB.class);
            method.setAccessible(true);
            AABB giant = new AABB(0, 0, 0, 2, 8, 2);
            Object primary = method.invoke(null, giant, new Vec3(1, 4, 1), new AABB(0.5, 0, 0.5, 1.5, 2, 1.5));
            Object secondary = method.invoke(null, giant, new Vec3(4, 4, 4), new AABB(1.5, 0, 1.5, 2.5, 2, 2.5));
            check(primary.toString().equals("PRIMARY"), "Eyes inside the giant must be the primary alert");
            check(secondary.toString().equals("SECONDARY"), "Body-only overlap must be the secondary alert");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Supply alert classifier is missing", e);
        }
    }

    private static void assertCenteredLanding(Vec3 origin, Vec3 target, PearlAimSolution solution) {
        Vec3 direction = solution.direction();
        double horizontal = Math.hypot(direction.x, direction.z);
        Vec3 position = origin.add(-direction.z / horizontal * 0.1600000023841858, 0,
                direction.x / horizontal * 0.1600000023841858);
        Vec3 velocity = direction.scale(1.5);
        for (int tick = 1; tick <= 96; tick++) {
            Vec3 next = position.add(velocity);
            if (position.y >= target.y && next.y <= target.y && velocity.y < 0) {
                double fraction = (target.y - position.y) / velocity.y;
                Vec3 hit = position.add(velocity.scale(fraction));
                check(Math.hypot(hit.x - target.x, hit.z - target.z) <= 0.0001,
                        "Flat pearl must land at the target center");
                return;
            }
            position = next;
            velocity = velocity.scale(0.9900000095367432).add(0, -0.029999999329447746, 0);
        }
        throw new AssertionError("Flat pearl never crossed the landing surface");
    }

    private static boolean invokeStaticBoolean(Class<?> type, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (boolean) method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name + " is missing", e);
        }
    }

    private static boolean invokeBoolean(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (boolean) method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name + " is missing", e);
        }
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) <= 1.0E-9;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
