package com.howlite.cryoawakening.worldgen.feature

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.worldgen.CryoWorldGenConfig
import com.howlite.cryoawakening.worldgen.biome.ModBiomes
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
 * PillaredIceCaveFeature — Engine de Cathédrale de Glace (Gours : Bassins Rimstone en Gradins V26)
 *
 * Spécifications V26 :
 *  1. GOURS — BASSINS RIMSTONE GÉOLOGIQUES EN GRADINS :
 *     Génération de 10–15 bassins organiques en gradins (« Gours ») sur le sol de la cathédrale.
 *     - 1 à 3 niveaux de terrasses successives (rimstone dams).
 *     - Bords en Shivering Shale / Frozen Flysch / Blue Frozen Flysch selon la profondeur.
 *     - Piscines intérieures remplies de Blue Ice (cohérent avec le thème de la cave).
 *     - Formes organiques irrégulières grâce à la déformation 2D harmonique (blobs naturels).
 *
 *  2. VEINES DE BLUE ICE DANS LES STRATES DIAGONALES (15% COUVERTURE) :
 *     2 veines majeures de Blue Ice flanquées de blocs de transition automatiques.
 *
 *  3. MICRO-LAMES 3D AU SOL ET AU PLAFOND (LIMITE STRICTE 15%) :
 *     strataMask > 0.65 uniquement, +1 à +2 blocs au sol, -1 à -2 blocs au plafond.
 *
 *  4. BLOCS DE TRANSITION & ISOLATION IMPÉNÉTRABLE :
 *     BLUE_ICE_SHEET_GABBRO_STONE / SHIVERING_SHALE_STONE aux contacts de la blue ice.
 */
