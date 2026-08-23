package com.howlite.cryoawakening.world

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * AncientLilacTreeGenerator
 *
 * Générateur procédural de l'arbre Ancient Lilac avec :
 * - Grosses branches courbées visibles sous la canopée
 * - Lobes de trèfle à 3 ou 4 feuilles organiques (non géométriques) avec échancrure en cœur
 * - Feuilles retombantes naturelles
 */
object AncientLilacTreeGenerator {

    fun generate(level: ServerLevel, origin: BlockPos, random: RandomSource): Boolean {
        val trunkHeight = random.nextIntBetweenInclusive(5, 7)

        // 1. Vérification de l'espace disponible
        for (y in 1..trunkHeight + 4) {
            val checkRadius = if (y < trunkHeight - 2) 1 else 7
            for (dx in -checkRadius..checkRadius) {
                for (dz in -checkRadius..checkRadius) {
                    val checkPos = origin.offset(dx, y, dz)
                    val state = level.getBlockState(checkPos)
                    if (!state.isAir && !state.`is`(ModBlocks.ANCIENT_LILAC_LEAVES) && !state.`is`(ModBlocks.ANCIENT_LILAC_SAPLING)) {
                        if (!state.canBeReplaced()) return false
                    }
                }
            }
        }

        // 2. Décision Trèfle à 3 lobes (70%) ou 4 lobes (30%)
        val isFourLeaf = random.nextFloat() < 0.30f
        val lobeCount = if (isFourLeaf) 4 else 3
        val baseAngleOffset = random.nextFloat() * 360.0f

        // Retirer la sapling
        level.removeBlock(origin, false)

        // 3. Tronc central avec contreforts au sol
        for (y in 0 until trunkHeight) {
            setLog(level, origin.above(y), Direction.Axis.Y)
        }
        for (dir in Direction.Plane.HORIZONTAL) {
            val rootPos = origin.relative(dir)
            if (level.getBlockState(rootPos).canBeReplaced()) {
                setLog(level, rootPos, Direction.Axis.Y)
            }
        }

        val canopyBaseY = (trunkHeight - 2).coerceAtLeast(2)
        val lobeDistance = 4.8 // Distance horizontale du centre du lobe
        val leavesState = ModBlocks.ANCIENT_LILAC_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false)

