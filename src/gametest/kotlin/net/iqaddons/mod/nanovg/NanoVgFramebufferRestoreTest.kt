package net.iqaddons.mod.nanovg

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import org.lwjgl.opengl.GL33.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL33.GL_DRAW_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL33.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL33.GL_READ_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL33.glBindFramebuffer
import org.lwjgl.opengl.GL33.glDeleteFramebuffers
import org.lwjgl.opengl.GL33.glGenFramebuffers
import org.lwjgl.opengl.GL33.glGetInteger

/** Guards the framebuffer state required immediately before GpuSurface.present(). */
class NanoVgFramebufferRestoreTest : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.runOnClient<RuntimeException> {
            val originalRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
            val originalDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
            val transientRead = glGenFramebuffers()
            val presentationFramebuffer = glGenFramebuffers()

            try {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, transientRead)
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, presentationFramebuffer)
                val snapshotClass = Class.forName("net.iqaddons.mod.nanovg.GlStateSnapshot")
                val companion = snapshotClass.getField("Companion").get(null)
                val capture = companion.javaClass.methods.single { it.name.startsWith("capture\$") }
                val restore = snapshotClass.methods.single { it.name.startsWith("restore\$") }
                val snapshot = capture.invoke(companion)

                glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
                restore.invoke(snapshot)

                check(glGetInteger(GL_READ_FRAMEBUFFER_BINDING) == presentationFramebuffer) {
                    "NanoVG must collapse the transient read target into the presentation framebuffer"
                }
                check(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) == presentationFramebuffer) {
                    "NanoVG must restore the presentation framebuffer as the draw target"
                }
            } finally {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originalRead)
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originalDraw)
                glDeleteFramebuffers(transientRead)
                glDeleteFramebuffers(presentationFramebuffer)
            }
        }
    }
}
