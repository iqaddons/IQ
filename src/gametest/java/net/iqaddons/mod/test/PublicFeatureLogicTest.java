package net.iqaddons.mod.gametest;

import net.iqaddons.mod.features.kuudra.alerts.BackboneAlertFeature;
import net.iqaddons.mod.features.generic.LoadoutsFeature;
import net.iqaddons.mod.features.generic.WardrobeFeature;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/** Deterministic regressions for public behavior retained during distribution sanitization. */
public final class PublicFeatureLogicTest {
    public static void main(String[] args) {
        backboneAlertKeepsPublicBaseAdvance();
        loadoutAndWardrobeNeverCloseContainers();
        System.out.println("PASS: public feature logic regressions");
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
