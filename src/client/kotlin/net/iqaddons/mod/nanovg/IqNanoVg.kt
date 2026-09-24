package net.iqaddons.mod.nanovg

import com.mojang.blaze3d.systems.RenderSystem
import net.iqaddons.mod.nanovg.assets.FontManager
import net.iqaddons.mod.nanovg.assets.IconManager
import net.iqaddons.mod.nanovg.assets.TextureManager
import net.iqaddons.mod.nanovg.event.UiEventBus
import net.iqaddons.mod.nanovg.rendering.nanovg.NanoVGBackend
import net.iqaddons.mod.nanovg.rendering.nanovg.NanoVGContext
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.ui.theme.ThemeManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.opengl.GL33.*
import org.slf4j.LoggerFactory

interface IqNanoVgRenderable {
    fun renderNanoVg(renderer: Renderer2D, mouseX: Float, mouseY: Float)
}

object IqNanoVg {
    private const val RECREATE_DELAY_FRAMES = 10
    private const val SCREEN_TRACE_FRAMES = 5

    @JvmField
    val logger = LoggerFactory.getLogger("IQ-NanoVG")

    private class Runtime {
        val events = UiEventBus()
        val context = NanoVGContext(IqNanoVgConfiguration.traceStencilStrokesEnabled())
        val fontManager = FontManager(context)
        val iconManager = IconManager()
        val textureManager = TextureManager(context)
        val themeManager = ThemeManager(events)
        val backend = NanoVGBackend(
            context,
            fontHandle = fontManager::handleFor,
            blurEnabled = IqNanoVgConfiguration.traceBlurEnabled(),
        )
        val renderer = Renderer2D(backend, iconManager, textureManager)
    }

    @Volatile
    private var runtime: Runtime? = null

    private val frameDelay = NanoVgFrameDelay()

    @Volatile
    private var traceFramesRemaining = 0

    private var traceFrameNumber = 0L

    private fun runtime(): Runtime = runtime ?: synchronized(this) {
        runtime ?: Runtime().also { runtime = it }
    }

    val theme get() = runtime().themeManager.active
    val textures get() = runtime().textureManager

    fun prepare() {
        IqNanoVgConfiguration.mode().runPreparation {
            val active = runtime()
            if (!active.context.isReady) {
                traceCheckpoint("before NanoVGContext creation", force = true)
                active.context.start()
                traceCheckpoint("after NanoVGContext creation", finish = true, force = true)
            }
            if (!active.fontManager.isReady) active.fontManager.start()
            active.themeManager.start()
        }
    }

    @JvmStatic
    fun dispose() {
        val active = runtime ?: return
        traceCheckpoint("before NanoVG resource destruction", force = true)
        active.backend.dispose()
        active.textureManager.stop()
        active.context.stop()
        active.fontManager.stop()
        active.themeManager.stop()
        runtime = null
        traceCheckpoint("after NanoVG resource destruction", finish = true, force = true)
    }

    @JvmStatic
    fun resetAfterRenderTransition(reason: String) {
        IqNanoVgConfiguration.mode().runReset {
            if (reason == "client level change") {
                logger.info("Ignored IQ NanoVG reset after {}; level changes preserve the active GL context", reason)
                return@runReset
            }
            frameDelay.reset(RECREATE_DELAY_FRAMES)
            if (IqNanoVgConfiguration.mode().tracesGl()) {
                traceFramesRemaining = RECREATE_DELAY_FRAMES + SCREEN_TRACE_FRAMES
            }
            if (runtime == null) {
                logger.info(
                    "IQ NanoVG reset requested after {}, but no NanoVG runtime/context was created; delaying render for {} frames",
                    reason,
                    RECREATE_DELAY_FRAMES,
                )
                return@runReset
            }
            traceCheckpoint("before reset: $reason", force = true)
            dispose()
            logger.info("Reset IQ NanoVG resources after {}; delaying render for {} frames", reason, RECREATE_DELAY_FRAMES)
            traceCheckpoint("after reset: $reason", finish = true, force = true)
        }
    }

    @JvmStatic
    fun isSupported(): Boolean = RenderSystem.tryGetDevice()?.deviceInfo?.backendName() == "OpenGL"

