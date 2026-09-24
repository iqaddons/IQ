package net.iqaddons.mod.nanovg.rendering.shaders

/**
 * A full-screen triangle vertex shader (no vertex buffer needed — the
 * three vertices are derived from `gl_VertexID`) paired with a 9-tap
 * separable Gaussian fragment shader. Two passes (horizontal, then
 * vertical) applied to a downsampled capture of the scene behind a panel
 * is what [IQ.rendering.blur.BlurPass] uses to implement glass-panel
 * backdrop blur cheaply.
 */
object Shaders {

    val FULLSCREEN_TRIANGLE_VERTEX = """
        #version 330 core
        out vec2 vUv;
        void main() {
            vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
            vUv = pos;
            gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
        }
    """.trimIndent()

    val GAUSSIAN_BLUR_FRAGMENT = """
        #version 330 core
        in vec2 vUv;
        out vec4 fragColor;

        uniform sampler2D uSource;
        uniform vec2 uTexelDirection; // (1/width, 0) horizontal pass, (0, 1/height) vertical pass
        uniform float uRadius;

        // 9-tap Gaussian weights, sigma tuned for a soft "glass" look at uRadius scale.
        const float WEIGHTS[5] = float[5](0.2270270270, 0.1945945946, 0.1216216216, 0.0540540541, 0.0162162162);

        void main() {
            vec4 sum = texture(uSource, vUv) * WEIGHTS[0];
            for (int i = 1; i < 5; i++) {
                vec2 offset = uTexelDirection * float(i) * uRadius;
                sum += texture(uSource, vUv + offset) * WEIGHTS[i];
                sum += texture(uSource, vUv - offset) * WEIGHTS[i];
            }
            fragColor = sum;
        }
    """.trimIndent()
}
