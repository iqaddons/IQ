package net.iqaddons.mod.gametest;

import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.kuudra.miscellaneous.KuudraHitboxFeature;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

/** Regression coverage for Kuudra's hitbox respecting world depth. */
public final class KuudraHitboxOcclusionTest {
    public static void main(String[] args) {
        String owner = WorldRenderEvent.class.getName().replace('.', '/');
        boolean[] foundCall = {false};
        boolean[] throughWalls = {true};

        String resource = "/" + KuudraHitboxFeature.class.getName().replace('.', '/') + ".class";
        try (InputStream stream = KuudraHitboxFeature.class.getResourceAsStream(resource)) {
            if (stream == null) throw new AssertionError("Compiled Kuudra hitbox feature is missing");
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!name.equals("onRender")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        private Boolean latestBooleanConstant;

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.ICONST_0) latestBooleanConstant = false;
                            if (opcode == Opcodes.ICONST_1) latestBooleanConstant = true;
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String invokedOwner, String invokedName,
                                                    String invokedDescriptor, boolean isInterface) {
                            if (invokedOwner.equals(owner) && invokedName.equals("drawStyledHitbox")) {
                                if (latestBooleanConstant == null) {
                                    throw new AssertionError("Kuudra hitbox wall-occlusion argument is not explicit");
                                }
                                foundCall[0] = true;
                                throughWalls[0] = latestBooleanConstant;
                            }
                        }
                    };
                }
            }, 0);
        } catch (IOException e) {
            throw new AssertionError("Could not inspect Kuudra hitbox rendering", e);
        }

        if (!foundCall[0]) throw new AssertionError("Kuudra hitbox render call is missing");
        if (throughWalls[0]) {
            throw new AssertionError("Kuudra hitbox must be hidden when occluded by walls");
        }
        System.out.println("PASS: Kuudra hitbox respects wall occlusion");
    }
}
