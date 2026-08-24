#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// Convert RGB to HSV
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// Convert HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    // Aspect-ratio corrected UV coordinates centered at (0, 0)
    vec2 uv = texCoord - 0.5;
    uv.x *= OutSize.x / OutSize.y;

    float dist = length(uv);
    float angle = atan(uv.y, uv.x);

    // 8-Fold Mirrored Kaleidoscope Radial Symmetry
    float segments = 8.0;
    float segmentAngle = 3.14159265359 / segments;
    
    angle = mod(angle, 2.0 * segmentAngle);
    if (angle > segmentAngle) {
        angle = 2.0 * segmentAngle - angle; // Mirrored reflection fold
    }

    // Multi-tier crystalline fractal folding
    vec2 foldedUV = vec2(cos(angle), sin(angle)) * dist;

    for (int i = 0; i < 3; i++) {
        foldedUV = abs(foldedUV) - 0.12 * float(i + 1);
        float a2 = atan(foldedUV.y, foldedUV.x);
        float d2 = length(foldedUV);
        a2 = mod(a2, segmentAngle);
        if (a2 > segmentAngle * 0.5) a2 = segmentAngle - a2;
        foldedUV = vec2(cos(a2), sin(a2)) * d2;
    }

    vec2 sampleUV = fract(foldedUV + 0.5);

    // Chromatic Aberration / Prism Dispersion of RGB channels
    vec2 offsetR = foldedUV * 0.012;
    vec2 offsetB = -foldedUV * 0.012;

    float r = texture(InSampler, fract(sampleUV + offsetR)).r;
    float g = texture(InSampler, sampleUV).g;
    float b = texture(InSampler, fract(sampleUV + offsetB)).b;
    vec3 baseColor = vec3(r, g, b);

    // Stained-Glass / Fractal Psychedelic Palette Shift
    vec3 hsv = rgb2hsv(baseColor);
    hsv.x = fract(hsv.x + dist * 1.6 + angle * 0.9);
    hsv.y = clamp(hsv.y * 1.7 + 0.25, 0.0, 1.0);
    hsv.z = pow(hsv.z, 0.82) * 1.3;

    vec3 vibrantColor = hsv2rgb(hsv);

    // Facet crystalline edge glow
    float facetEdge = abs(sin(angle * segments * 2.0));
    float edgeGlow = smoothstep(0.92, 1.0, facetEdge) * 0.35;
    vibrantColor += vec3(edgeGlow * 1.0, edgeGlow * 0.8, edgeGlow * 0.3);

    // Circular lens aperture vignette
    float barrelRadius = 0.58;
    float vignette = smoothstep(barrelRadius, barrelRadius - 0.08, dist);
    vibrantColor *= vignette;

    fragColor = vec4(vibrantColor, 1.0);
}
