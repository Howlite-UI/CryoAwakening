package com.howlite.cryoawakening.worldgen

import net.minecraft.world.level.biome.Climate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CryoWorldGenConfig
 *
 * Configuration centralisée pour la génération du biome Cryo Caverns et de la cathédrale de glace.
 * Permet d'ajuster facilement la taille/fréquence du biome et le spawn de la cave.
 */
object CryoWorldGenConfig {

    // ── 1. Paramètres MultiNoise du Biome (Taille & Fréquence) ────────────────
    // Plages ciblées et resserrées pour rendre le biome RARE et sous forme de poches isolées.

    /** Température : Zone très froide / permafrost (-0.95f à -0.70f) */
    val TEMPERATURE_SPAN: Climate.Parameter = Climate.Parameter.span(-0.95f, -0.70f)

    /** Humidité : Humidité saturée (0.50f à 0.85f) */
    val HUMIDITY_SPAN: Climate.Parameter = Climate.Parameter.span(0.50f, 0.85f)

    /** Continentalité : Terres intérieures profondes (0.30f à 0.70f) */
    val CONTINENTALNESS_SPAN: Climate.Parameter = Climate.Parameter.span(0.30f, 0.70f)

    /** Érosion : Zones de cavités et d'érosion souterraine (-0.75f à -0.45f) */
    val EROSION_SPAN: Climate.Parameter = Climate.Parameter.span(-0.75f, -0.45f)

    /** Profondeur : STRICTEMENT sous-sol très profond (Y < -25, 0.80f à 1.30f) */
    val DEPTH_SPAN: Climate.Parameter = Climate.Parameter.span(0.80f, 1.30f)

    /** Weirdness : Tranche ciblée (-0.25f à 0.25f) */
    val WEIRDNESS_SPAN: Climate.Parameter = Climate.Parameter.span(-0.25f, 0.25f)

    /** Offset : 0.0f (optimal pour l'arbre RTree de /locate biome) */
    const val OFFSET: Float = 0.0f


    // ── 2. Paramètres de Génération de la Cave (Cathédrale) ───────────────────

    /** Espacement de la grille (en blocs) : 600 blocs (garantit 1 seule cave unique, sans découpe de chunk) */
    const val CAVE_GRID_SPACING: Int = 600

    /** Rayons de la cave en X (horizontal) */
    const val RADIUS_X_MIN: Double = 85.0
    const val RADIUS_X_MAX: Double = 105.0

    /** Rayons de la cave en Z (horizontal) */
    const val RADIUS_Z_MIN: Double = 75.0
    const val RADIUS_Z_MAX: Double = 95.0

    /** Altitude du sol sur les bords de la cave */
    const val BASE_EDGE_FLOOR_Y: Int = -49

    /** Creux maximal au centre de la cave (Y = -54 au centre) */
    const val FLOOR_MAX_DIP: Int = 5

    /** Hauteur sous plafond maximale de la cave au centre (en blocs) */
    const val CAVE_MAX_HEIGHT: Int = 38

    /** Protection absolue de la bedrock */
    const val BEDROCK_SAFE_Y: Int = -58


    // ── 3. Fonctions Mathématiques Déterministes Partagées ────────────────────

    fun hash1D(seed: Long): Double {
        var n = seed
        n = n xor (n shl 21)
        n = n xor (n ushr 35)
        n = n xor (n shl 4)
        return (n and 0x7FFFFFFFL).toDouble() / 0x7FFFFFFFL.toDouble()
    }

    fun getDomeSeed(gridX: Int, gridZ: Int): Long {
        return gridX.toLong() * 341873128712L xor gridZ.toLong() * 132897987541L
    }

    fun getCaveCenter(gridX: Int, gridZ: Int): Pair<Int, Int> {
        val domeSeed = getDomeSeed(gridX, gridZ)
        val centerX = gridX * CAVE_GRID_SPACING + (CAVE_GRID_SPACING / 2) + ((hash1D(domeSeed) - 0.5) * 20).toInt()
        val centerZ = gridZ * CAVE_GRID_SPACING + (CAVE_GRID_SPACING / 2) + ((hash1D(domeSeed xor 0x1A2BL) - 0.5) * 20).toInt()
        return Pair(centerX, centerZ)
    }

    fun getCaveRadii(domeSeed: Long): Pair<Double, Double> {
        val radX = RADIUS_X_MIN + hash1D(domeSeed xor 0x3C4DL) * (RADIUS_X_MAX - RADIUS_X_MIN)
        val radZ = RADIUS_Z_MIN + hash1D(domeSeed xor 0x5E6FL) * (RADIUS_Z_MAX - RADIUS_Z_MIN)
        return Pair(radX, radZ)
    }

    /**
     * Vérifie si les coordonnées 3D (blockX, blockY, blockZ) sont situées à l'intérieur
     * de l'enveloppe exacte de la cathédrale de glace (rayon horizontal + couche souterraine).
     *
     * Utilisé pour aligner le biome CRYO_CAVERNS à 100% avec le dôme physique de la cave.
     */
    fun isInsideCryoCavern(blockX: Int, blockY: Int, blockZ: Int): Boolean {
        // 1. Limite verticale stricte : uniquement dans la zone souterraine de la cave (Y = -58 à -10)
        if (blockY < BEDROCK_SAFE_Y || blockY > (BASE_EDGE_FLOOR_Y + CAVE_MAX_HEIGHT + 2)) {
            return false
        }

        // 2. Coordonnées de cellule et centre déterministe
        val gridX = Math.floorDiv(blockX, CAVE_GRID_SPACING)
        val gridZ = Math.floorDiv(blockZ, CAVE_GRID_SPACING)
        val (centerX, centerZ) = getCaveCenter(gridX, gridZ)

        val domeSeed = getDomeSeed(gridX, gridZ)
        val (radX, radZ) = getCaveRadii(domeSeed)

        val dx = (blockX - centerX) / radX
        val dz = (blockZ - centerZ) / radZ
        val dOvoidSq = dx * dx + dz * dz

        // Rejet rapide
        if (dOvoidSq > 1.35 * 1.35) return false

        // 3. Déformation harmonique identique à la génération physique
        val phi1 = hash1D(domeSeed xor 0x7A8BL) * 6.2831853
        val phi2 = hash1D(domeSeed xor 0x9C0DL) * 6.2831853
        val phi3 = hash1D(domeSeed xor 0xB41FL) * 6.2831853

        val amp1 = 0.12 + hash1D(domeSeed xor 0x1111L) * 0.08
        val amp2 = 0.08 + hash1D(domeSeed xor 0x2222L) * 0.06
        val amp3 = 0.04 + hash1D(domeSeed xor 0x3333L) * 0.04

        val theta = atan2(dz, dx)
        val harmonicDeform = 1.0 + amp1 * sin(2.0 * theta + phi1) + amp2 * cos(3.0 * theta + phi2) + amp3 * sin(5.0 * theta + phi3)
        val dOvoid = sqrt(dOvoidSq) / harmonicDeform

        // 4. Enveloppe totale de la cathédrale + coque en gabbro (dOvoid <= 1.25)
        return dOvoid <= 1.25
    }
}