    @JvmStatic
    fun onScreenChanged(previous: Screen?, current: Screen?) {
        if (!IqNanoVgConfiguration.mode().tracesGl()) return
        traceFramesRemaining = maxOf(traceFramesRemaining, SCREEN_TRACE_FRAMES)
        logger.info(
            "[IQ NanoVG trace] GUI changed: {} -> {}; detailed checkpoints enabled for {} frames",
            previous?.javaClass?.name ?: "<none>",
            current?.javaClass?.name ?: "<none>",
            SCREEN_TRACE_FRAMES,
        )
    }

    @JvmStatic
    fun renderCurrentScreen(client: Minecraft) {
        if (!isSupported()) return
        IqNanoVgConfiguration.mode().runScreen {
            val screen = client.gui.screen() as? IqNanoVgRenderable ?: return@runScreen
            renderFrame(client, "screen") { renderer, mouseX, mouseY ->
                screen.renderNanoVg(renderer, mouseX, mouseY)
            }
        }
    }

    fun renderOverlay(client: Minecraft, draw: (Renderer2D, Float, Float) -> Unit) {
        if (!isSupported()) return
        IqNanoVgConfiguration.mode().runHud { renderFrame(client, "HUD", draw) }
    }

    @JvmStatic
    fun shouldSkipRenderFrame(): Boolean = frameDelay.consumeFrame()

    @JvmStatic
    fun tracePresent(phase: String) {
        if (!isDetailedTraceActive()) return
        traceCheckpoint(phase, finish = phase.startsWith("after"))
        if (phase == "after GpuSurface.present()") {
            traceFrameNumber++
            traceFramesRemaining = (traceFramesRemaining - 1).coerceAtLeast(0)
        }
    }

    private inline fun renderFrame(
        client: Minecraft,
        drawPhase: String,
        draw: (Renderer2D, Float, Float) -> Unit,
    ) {
        val window = client.window
        val width = window.guiScaledWidth.toFloat()
        val height = window.guiScaledHeight.toFloat()
        val scale = window.guiScale.toFloat()
        val mouseX = (client.mouseHandler.xpos() / window.guiScale).toFloat()
        val mouseY = (client.mouseHandler.ypos() / window.guiScale).toFloat()

        traceCheckpoint("begin $drawPhase frame")
        traceCheckpoint("before GL state capture")
        val oldState = GlStateSnapshot.capture()
        traceCheckpoint("after GL state capture")

        try {
            traceCheckpoint("before framebuffer bind")
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glViewport(0, 0, window.width, window.height)
            glUseProgram(0)
            glBindVertexArray(0)
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, 0)
            glBindSampler(0, 0)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glDisable(GL_SCISSOR_TEST)
            glDisable(GL_DEPTH_TEST)
            glDisable(GL_CULL_FACE)
            glDisable(GL_STENCIL_TEST)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glColorMask(true, true, true, true)
            traceCheckpoint("after framebuffer bind", finish = true)

            prepare()
            val active = runtime()
            traceCheckpoint("before nvgBeginFrame")
            active.backend.beginFrame(width, height, scale)
            traceCheckpoint("after nvgBeginFrame", finish = true)
            try {
                traceCheckpoint("before $drawPhase draw")
                draw(active.renderer, mouseX, mouseY)
                traceCheckpoint("after $drawPhase draw", finish = true)
            } finally {
                traceCheckpoint("before nvgEndFrame")
                active.backend.endFrame()
                traceCheckpoint("after nvgEndFrame", finish = true)
            }
        } finally {
            traceCheckpoint("before GL state restoration")
            oldState.restore()
            traceCheckpoint("after GL state restoration", finish = true)
            traceCheckpoint("end $drawPhase frame")
        }
    }

    private fun isDetailedTraceActive(): Boolean =
        IqNanoVgConfiguration.mode().tracesGl() && traceFramesRemaining > 0

    private fun traceCheckpoint(phase: String, finish: Boolean = false, force: Boolean = false) {
        if (!IqNanoVgConfiguration.mode().tracesGl() || (!force && !isDetailedTraceActive())) return
        if (finish) glFinish()
        val error = glGetError()
        logger.info(
            "[IQ NanoVG trace frame={}] {} | glError=0x{} | {}",
            traceFrameNumber,
            phase,
            error.toString(16).uppercase().padStart(4, '0'),
            GlStateSnapshot.currentSummary(),
        )
    }
}