class PillaredIceCaveFeature(codec: Codec<NoneFeatureConfiguration>) :
    Feature<NoneFeatureConfiguration>(codec) {

    companion object {
        private const val CAVE_GRID_SPACING = CryoWorldGenConfig.CAVE_GRID_SPACING
        private const val VORONOI_CELL_SIZE = 36    // Cellule Voronoi spéléothèmes (36 blocs)
        private const val GOUR_CELL_SIZE    = 42    // Cellule Voronoi Gours (42 blocs)

        private const val BASE_EDGE_FLOOR_Y = CryoWorldGenConfig.BASE_EDGE_FLOOR_Y
        private const val FLOOR_MAX_DIP     = CryoWorldGenConfig.FLOOR_MAX_DIP
        private const val CAVE_MAX_HEIGHT   = CryoWorldGenConfig.CAVE_MAX_HEIGHT
        private const val BEDROCK_SAFE_Y    = CryoWorldGenConfig.BEDROCK_SAFE_Y

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

    // ── Structures de données internes ────────────────────────────────────────

    private data class SpeleoCandidate(
        val px: Double,
        val pz: Double,
        val pSeed: Long,
        val distSq: Double
    )

    /**
     * Un bassin Gour individuel — piscine unique avec son propre niveau d'altitude.
     * Plusieurs bassins forment une "chaîne en cascade" depuis la paroi vers le centre.
     *
     * Chaque bassin est une baignoire indépendante :
     *   ◼ Rebord rim (+2 blocs au-dessus de elevationBase)
     *   ─ Sol de la piscine  (elevationBase - 1)
     *   ❄ Centre Blue Ice   (elevationBase - 1, matiere Blue Ice)
     */
    private data class GourPool(
        val cx: Double,            // Centre X (coordonnées monde)
        val cz: Double,            // Centre Z (coordonnées monde)
        val radius: Double,        // Rayon de la piscine (5–9 blocs)
        val elevationBase: Int,    // Hauteur de base de CETTE piscine au-dessus du floorY : 4 (paroi) → 0 (centre)
        val bseed: Long            // Graine déterministe unique
    )

    // ── Entrée Principale de Génération ──────────────────────────────────────

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
        val domeSeed = CryoWorldGenConfig.getDomeSeed(gridX, gridZ)
        val (centerX, centerZ) = CryoWorldGenConfig.getCaveCenter(gridX, gridZ)

        // Paramètres de déformation angulaire unique pour CETTE caverne
        val phi1 = hash1D(domeSeed xor 0x7A8BL) * 6.2831853
        val phi2 = hash1D(domeSeed xor 0x9C0DL) * 6.2831853
        val phi3 = hash1D(domeSeed xor 0xB41FL) * 6.2831853

        val amp1 = 0.12 + hash1D(domeSeed xor 0x1111L) * 0.08
        val amp2 = 0.08 + hash1D(domeSeed xor 0x2222L) * 0.06
        val amp3 = 0.04 + hash1D(domeSeed xor 0x3333L) * 0.04

        val (radX, radZ) = CryoWorldGenConfig.getCaveRadii(domeSeed)

        // Quick distance check (max radius ~105 blocks * 1.35 max deformation + 16 chunk margin = 158 blocks)
        val cdx = (chunkMidX - centerX).toDouble()
        val cdz = (chunkMidZ - centerZ).toDouble()
        if (cdx * cdx + cdz * cdz > 165.0 * 165.0) return false

        // ── PRÉ-CALCUL DES GOURS POUR CETTE CAVERNE (DÉTERMINISTE PAR domeSeed) ──────────────
        val gourBasins = buildGourBasins(centerX, centerZ, domeSeed, radX, radZ)

        var placedAny = false

        for (wx in cxMin..cxMax) {
            for (wz in czMin..czMax) {

                val dx = (wx - centerX) / radX
                val dz = (wz - centerZ) / radZ
                val dOvoidSq = dx * dx + dz * dz

                val theta = atan2(dz, dx)
                val harmonicDeform = 1.0 + amp1 * sin(2.0 * theta + phi1) + amp2 * cos(3.0 * theta + phi2) + amp3 * sin(5.0 * theta + phi3)
                val dOvoid = sqrt(dOvoidSq) / harmonicDeform

                if (dOvoid > 1.25) continue

                // ── RELIEF DU SOL, RAMPE PARABOLIQUE & GOURS & LAMES 3D ─────────────────────
                val floorReliefN = 1.8 * sin(wx * 0.06 + phi1) * cos(wz * 0.06 + phi2) + 1.2 * cos((wx - wz) * 0.10 + phi3)
                val bowlRatio    = (1.0 - (dOvoid / 0.88)).coerceIn(0.0, 1.0)

                val coveProgress = ((dOvoid - 0.70) / 0.18).coerceIn(0.0, 1.0)
                val coveRamp     = (4.0 * coveProgress * coveProgress).toInt()

                // Micro-lames 3D au sol (Stricte limite 15%)
                val floorBladeH  = getDiagonalBladeHeight(wx, BASE_EDGE_FLOOR_Y, wz)

                // ── GOUR : décalage hauteur + matériau de surface ───────────────────────────
                val (gourDelta, gourSurface) = getGourEffect(wx, wz, gourBasins)

                // floorY intègre : dip central + relief naturel + rampe paroi + lames 3D + gour
                val floorY = BASE_EDGE_FLOOR_Y - (FLOOR_MAX_DIP.toDouble() * bowlRatio * bowlRatio).toInt() +
                             floorReliefN.toInt() + coveRamp + floorBladeH + gourDelta

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

                // Micro-lames suspendues au plafond (Stricte limite 15%)
                val ceilBladeH = getDiagonalBladeHeight(wx, BASE_EDGE_FLOOR_Y + CAVE_MAX_HEIGHT, wz)

                val clearance  = ((CAVE_MAX_HEIGHT * hRatio) + ceilRipple + wallLedge - ceilBladeH).toInt().coerceAtLeast(1)
                val ceilingY   = floorY + clearance
                val midY       = (floorY + ceilingY) / 2.0
                val halfH      = (ceilingY - floorY) / 2.0

                // ── ÉVALUATION VORONOI SPÉLÉOTHÈMES 3x3 ─────────────────────────────────────
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

                val typeVal   = hash1D(pSeed xor 0x8888L)
                val tiltPhase = hash1D(pSeed xor 0x9999L) * 6.2831853

                val isTitan      = typeVal < 0.15
                val isStandard   = typeVal >= 0.15 && typeVal < 0.65
                val isNeedle     = typeVal >= 0.65 && typeVal < 0.79
                val isStalagmite = typeVal >= 0.79 && typeVal < 0.89
                val isStalactite = typeVal >= 0.89

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
                    if (wy < BEDROCK_SAFE_Y) continue

                    val pos = BlockPos(wx, wy, wz)

                    if (wy >= ceilingY) {
                        // TOIT MASSIF STRATIFIÉ (10 BLOCS)
                        val roofBlock = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = GABBRO_STONE)
                        world.setBlock(pos, roofBlock, PLACE_FLAG)
                        placedAny = true
                    } else if (wy == floorY) {
                        // SURFACE DU SOL : Gour → Blue Ice pool ; sinon lac central ou roche
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
                            val surfaceBlock: BlockState = when {
                                // 1. Gour : piscine centrale → fond solide (eau+glace seront ajoutées au-dessus)
                                gourSurface == BLUE_ICE -> SHIVERING_SHALE

                                // 2. Gour : rebord rimstone ou sol de terrasse → bloc imposé
                                gourSurface != null -> gourSurface

                                // 3. Lac de Blue Ice central
                                dOvoid < (0.25 + floorReliefN * 0.03) -> BLUE_ICE

                                // 3. Bordure du lac (blocs de transition)
                                else -> {
                                    val defaultBase = if (dOvoid >= 0.80) GABBRO_STONE else SHIVERING_SHALE
                                    val baseMat = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = defaultBase)
                                    val lakeEdge = dOvoid < (0.25 + floorReliefN * 0.03) + 0.04
                                    when {
                                        lakeEdge && baseMat == GABBRO_STONE    -> BLUE_ICE_SHEET_GABBRO_STONE
                                        lakeEdge && baseMat == SHIVERING_SHALE -> BLUE_ICE_SHEET_SHIVERING_SHALE_STONE
                                        else                                    -> baseMat
                                    }
                                }
                            }
                            world.setBlock(pos, surfaceBlock, PLACE_FLAG)
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

                        val floorDist = ((wy - floorY).toDouble() / 5.0).coerceIn(0.0, 1.0)
                        val baseMorph = 1.0 - floorDist
                        val baseFlare = if (!isStalactite && rawR > 0.0) baseFlareMax * (baseMorph * baseMorph * baseMorph) else 0.0

                        val ceilDist = ((ceilingY - wy).toDouble() / 5.0).coerceIn(0.0, 1.0)
                        val topMorph = 1.0 - ceilDist
                        val topFlare = if (!isStalagmite && rawR > 0.0) topFlareMax * (topMorph * topMorph * topMorph) else 0.0

                        val finalR = rawR + baseFlare + topFlare
                        val isPillar = hasPillar && (finalR > 0.0) && (pDistSq <= finalR * finalR)

                        if (isPillar) {
                            val pillarMat = getStrataBlock(wx, wy, wz, isBaseShell = false, defaultMat = GABBRO_STONE)
                            world.setBlock(pos, pillarMat, PLACE_FLAG)
                            placedAny = true
                        } else if (gourSurface == BLUE_ICE && wy == floorY + 1) {
                            // COUCHE EAU GELÉE : Packed Ice (=eau solide gelée dans une cave de glace)
                            world.setBlock(pos, PACKED_ICE, PLACE_FLAG)
                            placedAny = true
                        } else if (gourSurface == BLUE_ICE && wy == floorY + 2) {
                            // SURFACE DE GLACE TRANSLUCIDE : Ice sur le dessus de la piscine
                            world.setBlock(pos, ICE, PLACE_FLAG)
                            placedAny = true
                        } else {
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
                            val ipos = BlockPos(wx, iy, wz)
                            val block = if (i == length) ICE else PACKED_ICE
                            world.setBlock(ipos, block, PLACE_FLAG)
                        }
                    }
                }
            }
        }

        if (placedAny) {
            CryoAwakening.LOGGER.info("[CryoAwakening] Successfully generated PillaredIceCave slice in chunk [${chunkX}, ${chunkZ}] (Cave center: [$centerX, $centerZ], Y: ${BASE_EDGE_FLOOR_Y})")
        }

        return placedAny
    }

    // ── GOURS : CASCADES DE BASSINS RIMSTONE (CHAQUE BASSIN = PISCINE INDIVIDUELLE) ────────────────

    /**
     * Génère les chaînes de bassins Gour en cascade (déterministe par domeSeed).
     *
     * Chaque chaîne = 3–5 bassins INDIVIDUELS alignés depuis la paroi vers le centre.
     * Chaque bassin est plus bas que le précédent d'1–2 blocs (elevationBase décroissant).
     * Le débordement d'un bassin dans le suivant se crée automatiquement car :
     *   bord_haut_N = elevationBase_N + 2
     *   sol_N+1     = elevationBase_N+1 - 1
     *   Si bord_haut_N ≈ sol_N+1 → l'eau semble déborder exactement au niveau du rebord.
     */
    private fun buildGourBasins(centerX: Int, centerZ: Int, domeSeed: Long, radX: Double, radZ: Double): List<GourPool> {
        val allPools = mutableListOf<GourPool>()

        // Génère 5–8 chaînes de cascade autour de la cave
        val numCascades = 5 + (hash1D(domeSeed xor 0xFACEL) * 4.0).toInt()

        for (ci in 0 until numCascades) {
            val cseed = domeSeed xor (ci.toLong() * 837482139L)

            // Angle aléatoire autour de la paroi pour ancrer cette cascade
            val anchorAngle = hash1D(cseed xor 0x1234L) * 6.2831853

            // Point d'ancrage : très proche de la paroi de la cave (dOvoid ≈ 0.68–0.76)
            val anchorRadFrac = 0.68 + hash1D(cseed xor 0x5678L) * 0.08
            val anchorX = centerX + cos(anchorAngle) * radX * anchorRadFrac
            val anchorZ = centerZ + sin(anchorAngle) * radZ * anchorRadFrac

            // Direction d'ancrage vers le centre de la cave (vecteur unitaire)
            val dirRawX = (centerX - anchorX)
            val dirRawZ = (centerZ - anchorZ)
            val dirLen  = sqrt(dirRawX * dirRawX + dirRawZ * dirRawZ).coerceAtLeast(0.001)
            val ndirX   = dirRawX / dirLen
            val ndirZ   = dirRawZ / dirLen

            // Nombre de bassins dans cette cascade : 3–5 (chaque bassin = une marche)
            val numPools = 3 + (hash1D(cseed xor 0x9ABCL) * 3.0).toInt()

            // Longueur totale de la cascade : 20–40 blocs (du bord vers le centre)
            val cascadeSpan = 20.0 + hash1D(cseed xor 0xDEF0L) * 20.0

            // Altitude de la première piscine (la plus haute, collée à la paroi)
            val topElevation = 3 + (hash1D(cseed xor 0xEEEEL) * 3.0).toInt()  // 3–5 blocs au-dessus du sol

            for (pi in 0 until numPools) {
                val pseed    = cseed xor (pi.toLong() * 123456789L)
                val fraction = pi.toDouble() / numPools.toDouble()  // 0.0 = paroi, 1.0 = centre

                // Position le long de la chaîne + léger décalage latéral aléatoire
                val dist = fraction * cascadeSpan + (hash1D(pseed xor 0xAAAAL) - 0.5) * 2.0
                val px   = anchorX + ndirX * dist + (hash1D(pseed xor 0xBBBBL) - 0.5) * 5.0
                val pz   = anchorZ + ndirZ * dist + (hash1D(pseed xor 0xCCCCL) - 0.5) * 5.0

                // Rayon de cette piscine : 5–9 blocs (format bathtub indépendant)
                val radius = 5.0 + hash1D(pseed xor 0xDDDDL) * 4.0

                // Altitude de cette piscine : décroit de topElevation vers 0 de la paroi vers le centre
                // Décrement de 1–2 blocs par marche pour effet cascade naturel
                val stepDown   = (topElevation.toDouble() / numPools.toDouble())
                val elevBase   = (topElevation - pi * stepDown).toInt().coerceAtLeast(0)

                allPools.add(GourPool(px, pz, radius, elevBase, pseed))
            }
        }

        return allPools
    }

    /**
     * Calcule l'effet Gour sur la colonne (wx, wz).
     *
     * - À l'intérieur (nd < 1.0) : pool ou rebord, priorité à l'elevationBase le plus haut.
     * - Zone de pente extérieure (nd 1.0–1.30) : pente douce qui remonte vers le rebord
     *   pour un morphing naturel sol/bassin (pas de marche abrupte).
     */
    private fun getGourEffect(wx: Int, wz: Int, pools: List<GourPool>): Pair<Int, BlockState?> {
        var bestResult:   Pair<Int, BlockState?>? = null
        var bestElevPrio: Int = Int.MIN_VALUE

        for (pool in pools) {
            val dx      = (wx - pool.cx)
            val dz      = (wz - pool.cz)
            val rawDist = sqrt(dx * dx + dz * dz)

            // Rejet rapide (élargi pour inclure la zone de pente extérieure)
            if (rawDist > pool.radius * 1.45) continue

            // Déformation organique
            val angle     = atan2(dz, dx)
            val ws1       = hash1D(pool.bseed xor 0x5566L) * 6.2831853
            val ws2       = hash1D(pool.bseed xor 0x7788L) * 6.2831853
            val warp      = 1.0 + 0.22 * sin(3.0 * angle + ws1) + 0.14 * cos(5.0 * angle + ws2)
            val orgDist   = rawDist / warp
            val nd        = orgDist / pool.radius

            // Variation naturelle de la hauteur du rebord (1 ou 2 blocs selon l'angle)
            val rimVar = if (hash1D(pool.bseed xor ((angle * 53.0).toLong() and 0x0FFFFFFL)) < 0.50) 0 else 1

            if (nd < 1.0) {
                // ── INTÉRIEUR DU BASSIN : pool ou rebord rimstone ────────────────────────────
                if (pool.elevationBase > bestElevPrio) {
                    bestElevPrio = pool.elevationBase
                    bestResult   = computePoolEffect(nd, pool.elevationBase, rimVar)
                }
            } else if (nd < 1.30) {
                // ── ZONE DE PENTE EXTÉRIEURE (morphing sol → rebord) ───────────────────────
                // Pente quadratique : sol qui monte doucement vers le bord extérieur du bassin
                val slopeFrac = 1.0 - ((nd - 1.0) / 0.30)  // 1.0 au bord, 0.0 à nd=1.30
                val slopeH = ((slopeFrac * slopeFrac) * (pool.elevationBase * 0.5 + 0.5)).toInt().coerceAtLeast(0)
                if (slopeH > 0 && pool.elevationBase > bestElevPrio) {
                    bestElevPrio = pool.elevationBase
                    bestResult   = Pair(slopeH, null)  // matériau normal (sol de cave), juste surelevé
                }
            }
        }

        return bestResult ?: Pair(0, null)
    }

    /**
     * Calcule l'effet d'UNE piscine individuelle à la distance normalisée [nd].
     *
     * NOUVEAU : toute la surface intérieure (nd < 0.82) est signalée BLUE_ICE
     * → la boucle principale place :
     *   floorY     : Shivering Shale (fond solid)
     *   floorY + 1 : Packed Ice (couche de glace/eau gelée)
     *   floorY + 2 : Ice (surface de glace translucide)
     * Le bassin est ENTIER rempli de glace jusqu'au bord du rebord.
     *
     *  ◼ REBORD RIMSTONE  (nd ∈ [0.82..1.0]) : elevationBase + 1 + rimVar (1–2 blocs, organique)
     *  ❄ PISCINE GLACE    (nd ∈ [0.00..0.82]) : elevationBase - 1, signal BLUE_ICE = rempli glace
     */
    private fun computePoolEffect(nd: Double, elevationBase: Int, rimVar: Int = 0): Pair<Int, BlockState?> {
        val rimHeight  = elevationBase + 1 + rimVar  // 1–2 blocs de rebord
        val poolHeight = elevationBase - 1            // Sol de la cuvette (enfoncé de 1 bloc)
        return when {
            nd >= 0.82 -> Pair(rimHeight,  SHIVERING_SHALE)  // ◼ Rebord rimstone surélevé
            else       -> Pair(poolHeight, BLUE_ICE)          // ❄ Toute la piscine = glace (signal BLUE_ICE)
        }
    }

    // ── SOULÈVEMENT PHYSIQUE : LIMITE STRICTE DE 15% DE COUVERTURE ────────────────────────

    private fun getDiagonalBladeHeight(wx: Int, wy: Int, wz: Int): Int {
        val strataMask = sin(wx * 0.035) * cos(wz * 0.035) + 0.5 * sin((wx + wy) * 0.06)
        if (strataMask <= 0.65) return 0

        val warp = 8.0 * sin(wx * 0.03 + wz * 0.03) + 4.0 * cos(wx * 0.08 - wz * 0.05)
        val strataPos = (wx * 1.2 - wy * 0.8 + wz * 1.4) + warp
        val layer = Math.floorMod(strataPos.toInt(), 16)

        if (layer in 7..9) {
            val p = (layer - 7).toDouble() / 2.0
            return Math.round(1.0 + sin(p * 3.14159)).toInt()
        }

        return 0
    }

    // ── GÉNÉRATEUR D'AFFLEUREMENTS DE STRATES DIAGONALES ENRICHIES EN BLUE ICE (15% COVERAGE) ─────

    private fun getStrataBlock(wx: Int, wy: Int, wz: Int, isBaseShell: Boolean, defaultMat: BlockState): BlockState {
        if (isBaseShell) return GABBRO_STONE

        val strataMask = sin(wx * 0.035) * cos(wz * 0.035) + 0.5 * sin((wx + wy) * 0.06)
        if (strataMask <= 0.65) return defaultMat

        val warp = 8.0 * sin(wx * 0.03 + wz * 0.03) + 4.0 * cos(wx * 0.08 - wz * 0.05)
        val strataPos = wx + (wy * 1.5) + (wz * 0.8) + warp
        val layerMod  = Math.floorMod(strataPos.toInt(), 32)

        val rawState = when (layerMod) {
            in 0..5   -> GABBRO_STONE
            in 6..10  -> BLUE_ICE                // Veine 1 de Blue Ice (5 blocs)
            in 11..16 -> SHIVERING_SHALE
            in 17..21 -> BLUE_FROZEN_FLYSCH
            in 22..25 -> BLUE_ICE                // Veine 2 de Blue Ice (4 blocs)
            else      -> FROZEN_FLYSCH
        }

        if (rawState == BLUE_ICE) {
            val neighborMat = if (layerMod <= 10) GABBRO_STONE else SHIVERING_SHALE
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

    private fun hash1D(seed: Long): Double = CryoWorldGenConfig.hash1D(seed)
}
