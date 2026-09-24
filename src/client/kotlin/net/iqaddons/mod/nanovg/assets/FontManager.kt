package net.iqaddons.mod.nanovg.assets

import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.Lifecycle
import net.iqaddons.mod.nanovg.rendering.nanovg.NanoVGContext
import org.lwjgl.nanovg.NanoVG.nvgCreateFontMem
import org.lwjgl.system.MemoryUtil
import java.io.File
import java.nio.ByteBuffer

/**
 * Loads font files from mod resources into the NanoVG context and exposes
 * `family name -> NanoVG font handle` lookups. NanoVG's built-in font
 * rasterizer (stb_truetype under the hood) already gives us dynamic sizing,
 * kerning, a glyph atlas, and Unicode coverage without a bespoke MSDF
 * pipeline — see the note in the class doc for when that would change.
 *
 * A hand-rolled MSDF atlas system would only pay for itself once glyphs are
 * rendered at wildly different scales in the same frame with crisp edges
 * required at all of them (e.g. a huge animated headline). NanoVG's
 * per-size rasterized atlas already covers every size used by this theme's
 * [IQ.ui.theme.Typography] tokens cleanly; swapping the rasterizer later
 * is a matter of implementing a new backend behind [FontHandleResolver],
 * not a widget-level change.
 */
class FontManager(private val context: NanoVGContext) : Lifecycle, FontHandleResolver {

    private data class LoadedFont(val handle: Int, val buffer: ByteBuffer?)

    private val fonts = HashMap<String, LoadedFont>()

    val isReady: Boolean get() = fonts.isNotEmpty()

    /** family name -> classpath resource path, registered once at start-up. */
    private val registrations = linkedMapOf(
        "Rajdhani-Regular" to "/assets/iq/font/rajdhani_regular.ttf",
        "Rajdhani-Medium" to "/assets/iq/font/rajdhani_regular.ttf",
        "Rajdhani-SemiBold" to "/assets/iq/font/rajdhani_bold.ttf",
        "Rajdhani-Bold" to "/assets/iq/font/rajdhani_bold.ttf",
    )

    override fun start() {
        if (isReady) return
        registrations.forEach { (family, path) -> loadFont(family, path) }
    }

    override fun stop() {
        fonts.values.forEach { it.buffer?.let(MemoryUtil::memFree) }
        fonts.clear()
    }

    override fun handleFor(family: String): Int =
        fonts[family]?.handle ?: fonts[FALLBACK_FAMILY]?.handle ?: -1

    private fun loadFont(family: String, resourcePath: String) {
        val resourceBytes = javaClass.getResourceAsStream(resourcePath)?.use { it.readBytes() }
        val bytes = resourceBytes ?: loadSystemFallbackFontBytes(family)
        if (bytes == null) {
            IqNanoVg.logger.warn("Font resource missing and no system fallback was accepted for '$family': $resourcePath")
            return
        }

        val buffer = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
        val handle = nvgCreateFontMem(context.handle, family, buffer, false)
        if (handle == -1) {
            IqNanoVg.logger.warn("NanoVG rejected font '$family' ($resourcePath)")
            MemoryUtil.memFree(buffer)
            return
        }
        fonts[family] = LoadedFont(handle, buffer)
        IqNanoVg.logger.info("NanoVG font loaded: $family")
    }

    private fun loadSystemFallbackFontBytes(family: String): ByteArray? {
        val preferred = when {
            family.contains("Bold", ignoreCase = true) || family.contains("SemiBold", ignoreCase = true) -> listOf(
                "C:/Windows/Fonts/arialbd.ttf",
                "C:/Windows/Fonts/calibrib.ttf",
                "C:/Windows/Fonts/segoeuib.ttf",
            )
            family.contains("Medium", ignoreCase = true) -> listOf(
                "C:/Windows/Fonts/bahnschrift.ttf",
                "C:/Windows/Fonts/calibri.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
            )
            else -> emptyList()
        }
        val candidates = preferred + listOf(
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/calibri.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
        )
        return candidates.firstNotNullOfOrNull { path ->
            File(path).takeIf { it.isFile }?.readBytes()
        }
    }

    companion object {
        const val FALLBACK_FAMILY = "Rajdhani-Regular"
    }
}

/** Narrow contract [IQ.rendering.nanovg.NanoVGBackend] needs from [FontManager] — keeps the backend decoupled from asset loading details. */
fun interface FontHandleResolver {
    fun handleFor(family: String): Int
}
