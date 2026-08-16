#version 330

layout(std140) uniform PaneInfo {
    vec4 PaneBounds;
    vec4 PaneRadii;
    vec4 PaneColorA;
    vec4 PaneColorB;
    vec4 PaneOptions;
};

out vec4 fragColor;

float roundedRectSdfPerCorner(vec2 local, vec2 size, vec4 radii) {
    bool right = local.x > size.x * 0.5;
    bool top = local.y > size.y * 0.5;
    float radius = top
        ? (right ? radii.y : radii.x)
        : (right ? radii.z : radii.w);
    radius = max(0.0, radius);
    vec2 halfSize = size * 0.5;
    vec2 centered = local - halfSize;
    vec2 q = abs(centered) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 origin = PaneBounds.xy;
    vec2 size = PaneBounds.zw;
    vec2 local = gl_FragCoord.xy - origin;

    float d = roundedRectSdfPerCorner(local, size, PaneRadii);
    float fill = 1.0 - smoothstep(0.0, 1.0, d);

    float glowWidth = max(PaneOptions.z, 0.0);
    float glowStrength = max(PaneOptions.w, 0.0);
    float glow = 0.0;
    if (glowWidth > 0.0 && glowStrength > 0.0) {
        float t = clamp(max(d, 0.0) / glowWidth, 0.0, 1.0);
        float falloff = 1.0 - t;
        glow = falloff * falloff * falloff * glowStrength;
    }

    float t = smoothstep(0.0, 1.0, clamp(local.x / max(1.0, size.x), 0.0, 1.0));
    vec4 fillColor = PaneOptions.x > 0.5 ? mix(PaneColorB, PaneColorA, t) : PaneColorA;
    vec4 glowColor = PaneColorB;

    float fillA = fillColor.a * fill;
    float glowA = glowColor.a * glow * (1.0 - fill);
    float alpha = fillA + glowA;
    if (alpha <= 0.001) {
        discard;
    }

    vec3 rgb = (fillColor.rgb * fillA + glowColor.rgb * glowA) / alpha;
    float fade = 1.0;
    if (PaneOptions.y > 0.5) {
        vec2 normalized = abs((local / max(size, vec2(1.0))) * 2.0 - 1.0);
        float distanceFromCenter = PaneOptions.y > 1.5
            ? length(normalized)
            : max(normalized.x, normalized.y);
        fade = 1.0 - smoothstep(0.28, 1.0, distanceFromCenter);
    }
    fragColor = vec4(rgb, alpha * fade);
}
