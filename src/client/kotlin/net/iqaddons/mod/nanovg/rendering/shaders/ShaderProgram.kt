package net.iqaddons.mod.nanovg.rendering.shaders

import org.lwjgl.opengl.GL33.*

/**
 * Thin wrapper around a compiled GL program. Deliberately dumb — no
 * reflection-based uniform caching magic, just explicit `uniform*` calls —
 * because this is the one place in the codebase where raw GL state is
 * unavoidable (NanoVG has no first-class Gaussian blur), and it should stay
 * easy to audit.
 */
class ShaderProgram(vertexSource: String, fragmentSource: String) : AutoCloseable {

    val id: Int = glCreateProgram()
    private val uniformLocations = HashMap<String, Int>()

    init {
        val vertex = compile(GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GL_FRAGMENT_SHADER, fragmentSource)
        glAttachShader(id, vertex)
        glAttachShader(id, fragment)
        glLinkProgram(id)
        check(glGetProgrami(id, GL_LINK_STATUS) != GL_FALSE) {
            "Shader link failed: ${glGetProgramInfoLog(id)}"
        }
        glDeleteShader(vertex)
        glDeleteShader(fragment)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        check(glGetShaderi(shader, GL_COMPILE_STATUS) != GL_FALSE) {
            "Shader compile failed: ${glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    fun use() = glUseProgram(id)

    fun uniform1i(name: String, value: Int) = glUniform1i(location(name), value)
    fun uniform2f(name: String, x: Float, y: Float) = glUniform2f(location(name), x, y)
    fun uniform1f(name: String, value: Float) = glUniform1f(location(name), value)

    private fun location(name: String): Int =
        uniformLocations.getOrPut(name) { glGetUniformLocation(id, name) }

    override fun close() = glDeleteProgram(id)
}