        // 4. Génération des branches visibles en dessous ET des lobes de feuilles organiques
        for (i in 0 until lobeCount) {
            val angleDeg = baseAngleOffset + (i.toFloat() * (360.0f / lobeCount))
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val dirX = cos(angleRad)
            val dirZ = sin(angleRad)

            // Vecteur perpendiculaire pour scinder en forme de cœur de trèfle (2 sous-lobes par feuille)
            val perpX = -dirZ
            val perpZ = dirX

            val branchStartPos = origin.above(canopyBaseY - 1)
            val lobeCenterY = origin.y + trunkHeight + random.nextIntBetweenInclusive(0, 1)
            val lobeCenterX = origin.x + 0.5 + dirX * lobeDistance
            val lobeCenterZ = origin.z + 0.5 + dirZ * lobeDistance

            // --- A. Branche visible courbée sous le lobe ---
            // 1. Branche principale s'élevant diagonalement depuis le tronc vers le dessous du lobe
            val branchSteps = 6
            for (step in 1..branchSteps) {
                val t = step.toDouble() / branchSteps.toDouble()
                val bx = (origin.x + 0.5 + dirX * (lobeDistance * 0.75) * t).toInt()
                val by = (branchStartPos.y + (lobeCenterY - 1.5 - branchStartPos.y) * (t * t)).toInt()
                val bz = (origin.z + 0.5 + dirZ * (lobeDistance * 0.75) * t).toInt()
                val axis = if (abs(dirX) > abs(dirZ)) Direction.Axis.X else Direction.Axis.Z
                setLog(level, BlockPos(bx, by, bz), axis)
            }

            // 2. Ramifications secondaires sous la feuille
            for (forkDir in listOf(-1.0, 1.0)) {
                val forkSteps = 3
                for (f in 1..forkSteps) {
                    val ft = f.toDouble() / forkSteps.toDouble()
                    val fx = (origin.x + 0.5 + dirX * (lobeDistance * 0.75) + (dirX * 1.5 + perpX * forkDir * 1.5) * ft).toInt()
                    val fy = (lobeCenterY - 1.5 + (0.5 * ft)).toInt()
                    val fz = (origin.z + 0.5 + dirZ * (lobeDistance * 0.75) + (dirZ * 1.5 + perpZ * forkDir * 1.5) * ft).toInt()
                    val axis = if (abs(perpX) > abs(perpZ)) Direction.Axis.X else Direction.Axis.Z
                    setLog(level, BlockPos(fx, fy, fz), axis)
                }
            }

            // --- B. Canopée du Lobe en Forme de Feuille de Trèfle (2 sous-lobes bombés formant un cœur) ---
            val subLobeCenters = listOf(
                // Sous-lobe gauche
                Vec3(lobeCenterX + perpX * 1.6 + dirX * 0.3, lobeCenterY.toDouble(), lobeCenterZ + perpZ * 1.6 + dirZ * 0.3),
                // Sous-lobe droit
                Vec3(lobeCenterX - perpX * 1.6 + dirX * 0.3, lobeCenterY.toDouble(), lobeCenterZ - perpZ * 1.6 + dirZ * 0.3),
                // Corps central reliant au tronc
                Vec3(lobeCenterX - dirX * 1.5, lobeCenterY - 0.3, lobeCenterZ - dirZ * 1.5)
            )

            // Générer chaque sous-lobe organique
            for ((idx, subCenter) in subLobeCenters.withIndex()) {
                val subRadiusH = if (idx == 2) 2.2 else 2.5
                val subRadiusV = 1.6

                val rH = subRadiusH.toInt() + 1
                val rV = subRadiusV.toInt() + 1

                for (dx in -rH..rH) {
                    for (dy in -rV..rV) {
                        for (dz in -rH..rH) {
                            val lx = (subCenter.x + dx).toInt()
                            val ly = (subCenter.y + dy).toInt()
                            val lz = (subCenter.z + dz).toInt()

                            // Bruit subtil sur les bords pour casser le côté sphérique rigide
                            val noise = (random.nextFloat() * 0.25f) - 0.12f
                            val distSq = (dx * dx) / ((subRadiusH + noise) * (subRadiusH + noise)) +
                                         (dy * dy) / (subRadiusV * subRadiusV) +
                                         (dz * dz) / ((subRadiusH + noise) * (subRadiusH + noise))

                            if (distSq <= 1.0) {
                                setLeaf(level, BlockPos(lx, ly, lz), leavesState)

                                // Feuilles retombantes organiques sur le pourtour inférieur
                                if (dy == -rV + 1 && random.nextFloat() < 0.22f) {
                                    setLeaf(level, BlockPos(lx, ly - 1, lz), leavesState)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Cœur central du trèfle (sommet du tronc connecté)
        val centerTop = origin.above(trunkHeight)
        for (dx in -2..2) {
            for (dy in -1..2) {
                for (dz in -2..2) {
                    val distSq = dx * dx + dz * dz + (dy * dy * 1.4)
                    if (distSq <= 4.5 + (random.nextFloat() * 0.5f)) {
                        setLeaf(level, centerTop.offset(dx, dy, dz), leavesState)
                    }
                }
            }
        }

        return true
    }

    private fun setLog(level: ServerLevel, pos: BlockPos, axis: Direction.Axis) {
        val current = level.getBlockState(pos)
        if (current.isAir || current.canBeReplaced() || current.`is`(ModBlocks.ANCIENT_LILAC_LEAVES) || current.`is`(ModBlocks.ANCIENT_LILAC_SAPLING)) {
            val logState = ModBlocks.ANCIENT_LILAC_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis)
            level.setBlock(pos, logState, 3)
        }
    }

    private fun setLeaf(level: ServerLevel, pos: BlockPos, state: BlockState) {
        val current = level.getBlockState(pos)
        if (current.isAir || current.canBeReplaced()) {
            level.setBlock(pos, state, 3)
        }
    }
}
