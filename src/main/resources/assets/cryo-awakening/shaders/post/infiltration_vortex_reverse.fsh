#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p = p * 2.04 + vec2(11.3, 5.9);
        amp *= 0.5;
    }
    return v;
}

void main() {
    float t = GameTime * 1200.0;

    vec2 uv = texCoord - 0.5;
    uv.x *= OutSize.x / OutSize.y;
    uv += vec2(cos(t * 2.4), sin(t * 2.0)) * 0.014;

    float r = length(uv);
    float angle = atan(uv.y, uv.x);

    // Flux INVERSÉ (redescente vers la position d'origine)
    float depth = 0.62 / (r + 0.09);
    float travel = depth + t * 6.0;

    float spiralAngle = angle - depth * 0.55 + t * 1.45;
    vec2 tunnelUV = vec2(spiralAngle / 3.14159265 * 3.0, travel * 0.65);

    float n1 = fbm(tunnelUV + vec2(0.0, t * 1.3));
    float n2 = fbm(tunnelUV * 2.1 + vec2(t * 0.4, t * 2.1));
    float nebula = smoothstep(0.18, 0.85, n1 * 0.65 + n2 * 0.45);

    float ribbons = pow(max(0.0, sin(spiralAngle * 6.0 + n1 * 3.2)), 4.0) * smoothstep(0.06, 0.48, r);
    float rings = pow(0.5 + 0.5 * sin(travel * 4.2 + n1 * 1.8), 7.0) * smoothstep(0.05, 0.35, r);

    // Palette Saphir / Bleu Glaciaire de rembobinage
    vec3 abyssColor  = vec3(0.02, 0.05, 0.13);
    vec3 azureColor  = vec3(0.08, 0.30, 0.62);
    vec3 cryoGlow    = vec3(0.32, 0.78, 1.0);
    vec3 coreWhite   = vec3(0.84, 0.94, 1.0);

    vec3 col = mix(abyssColor, azureColor, nebula);
    col += cryoGlow * ribbons * 0.72;
    col += coreWhite * rings * 0.52;

    float centerGlow = exp(-r * 4.5) * (0.9 + 0.1 * sin(t * 7.0));
    col += cryoGlow * exp(-r * 2.0) * 0.32;
    col = mix(col, coreWhite, clamp(centerGlow, 0.0, 1.0));

    float edgeBoost = smoothstep(0.45, 0.95, r);
    col *= (1.0 - edgeBoost * 0.42);

    col = col / (1.0 + col * 0.25);
    col += texture(InSampler, texCoord).rgb * 0.0001;
    fragColor = vec4(col, 1.0);
}
