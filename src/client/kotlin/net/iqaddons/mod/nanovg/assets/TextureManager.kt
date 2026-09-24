package net.iqaddons.mod.nanovg.assets

import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.Lifecycle
import net.iqaddons.mod.nanovg.rendering.nanovg.NanoVGContext
import net.iqaddons.mod.nanovg.util.Size
import org.lwjgl.nanovg.NanoVG.NVG_IMAGE_GENERATE_MIPMAPS
import org.lwjgl.nanovg.NanoVG.nvgCreateImageMem
import org.lwjgl.nanovg.NanoVG.nvgDeleteImage
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * Loads image assets as NanoVG images, keyed by resource path so a given
 * texture is uploaded to the GPU at most once regardless of how many widgets
 * reference it.
 */
class TextureManager(private val context: NanoVGContext) : Lifecycle {

    data class Texture(val id: Int, val size: Size)

    private val cache = HashMap<String, Texture>()

    override fun start() {}

    override fun stop() {
        if (context.isReady) {
            cache.values.forEach { nvgDeleteImage(context.handle, it.id) }
        }
        cache.clear()
    }

    /** Returns the cached GL texture id for [resourcePath], loading it on first request. */
    fun textureFor(resourcePath: String): Int? = texture(resourcePath)?.id

    fun textureSize(resourcePath: String): Size? = texture(resourcePath)?.size

    private fun texture(resourcePath: String): Texture? = cache.getOrPut(resourcePath) {
        load(resourcePath) ?: return null
    }.takeIf { it.id != 0 }

    private fun load(resourcePath: String): Texture? {
        if (!context.isReady) {
            IqNanoVg.logger.warn("Texture requested before NanoVG was ready: $resourcePath")
            return null
        }
        val bytes = javaClass.getResourceAsStream(resourcePath)?.use { it.readBytes() }
        if (bytes == null) {
            IqNanoVg.logger.warn("Texture resource missing: $resourcePath")
            return null
        }
        MemoryStack.stackPush().use { stack ->
            val nativeBytes = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            val ok = STBImage.stbi_info_from_memory(nativeBytes, width, height, channels)
            if (!ok) {
                IqNanoVg.logger.warn("Failed to inspect texture: $resourcePath (${STBImage.stbi_failure_reason()})")
                MemoryUtil.memFree(nativeBytes)
                return null
            }
            nativeBytes.rewind()
            val image = nvgCreateImageMem(context.handle, NVG_IMAGE_GENERATE_MIPMAPS, nativeBytes)
            MemoryUtil.memFree(nativeBytes)
            if (image == 0) {
                IqNanoVg.logger.warn("NanoVG rejected texture: $resourcePath")
                return null
            }
            return Texture(image, Size(width.get(0).toFloat(), height.get(0).toFloat()))
        }
    }
}
