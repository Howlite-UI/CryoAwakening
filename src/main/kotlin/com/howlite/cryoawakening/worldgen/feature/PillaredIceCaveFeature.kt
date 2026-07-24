package com.howlite.cryoawakening.worldgen.feature

import com.howlite.cryoawakening.ModBlocks
import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PillaredIceCaveFeature — Engine de Cathédrale de Glace (Poches de Strates Localisées & Sans Minerai de Bismuth)
 *
 * Spécifications V19 :
 *  1. POCHES DE STRATES DIAGONALES LOCALISÉES ("PAS PARTOUT, POUR CASSER LA RÉGULARITÉ") :
 *     Les strates diagonales en Flysch ne recouvrent plus 100% de la cave.
 *     Elles émergent uniquement sous forme d'affleurements géologiques localisés en 3D (maskNoise > 0.38).
 *     Ailleurs (70% de la cave), la roche reste un Shivering Shale Stone ou Gabbro Stone pur.
 *
 *  2. SUPPRESSION DU MINERAI DE BISMUTH DES STRATES :
 *     Le minerai de bismuth est 100% retiré de la palette des strates.
 *
 *  3. BLOCS DE TRANSITION AUX CONTACTS DE LA BLUE ICE CONSERVÉS :
 *     - BLUE_ICE_SHEET_GABBRO_STONE
 *     - BLUE_ICE_SHEET_SHIVERING_SHALE_STONE
 *
 *  4. RELIEF DE SOL DOUX & SPÉLÉOTHÈMES VORONOI ANTI-GRILLE :
 *     Monticules douces agréables à traverser à pied et dôme voûté hermétique à 100%.
 */
