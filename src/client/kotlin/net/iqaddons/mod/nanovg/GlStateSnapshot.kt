package net.iqaddons.mod.nanovg

import org.lwjgl.opengl.GL33.*

/** Actual OpenGL state touched by NanoVG/BlurPass and commonly cached by renderers. */
internal class GlStateSnapshot private constructor(
    private val viewport: IntArray,
    private val scissorBox: IntArray,
    private val readFramebuffer: Int,
    private val drawFramebuffer: Int,
    private val program: Int,
    private val vao: Int,
    private val arrayBuffer: Int,
    private val elementArrayBuffer: Int,
    private val uniformBuffer: Int,
    private val uniformBuffer0: Int,
    private val uniformBuffer0Start: Long,
    private val uniformBuffer0Size: Long,
    private val pixelPackBuffer: Int,
    private val pixelUnpackBuffer: Int,
    private val activeTexture: Int,
    private val activeTexture2d: Int,
    private val activeSampler: Int,
    private val texture0: Int,
    private val sampler0: Int,
    private val packAlignment: Int,
    private val unpackAlignment: Int,
    private val packRowLength: Int,
    private val unpackRowLength: Int,
    private val packSkipRows: Int,
    private val unpackSkipRows: Int,
    private val packSkipPixels: Int,
    private val unpackSkipPixels: Int,
    private val blendSrcRgb: Int,
    private val blendDstRgb: Int,
    private val blendSrcAlpha: Int,
    private val blendDstAlpha: Int,
    private val blendEquationRgb: Int,
    private val blendEquationAlpha: Int,
    private val cullFaceMode: Int,
    private val frontFace: Int,
    private val colorMask: IntArray,
    private val depthMask: Boolean,
    private val stencilFront: StencilState,
    private val stencilBack: StencilState,
    private val scissorEnabled: Boolean,
    private val depthEnabled: Boolean,
    private val cullEnabled: Boolean,
    private val blendEnabled: Boolean,
    private val stencilEnabled: Boolean,
) {
    internal fun restore() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFramebuffer)
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])

        glUseProgram(if (program == 0 || glIsProgram(program)) program else 0)
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer)
        if (vao != 0) glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer)
        if (uniformBuffer0 == 0) {
            glBindBufferBase(GL_UNIFORM_BUFFER, 0, 0)
        } else {
            glBindBufferRange(GL_UNIFORM_BUFFER, 0, uniformBuffer0, uniformBuffer0Start, uniformBuffer0Size)
        }
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBuffer)
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pixelPackBuffer)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture0)
        glBindSampler(0, sampler0)
        glActiveTexture(activeTexture)
        glBindTexture(GL_TEXTURE_2D, activeTexture2d)
        glBindSampler((activeTexture - GL_TEXTURE0).coerceAtLeast(0), activeSampler)

        glPixelStorei(GL_PACK_ALIGNMENT, packAlignment)
        glPixelStorei(GL_UNPACK_ALIGNMENT, unpackAlignment)
        glPixelStorei(GL_PACK_ROW_LENGTH, packRowLength)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, unpackRowLength)
        glPixelStorei(GL_PACK_SKIP_ROWS, packSkipRows)
        glPixelStorei(GL_UNPACK_SKIP_ROWS, unpackSkipRows)
        glPixelStorei(GL_PACK_SKIP_PIXELS, packSkipPixels)
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, unpackSkipPixels)

        glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha)
        glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha)
        glCullFace(cullFaceMode)
        glFrontFace(frontFace)
        glColorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0)
        glDepthMask(depthMask)
        stencilFront.restore(GL_FRONT)
        stencilBack.restore(GL_BACK)

        setEnabled(GL_SCISSOR_TEST, scissorEnabled)
        setEnabled(GL_DEPTH_TEST, depthEnabled)
        setEnabled(GL_CULL_FACE, cullEnabled)
        setEnabled(GL_BLEND, blendEnabled)
        setEnabled(GL_STENCIL_TEST, stencilEnabled)
    }

    internal fun summary(): String =
        "readFbo=$readFramebuffer drawFbo=$drawFramebuffer program=$program vao=$vao " +
            "arrayBuffer=$arrayBuffer elementBuffer=$elementArrayBuffer uniformBuffer=$uniformBuffer " +
            "uniformBuffer0=$uniformBuffer0 packBuffer=$pixelPackBuffer unpackBuffer=$pixelUnpackBuffer activeTexture=$activeTexture " +
            "texture2D=$activeTexture2d sampler=$activeSampler viewport=[${viewport.joinToString()}] " +
            "blend=$blendEnabled depth=$depthEnabled stencil=$stencilEnabled scissor=$scissorEnabled"

    private data class StencilState(
        val function: Int,
        val reference: Int,
        val valueMask: Int,
        val writeMask: Int,
        val fail: Int,
        val depthFail: Int,
        val depthPass: Int,
    ) {
        fun restore(face: Int) {
            glStencilFuncSeparate(face, function, reference, valueMask)
            glStencilMaskSeparate(face, writeMask)
            glStencilOpSeparate(face, fail, depthFail, depthPass)
        }
    }

    companion object {
        internal fun capture(): GlStateSnapshot {
            val viewport = IntArray(4).also { glGetIntegerv(GL_VIEWPORT, it) }
            val scissorBox = IntArray(4).also { glGetIntegerv(GL_SCISSOR_BOX, it) }
            val colorMask = IntArray(4).also { glGetIntegerv(GL_COLOR_WRITEMASK, it) }
            val activeTexture = glGetInteger(GL_ACTIVE_TEXTURE)
            val activeTexture2d = glGetInteger(GL_TEXTURE_BINDING_2D)
            val activeSampler = glGetInteger(GL_SAMPLER_BINDING)
            glActiveTexture(GL_TEXTURE0)
            val texture0 = glGetInteger(GL_TEXTURE_BINDING_2D)
            val sampler0 = glGetInteger(GL_SAMPLER_BINDING)
            glActiveTexture(activeTexture)

            return GlStateSnapshot(
                viewport = viewport,
                scissorBox = scissorBox,
                readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING),
                drawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),
                program = glGetInteger(GL_CURRENT_PROGRAM),
                vao = glGetInteger(GL_VERTEX_ARRAY_BINDING),
                arrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING),
                elementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING),
                uniformBuffer = glGetInteger(GL_UNIFORM_BUFFER_BINDING),
                uniformBuffer0 = glGetIntegeri(GL_UNIFORM_BUFFER_BINDING, 0),
                uniformBuffer0Start = glGetInteger64i(GL_UNIFORM_BUFFER_START, 0),
                uniformBuffer0Size = glGetInteger64i(GL_UNIFORM_BUFFER_SIZE, 0),
                pixelPackBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING),
                pixelUnpackBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING),
                activeTexture = activeTexture,
                activeTexture2d = activeTexture2d,
                activeSampler = activeSampler,
                texture0 = texture0,
                sampler0 = sampler0,
                packAlignment = glGetInteger(GL_PACK_ALIGNMENT),
                unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT),
                packRowLength = glGetInteger(GL_PACK_ROW_LENGTH),
                unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH),
                packSkipRows = glGetInteger(GL_PACK_SKIP_ROWS),
                unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS),
                packSkipPixels = glGetInteger(GL_PACK_SKIP_PIXELS),
                unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS),
                blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB),
                blendDstRgb = glGetInteger(GL_BLEND_DST_RGB),
                blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA),
                blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA),
                blendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB),
                blendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA),
                cullFaceMode = glGetInteger(GL_CULL_FACE_MODE),
                frontFace = glGetInteger(GL_FRONT_FACE),
                colorMask = colorMask,
                depthMask = glGetInteger(GL_DEPTH_WRITEMASK) != 0,
                stencilFront = stencilState(false),
                stencilBack = stencilState(true),
                scissorEnabled = glIsEnabled(GL_SCISSOR_TEST),
                depthEnabled = glIsEnabled(GL_DEPTH_TEST),
                cullEnabled = glIsEnabled(GL_CULL_FACE),
                blendEnabled = glIsEnabled(GL_BLEND),
                stencilEnabled = glIsEnabled(GL_STENCIL_TEST),
            )
        }

        internal fun currentSummary(): String = capture().summary()

        private fun stencilState(back: Boolean): StencilState = StencilState(
            function = glGetInteger(if (back) GL_STENCIL_BACK_FUNC else GL_STENCIL_FUNC),
            reference = glGetInteger(if (back) GL_STENCIL_BACK_REF else GL_STENCIL_REF),
            valueMask = glGetInteger(if (back) GL_STENCIL_BACK_VALUE_MASK else GL_STENCIL_VALUE_MASK),
            writeMask = glGetInteger(if (back) GL_STENCIL_BACK_WRITEMASK else GL_STENCIL_WRITEMASK),
            fail = glGetInteger(if (back) GL_STENCIL_BACK_FAIL else GL_STENCIL_FAIL),
            depthFail = glGetInteger(if (back) GL_STENCIL_BACK_PASS_DEPTH_FAIL else GL_STENCIL_PASS_DEPTH_FAIL),
            depthPass = glGetInteger(if (back) GL_STENCIL_BACK_PASS_DEPTH_PASS else GL_STENCIL_PASS_DEPTH_PASS),
        )

        private fun setEnabled(capability: Int, enabled: Boolean) {
            if (enabled) glEnable(capability) else glDisable(capability)
        }
    }
}
