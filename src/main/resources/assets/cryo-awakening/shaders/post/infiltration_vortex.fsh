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
        p = p * 2.04 + vec2(13.7, 7.3);
        amp *= 0.5;
    }
    return v;
}

void main() {
    // Temps continu en secondes (GameTime va de 0 à 1 sur 24000 ticks = 1200s)
    float t = GameTime * 1200.0;

    // Coordonnées centrées corrigées par le ratio d'aspect
    vec2 uv = texCoord - 0.5;
    uv.x *= OutSize.x / OutSize.y;

    // Léger flottement organique de caméra dans le courant d'énergie
    uv += vec2(sin(t * 2.3), cos(t * 1.9)) * 0.016;

    float r = length(uv);
    float angle = atan(uv.y, uv.x);

    // Projection de tunnel 3D en perspective vers la surface lumineuse
    float depth = 0.62 / (r + 0.09);
    float travel = depth - t * 5.6;

    // Spirale fluide inspirée de l'Infiltration de Zelda TOTK
    float spiralAngle = angle + depth * 0.55 - t * 1.35;
    vec2 tunnelUV = vec2(spiralAngle / 3.14159265 * 3.0, travel * 0.65);

    // 1. Nappes nébuleuses émeraude / turquoise en mouvement
    float n1 = fbm(tunnelUV + vec2(0.0, -t * 1.2));
    float n2 = fbm(tunnelUV * 2.1 - vec2(t * 0.4, t * 2.2));
    float nebula = smoothstep(0.18, 0.85, n1 * 0.65 + n2 * 0.45);

    // 2. Rubans lumineux spiralés le long des parois du vortex
    float ribbonWave = sin(spiralAngle * 6.0 + n1 * 3.5);
    float ribbons = pow(max(0.0, ribbonWave), 4.0) * smoothstep(0.06, 0.48, r);

    // 3. Ondes concentriques qui défilent rapidement vers le joueur (sensation de vitesse)
    float ringWave = sin(travel * 4.2 + n1 * 1.8);
    float rings = pow(0.5 + 0.5 * ringWave, 7.0) * smoothstep(0.05, 0.35, r);

    // 4. Stries d'énergie cristalline (traînées rapides)
    float streakSector = floor((angle + 3.14159265) * 14.0);
    float streakRand = hash21(vec2(streakSector, 19.7));
    float streakSpeed = fract(depth * 0.42 - t * (1.8 + streakRand * 1.6) + streakRand);
    float streakMask = smoothstep(0.0, 0.12, streakSpeed) * smoothstep(0.45, 0.08, streakSpeed);
    float streakThin = pow(max(0.0, cos((angle * 14.0 - streakSector) * 3.14159)), 8.0);
    float streaks = streakMask * streakThin * smoothstep(0.08, 0.42, r);

    // 5. Palette Cryo-Émeraude lumineuse (style TOTK + thème Cryo Awakening)
    vec3 abyssColor   = vec3(0.015, 0.085, 0.105);
    vec3 jadeColor    = vec3(0.045, 0.48, 0.44);
    vec3 emeraldGlow  = vec3(0.18, 0.96, 0.72);
    vec3 iceCyanGlow  = vec3(0.45, 0.98, 1.0);
    vec3 portalCore   = vec3(0.86, 1.0, 0.96);

    vec3 col = mix(abyssColor, jadeColor, nebula);
    col += emeraldGlow * ribbons * 0.72;
    col += iceCyanGlow * rings * 0.62;
    col += portalCore * streaks * 0.95;

    // 6. Cœur lumineux du tunnel (lumière de la surface droit devant)
    float corePulse = 0.92 + 0.08 * sin(t * 6.5);
    float centerGlow = exp(-r * 4.2) * corePulse;
    float innerHalo = exp(-r * 1.8) * 0.35;
    col += emeraldGlow * innerHalo;
    col = mix(col, portalCore, clamp(centerGlow * 1.15, 0.0, 1.0));

    // 7. Douce vignette périphérique avec frange turquoise vibrante
    float edgeBoost = smoothstep(0.45, 0.95, r);
    col *= (1.0 - edgeBoost * 0.38);
    col += vec3(0.02, 0.16, 0.18) * edgeBoost * (0.5 + 0.5 * sin(angle * 4.0 - t * 3.0));

    // Tone mapping subtil pour garder des blancs doux sans écrêtage dur
    col = col / (1.0 + col * 0.25);
    col += texture(InSampler, texCoord).rgb * 0.0001;

    fragColor = vec4(col, 1.0);
}