class PillaredIceCaveFeature(codec: Codec<NoneFeatureConfiguration>) :
    Feature<NoneFeatureConfiguration>(codec) {

    companion object {
        private const val CAVE_GRID_SPACING = 380   // 380 blocs -> 1 seule cave géante par biome
        private const val VORONOI_CELL_SIZE = 36    // Cellule Voronoi de 36 blocs

        private const val BASE_EDGE_FLOOR_Y= -49   // Sol sur les bords (Y = -49)
        private const val FLOOR_MAX_DIP    = 5     // Creux maximal de 5 blocs au centre (Y = -54 au centre)
        private const val CAVE_MAX_HEIGHT  = 38    // Clearance monumentale (38 blocs de haut au centre)
        private const val BEDROCK_SAFE_Y   = -58   // Protection stricte de la bedrock

        private const val ICICLE_CHANCE   = 0.04  // 4% (petites stalactites de glace 1 à 3 blocs)
        private const val ICICLE_MAX_LEN  = 3

        private const val PLACE_FLAG = 2 // UPDATE_CLIENTS (zéro lag)

        // ── Palette des Blocs Mod & Vanilla ──────────────────────────────────
        private val AIR                                : BlockState = Blocks.AIR.defaultBlockState()
        private val ICE                                : BlockState = Blocks.ICE.defaultBlockState()
        private val PACKED_ICE                         : BlockState = Blocks.PACKED_ICE.defaultBlockState()
        private val BLUE_ICE                           : BlockState = Blocks.BLUE_ICE.defaultBlockState()
        private val SHIVERING_SHALE                    : BlockState = ModBlocks.SHIVERING_SHALE_STONE.defaultBlockState()
        private val GABBRO_STONE                       : BlockState = ModBlocks.GABBRO_STONE.defaultBlockState()
        private val BLUE_FROZEN_FLYSCH                 : BlockState = ModBlocks.BLUE_FROZEN_FLYSCH.defaultBlockState()
        private val FROZEN_FLYSCH                      : BlockState = ModBlocks.FROZEN_FLYSCH.defaultBlockState()
        private val BLUE_ICE_SHEET_GABBRO_STONE        : BlockState = ModBlocks.BLUE_ICE_SHEET_GABBRO_STONE.defaultBlockState()
        private val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE: BlockState = ModBlocks.BLUE_ICE_SHEET_SHIVERING_SHALE_STONE.defaultBlockState()
    }

    private data class SpeleoCandidate(
        val px: Double,
        val pz: Double,
        val pSeed: Long,
        val distSq: Double
    )

    override fun place(ctx: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val world  = ctx.level()
        val origin = ctx.origin()

        val chunkX = origin.x shr 4
        val chunkZ = origin.z shr 4
        val cxMin  = chunkX shl 4
        val czMin  = chunkZ shl 4
        val cxMax  = cxMin + 15
        val czMax  = czMin + 15

        val chunkMidX = cxMin + 8
        val chunkMidZ = czMin + 8
        val gridX = Math.floorDiv(chunkMidX, CAVE_GRID_SPACING)
        val gridZ = Math.floorDiv(chunkMidZ, CAVE_GRID_SPACING)
        val domeSeed = gridX.toLong() * 341873128712L xor gridZ.toLong() * 132897987541L

        val centerX = gridX * CAVE_GRID_SPACING + (CAVE_GRID_SPACING / 2) + ((hash1D(domeSeed) - 0.5) * 40).toInt()
        val centerZ = gridZ * CAVE_GRID_SPACING + (CAVE_GRID_SPACING / 2) + ((hash1D(domeSeed xor 0x1A2BL) - 0.5) * 40).toInt()

        // Paramètres de déformation angulaire unique pour CETTE caverne
        val phi1 = hash1D(domeSeed xor 0x7A8BL) * 6.2831853
        val phi2 = hash1D(domeSeed xor 0x9C0DL) * 6.2831853
        val phi3 = hash1D(domeSeed xor 0xB41FL) * 6.2831853

        val amp1 = 0.12 + hash1D(domeSeed xor 0x1111L) * 0.08
        val amp2 = 0.08 + hash1D(domeSeed xor 0x2222L) * 0.06
        val amp3 = 0.04 + hash1D(domeSeed xor 0x3333L) * 0.04

        // Quick distance check (max radius ~120 blocks * 1.35 max deformation + 16 chunk margin = 180 blocks)
        val cdx = (chunkMidX - centerX).toDouble()
        val cdz = (chunkMidZ - centerZ).toDouble()
        if (cdx * cdx + cdz * cdz > 180.0 * 180.0) return false

        var placedAny = false

        for (wx in cxMin..cxMax) {
            for (wz in czMin..czMax) {
                val radX = 90.0 + hash1D(domeSeed xor 0x3C4DL) * 30.0
                val radZ = 80.0 + hash1D(domeSeed xor 0x5E6FL) * 30.0

                val dx = (wx - centerX) / radX
                val dz = (wz - centerZ) / radZ
                val dOvoidSq = dx * dx + dz * dz

                val theta = atan2(dz, dx)
                val harmonicDeform = 1.0 + amp1 * sin(2.0 * theta + phi1) + amp2 * cos(3.0 * theta + phi2) + amp3 * sin(5.0 * theta + phi3)
                val dOvoid = sqrt(dOvoidSq) / harmonicDeform

                if (dOvoid > 1.25) continue

                // ── RELIEF DU SOL & RAMPE DE RACCORDEMENT PARABOLIQUE VERS LA PAROI ──────────
                val floorReliefN = 1.8 * sin(wx * 0.06 + phi1) * cos(wz * 0.06 + phi2) + 1.2 * cos((wx - wz) * 0.10 + phi3)
                val bowlRatio    = (1.0 - (dOvoid / 0.88)).coerceIn(0.0, 1.0)

                val coveProgress = ((dOvoid - 0.70) / 0.18).coerceIn(0.0, 1.0)
                val coveRamp     = (4.0 * coveProgress * coveProgress).toInt()

                val floorY       = BASE_EDGE_FLOOR_Y - (FLOOR_MAX_DIP.toDouble() * bowlRatio * bowlRatio).toInt() + floorReliefN.toInt() + coveRamp

                // ── A. COQUE D'ISOLATION HERMÉTIQUE IMPÉNÉTRABLE (dOvoid >= 0.88) ─────────────
                if (dOvoid >= 0.88) {
                    for (wy in (BEDROCK_SAFE_Y + 2)..10) {
                        val pos = BlockPos(wx, wy, wz)
                        val shellBlock = getStrataBlock(wx, wy, wz, isBaseShell = true, defaultMat = GABBRO_STONE)
                        world.setBlock(pos, shellBlock, PLACE_FLAG)
                        placedAny = true
                    }
                    continue
                }

                // ── B. INTÉRIEUR ET PAROIS ARQUÉES DE LA MEGA CATHÉDRALE (dOvoid < 0.88) ──────
                val wallRatio  = (dOvoid / 0.88).coerceIn(0.0, 1.0)
                val hRatio     = sqrt((1.0 - wallRatio * wallRatio).coerceAtLeast(0.0))
                val ceilRipple = 3.5 * sin(wx * 0.08 + phi1) * cos(wz * 0.08 + phi2) + 2.5 * cos((wx + wz) * 0.14 + phi3)
                val wallLedge  = if (dOvoid > 0.65) (2.0 * sin(theta * 4.0 + phi1) * (dOvoid - 0.65) / 0.23) else 0.0

                val clearance  = ((CAVE_MAX_HEIGHT * hRatio) + ceilRipple + wallLedge).toInt().coerceAtLeast(1)
                val ceilingY   = floorY + clearance
                val midY       = (floorY + ceilingY) / 2.0
                val halfH      = (ceilingY - floorY) / 2.0

                // ── ÉVALUATION VORONOI 3x3 ──────────────────────────────────────────────
                val vCellX = Math.floorDiv(wx, VORONOI_CELL_SIZE)
                val vCellZ = Math.floorDiv(wz, VORONOI_CELL_SIZE)

                var bestDistSq = Double.MAX_VALUE
                var bestCandidate: SpeleoCandidate? = null

                for (ncx in (vCellX - 1)..(vCellX + 1)) {
                    for (ncz in (vCellZ - 1)..(vCellZ + 1)) {
                        val pSeed = ncx.toLong() * 73856093L xor ncz.toLong() * 19349663L xor domeSeed
                        val exists = hash1D(pSeed) >= 0.38

                        if (exists) {
                            val rAngle = hash1D(pSeed xor 0x1111L) * 6.2831853
                            val rDist  = hash1D(pSeed xor 0x2222L) * 16.0
                            val px     = ncx * VORONOI_CELL_SIZE + (VORONOI_CELL_SIZE / 2.0) + cos(rAngle) * rDist
                            val pz     = ncz * VORONOI_CELL_SIZE + (VORONOI_CELL_SIZE / 2.0) + sin(rAngle) * rDist

                            val pdx = (wx - px)
                            val pdz = (wz - pz)
                            val distSq = pdx * pdx + pdz * pdz

                            if (distSq < bestDistSq) {
                                bestDistSq = distSq
                                bestCandidate = SpeleoCandidate(px, pz, pSeed, distSq)
                            }
                        }
                    }
                }

                val candidate = bestCandidate
                val pSeed     = candidate?.pSeed ?: 0L
                val hasPillar = candidate != null
                val px        = candidate?.px ?: 0.0
                val pz        = candidate?.pz ?: 0.0

                // Propriétés Géologiques du Premier Pilier
                val typeVal   = hash1D(pSeed xor 0x8888L)
                val tiltPhase = hash1D(pSeed xor 0x9999L) * 6.2831853

                val isTitan      = typeVal < 0.15
                val isStandard   = typeVal >= 0.15 && typeVal < 0.65
                val isNeedle     = typeVal >= 0.65 && typeVal < 0.79
                val isStalagmite = typeVal >= 0.79 && typeVal < 0.89   // 10% stalagmites du sol
                val isStalactite = typeVal >= 0.89                     // 11% stalactites géantes du plafond

                val baseR = when {
                    isTitan      -> 10.0 + hash1D(pSeed xor 0x1A1AL) * 4.0
                    isStandard   -> 6.5  + hash1D(pSeed xor 0x2B2BL) * 3.0
                    isNeedle     -> 3.5  + hash1D(pSeed xor 0x3C3CL) * 1.5
                    isStalagmite -> 3.5  + hash1D(pSeed xor 0x4D4DL) * 2.0
                    else         -> 0.0
                }

                val neckR = when {
                    isTitan      -> 5.0  + hash1D(pSeed xor 0x5E5EL) * 2.0
                    isStandard   -> 2.5  + hash1D(pSeed xor 0x6F6FL) * 1.5
                    isNeedle     -> 1.2  + hash1D(pSeed xor 0x7070L) * 0.8
                    else         -> 0.5
                }

                val topR = when {
                    isTitan      -> 8.0  + hash1D(pSeed xor 0x8181L) * 4.0
                    isStandard   -> 5.5  + hash1D(pSeed xor 0x9292L) * 2.5
                    isNeedle     -> 2.8  + hash1D(pSeed xor 0xA3A3L) * 1.5
                    isStalactite -> 3.5  + hash1D(pSeed xor 0xB4B4L) * 2.0
                    else         -> 0.0
                }

                val speleoHeight = (clearance * (0.50 + hash1D(pSeed xor 0xC5C5L) * 0.25)).toInt()
                val baseFlareMax = if (!isStalactite) 3.5 + hash1D(pSeed xor 0xEEEE1L) * 1.5 else 0.0
                val topFlareMax  = if (!isStalagmite) 3.5 + hash1D(pSeed xor 0xEEEE2L) * 1.5 else 0.0

                // 1. FONDATION SOL SOLIDE DE 5 BLOCS
                val floorBedMinY = (floorY - 5).coerceAtLeast(BEDROCK_SAFE_Y + 1)
                for (bwy in floorBedMinY until floorY) {
                    val bpos = BlockPos(wx, bwy, wz)
                    val bedBlock = getStrataBlock(wx, bwy, wz, isBaseShell = false, defaultMat = SHIVERING_SHALE)
                    world.setBlock(bpos, bedBlock, PLACE_FLAG)
                    placedAny = true
                }

                // 2. Traitement vertical de la colonne (floorY à ceilingY + 10)
                val topRoofY = (ceilingY + 10).coerceAtMost(10)

                for (wy in floorY..topRoofY) {
                    if (wy < BEDROCK_SAFE_Y) continue // Protection bedrock

                    val pos = BlockPos(wx, wy, wz)

                    if (wy >= ceilingY) {
                        // TOIT MASSIF STRATIFIÉ EN GABBRO (10 BLOCS DE HAUT)
                        val roofBlock = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = GABBRO_STONE)
                        world.setBlock(pos, roofBlock, PLACE_FLAG)
                        placedAny = true
                    } else if (wy == floorY) {
                        // SURFACE DU SOL : LAC DE BLUE ICE AU CENTRE ET STRATES LOCALISÉES EN BORDURE
                        val curPx = px + sin((wy - floorY) * 0.12 + tiltPhase) * 1.5
                        val curPz = pz + cos((wy - floorY) * 0.12 + tiltPhase) * 1.5
                        val pdx = (wx - curPx)
                        val pdz = (wz - curPz)
                        val effectiveBaseR = baseR + baseFlareMax
                        val isPillarFoot = hasPillar && !isStalactite && (pdx * pdx + pdz * pdz <= effectiveBaseR * effectiveBaseR)

                        if (isPillarFoot) {
                            val footBlock = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = GABBRO_STONE)
                            world.setBlock(pos, footBlock, PLACE_FLAG)
                        } else {
                            val lakeThreshold = 0.25 + (floorReliefN * 0.03)

                            val floorBlock = if (dOvoid < lakeThreshold) {
                                BLUE_ICE
                            } else {
                                val defaultBase = if (dOvoid >= 0.80) GABBRO_STONE else SHIVERING_SHALE
                                val baseMat = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = defaultBase)
                                val isLakeEdge = (dOvoid < lakeThreshold + 0.04)
                                when {
                                    isLakeEdge && baseMat == GABBRO_STONE -> BLUE_ICE_SHEET_GABBRO_STONE
                                    isLakeEdge && baseMat == SHIVERING_SHALE -> BLUE_ICE_SHEET_SHIVERING_SHALE_STONE
                                    else -> baseMat
                                }
                            }
                            world.setBlock(pos, floorBlock, PLACE_FLAG)
                        }
                        placedAny = true
                    } else {
                        // INTÉRIEUR DE LA CATHÉDRALE (wy > floorY et wy < ceilingY)
                        val relHeight = (wy - floorY).toDouble() / clearance.toDouble()
                        val curPx = px + sin(relHeight * 3.14 + tiltPhase) * 1.5
                        val curPz = pz + cos(relHeight * 3.14 + tiltPhase) * 1.5

                        val pdx = (wx - curPx)
                        val pdz = (wz - curPz)
                        val pDistSq = pdx * pdx + pdz * pdz

                        // ── CALCUL DU RAYON BRUT ISOLÉ ─────────────────────────────────────────
                        val rawR = when {
                            isStalagmite -> {
                                val stgLimitY = floorY + speleoHeight
                                if (wy in floorY..stgLimitY) {
                                    val stgProgress = (wy - floorY).toDouble() / speleoHeight.toDouble()
                                    baseR * (1.0 - stgProgress * stgProgress)
                                } else 0.0
                            }
                            isStalactite -> {
                                val stcLimitY = ceilingY - speleoHeight
                                if (wy in stcLimitY..ceilingY) {
                                    val stcProgress = (ceilingY - wy).toDouble() / speleoHeight.toDouble()
                                    topR * (1.0 - stcProgress * stcProgress)
                                } else 0.0
                            }
                            else -> {
                                val ny = if (halfH > 0) (wy - midY) / halfH else 0.0
                                if (ny <= 0) neckR + (baseR - neckR) * (ny * ny) else neckR + (topR - neckR) * (ny * ny)
                            }
                        }

                        // Évasement cubique au sol
                        val floorDist = ((wy - floorY).toDouble() / 5.0).coerceIn(0.0, 1.0)
                        val baseMorph = 1.0 - floorDist
                        val baseFlare = if (!isStalactite && rawR > 0.0) baseFlareMax * (baseMorph * baseMorph * baseMorph) else 0.0

                        // Évasement cubique au plafond
                        val ceilDist = ((ceilingY - wy).toDouble() / 5.0).coerceIn(0.0, 1.0)
                        val topMorph = 1.0 - ceilDist
                        val topFlare = if (!isStalagmite && rawR > 0.0) topFlareMax * (topMorph * topMorph * topMorph) else 0.0

                        val finalR = rawR + baseFlare + topFlare
                        val isPillar = hasPillar && (finalR > 0.0) && (pDistSq <= finalR * finalR)

                        if (isPillar) {
                            val pillarMat = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = GABBRO_STONE)
                            world.setBlock(pos, pillarMat, PLACE_FLAG)
                            placedAny = true
                        } else {
                            // Creuser le vide de la cathédrale
                            world.setBlock(pos, AIR, PLACE_FLAG)
                            placedAny = true
                        }
                    }
                }

                // Décoration : Stalactites de glace courtes au plafond (1 à 3 blocs)
                val isPillarCeil = hasPillar && !isStalagmite && (topR > 0.0)

                if (!isPillarCeil && clearance > 6) {
                    val icicleN = hash2D(wx, wz, 505L)
                    if (icicleN < ICICLE_CHANCE) {
                        val lenN = hash2D(wx, wz, 606L)
                        val length = (1 + (lenN * ICICLE_MAX_LEN).toInt()).coerceAtMost(ICICLE_MAX_LEN)

                        for (i in 1..length) {
                            val iy = ceilingY - i
                            if (iy <= floorY + 1) break

                            val pos = BlockPos(wx, iy, wz)
                            val block = if (i == length) ICE else PACKED_ICE
                            world.setBlock(pos, block, PLACE_FLAG)
                        }
                    }
                }
            }
        }

        return placedAny
    }

    // ── GÉNÉRATEUR D'AFFLEUREMENTS DE STRATES DIAGONALES LOCALISÉES ───────────────────────

    private fun getStrataBlock(wx: Int, wy: Int, wz: Int, isBaseShell: Boolean, defaultMat: BlockState): BlockState {
        if (isBaseShell) return GABBRO_STONE

        // Masque de présence 3D des affleurements de strates (ne couvre que ~30% de la cave)
        val strataMask = sin(wx * 0.035) * cos(wz * 0.035) + 0.5 * sin((wx + wy) * 0.06)

        // Hors des poches d'affleurements : retourner le matériau de base pur (Shivering Shale ou Gabbro)
        if (strataMask <= 0.38) {
            return defaultMat
        }

        // Dans les poches d'affleurements : tracer les strates diagonales à 45° sans bismuth
        val strataVal = wx + (wy * 1.5) + (wz * 0.8) + 6.0 * sin(wx * 0.04 + wz * 0.04)
        val layerMod  = Math.floorMod(strataVal.toInt(), 24)

        val rawState = when (layerMod) {
            in 0..6   -> GABBRO_STONE
            in 7..8   -> BLUE_ICE
            in 9..15  -> SHIVERING_SHALE
            in 16..20 -> BLUE_FROZEN_FLYSCH
            else      -> FROZEN_FLYSCH // Zéro minerai de bismuth !
        }

        // Blocs de transition aux contacts de la glace bleue dans les strates
        if (rawState == BLUE_ICE) {
            val neighborMat = if (layerMod == 7) GABBRO_STONE else SHIVERING_SHALE
            return if (neighborMat == GABBRO_STONE) BLUE_ICE_SHEET_GABBRO_STONE else BLUE_ICE_SHEET_SHIVERING_SHALE_STONE
        }

        return rawState
    }

    // ── Fonctions de Hachage Déterministes ────────────────────────────────────

    private fun hash2D(x: Int, z: Int, seed: Long): Double {
        var n = x.toLong() * 1619L + z.toLong() * 31337L + seed
        n = n xor (n shl 13)
        n = n * (n * n * 15731L + 789221L) + 1376312589L
        return (n and 0x7FFFFFFFL).toDouble() / 0x7FFFFFFFL.toDouble()
    }

    private fun hash1D(seed: Long): Double {
        var n = seed
        n = n xor (n shl 21)
        n = n xor (n ushr 35)
        n = n xor (n shl 4)
        return (n and 0x7FFFFFFFL).toDouble() / 0x7FFFFFFFL.toDouble()
    }
}
