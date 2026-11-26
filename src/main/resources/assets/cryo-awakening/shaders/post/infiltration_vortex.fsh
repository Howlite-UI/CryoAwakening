#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0;
    vec2 texel = 1.0 / max(InSize, vec2(1.0));

    // Scène 3D nette (personnage vertical + spirales voxel pixel-art + colonne en escaliers + carrés lumineux)
    vec3 scene = texture(InSampler, texCoord).rgb;

    // Bloom maîtrisé et serré pour faire rayonner les voxels blanc-menthe SANS flouter les arêtes pixel-art !
    vec3 tightGlow = vec3(0.0);
    vec3 haloGlow = vec3(0.0);

    const float TAU = 6.2831853;
    for (int i = 0; i < 8; i++) {
        float ang = float(i) * (TAU / 8.0);
        vec2 off = vec2(cos(ang), sin(ang)) * texel * 3.2;
        vec3 s = texture(InSampler, clamp(texCoord + off, 0.001, 0.999)).rgb;
        float luma = dot(s, vec3(0.2126, 0.7152, 0.0722));
        tightGlow += s * smoothstep(0.58, 0.96, luma);
    }
    tightGlow /= 8.0;

    for (int i = 0; i < 8; i++) {
        float ang = float(i) * (TAU / 8.0) + 0.39;
        vec2 off = vec2(cos(ang), sin(ang)) * texel * 9.0;
        vec3 s = texture(InSampler, clamp(texCoord + off, 0.001, 0.999)).rgb;
        float luma = dot(s, vec3(0.2126, 0.7152, 0.0722));
        haloGlow += s * smoothstep(0.55, 0.95, luma);
    }
    haloGlow /= 8.0;

    // Harmonie colorimétrique émeraude-sarcelle nette (préserve 100% la netteté du pixel-art)
    vec3 col = mix(scene, scene * vec3(0.86, 1.06, 1.01), 0.22);
    col += tightGlow * vec3(0.35, 1.02, 0.84) * 0.44;
    col += haloGlow * vec3(0.10, 0.88, 0.68) * 0.30;

    // Légère vignette sarcelle profonde sur les bords (identique au fond sombre de ton illustration)
    vec2 centered = (texCoord - 0.5) * vec2(OutSize.x / OutSize.y, 1.0);
    float dist = length(centered);
    float vignette = smoothstep(0.42, 1.08, dist);
    col = mix(col, vec3(0.006, 0.055, 0.068), vignette * 0.56);

    // Légère pulsation subtile liée à GameTime (garde le shader vivant)
    col *= (0.985 + 0.015 * sin(t * 3.0));

    col = col / (1.0 + col * 0.12);
    fragColor = vec4(col, 1.0);
}
