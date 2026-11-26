package com.howlite.cryoawakening.infiltration

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import kotlin.math.floor

/**
 * Phases de la mécanique d'Infiltration (style Ascend de Zelda: Tears of the Kingdom).
 */
enum class InfiltrationPhase(val id: Int) {
    IDLE(0),
    AIMING(1),
    RISING_TO_CEILING(2),
    SWIMMING_THROUGH(3),
    PEEKING(4),
    EMERGING(5),
    RETURNING(6);

    fun isActiveSequence(): Boolean =
        this == RISING_TO_CEILING ||
            this == SWIMMING_THROUGH ||
            this == PEEKING ||
            this == EMERGING ||
            this == RETURNING

    fun isInsideBlocks(): Boolean =
        this == SWIMMING_THROUGH || this == RETURNING

    fun locksMovement(): Boolean = isActiveSequence()

    companion object {
        fun byId(id: Int): InfiltrationPhase = entries.firstOrNull { it.id == id } ?: IDLE
    }
}

/**
 * Raison d'invalidité lorsqu'un plafond ne peut pas être traversé.
 */
enum class InfiltrationInvalidReason {
    NONE,
    NO_CEILING,
    UNBREAKABLE_BLOCK,
    NO_SURFACE_ABOVE,
    OUT_OF_WORLD
}

/**
 * Résultat d'analyse d'une colonne de plafond au-dessus du joueur.
 */
data class InfiltrationTarget(
    val valid: Boolean,
    val reason: InfiltrationInvalidReason,
    val columnX: Double,
    val columnZ: Double,
    val ceilingHitY: Double,
    val exitFloorY: Double,
    val ceilingBlockPos: BlockPos?
) {
    val ceilingDistance: Double
        get() = ceilingHitY

    val thickness: Double
        get() = (exitFloorY - ceilingHitY).coerceAtLeast(0.0)
}

/**
 * Constantes et calculs partagés client/serveur pour détecter un plafond traversable
 * et calculer les durées fluides des animations.
 */
object InfiltrationMath {
    const val MAX_CEILING_REACH: Int = 28
    const val MAX_TRAVERSE_THICKNESS: Int = 48
    const val PEEK_HEAD_OFFSET: Double = 1.24 // player.y = exitFloorY - 1.24 -> yeux ~0.38 au-dessus du sol

    fun computeRiseTicks(startY: Double, ceilingHitY: Double): Int {
        val dist = (ceilingHitY - (startY + 1.2)).coerceAtLeast(0.0)
        return (10 + (dist * 1.15).toInt()).coerceIn(10, 26)
    }

    fun computeSwimTicks(ceilingHitY: Double, exitFloorY: Double): Int {
        val thickness = (exitFloorY - ceilingHitY).coerceAtLeast(1.0)
        return (16 + (thickness * 1.6).toInt()).coerceIn(20, 66)
    }

    fun computeReturnTicks(exitFloorY: Double, originY: Double): Int {
        val totalDist = (exitFloorY - originY).coerceAtLeast(1.0)
        return (16 + (totalDist * 0.9).toInt()).coerceIn(18, 44)
    }

    const val EMERGE_TICKS: Int = 12

    /**
     * Analyse la colonne au-dessus du joueur pour trouver le plafond exact
     * et la première surface libre de 2 blocs au-dessus.
     */
    fun findTarget(level: Level, player: Player): InfiltrationTarget {
        val blockX = floor(player.x).toInt()
        val blockZ = floor(player.z).toInt()
        val colX = blockX + 0.5
        val colZ = blockZ + 0.5

        val startScanY = floor(player.eyeY + 0.15).toInt()
        val maxCeilingScanY = minOf(startScanY + MAX_CEILING_REACH, level.maxY - 2)

        var ceilingPos: BlockPos? = null
        var ceilingBottomY = player.eyeY + MAX_CEILING_REACH.toDouble()

        // 1. Rechercher le premier bloc de plafond solide au-dessus de la tête du joueur
        for (y in startScanY..maxCeilingScanY) {
            val pos = BlockPos(blockX, y, blockZ)
            val state = level.getBlockState(pos)
            val shape = state.getCollisionShape(level, pos)
            if (!shape.isEmpty) {
                val minY = shape.min(net.minecraft.core.Direction.Axis.Y)
                val worldBottomY = y.toDouble() + minY
                if (worldBottomY >= player.eyeY - 0.05) {
                    ceilingPos = pos
                    ceilingBottomY = worldBottomY
                    break
                }
            }
        }

        if (ceilingPos == null) {
            return InfiltrationTarget(
                valid = false,
                reason = InfiltrationInvalidReason.NO_CEILING,
                columnX = colX,
                columnZ = colZ,
                ceilingHitY = (player.eyeY + 10.0).coerceAtMost(level.maxY.toDouble()),
                exitFloorY = player.y,
                ceilingBlockPos = null
            )
        }

        // Vérifier que le plafond lui-même n'est pas interdit
        val firstState = level.getBlockState(ceilingPos)
        if (isForbiddenBlock(firstState)) {
            return InfiltrationTarget(
                valid = false,
                reason = InfiltrationInvalidReason.UNBREAKABLE_BLOCK,
                columnX = colX,
                columnZ = colZ,
                ceilingHitY = ceilingBottomY,
                exitFloorY = ceilingBottomY,
                ceilingBlockPos = ceilingPos
            )
        }

        // 2. Scanner à travers la roche/glace pour trouver une sortie avec 2 blocs d'espace libre
        val maxTraverseY = minOf(ceilingPos.y + MAX_TRAVERSE_THICKNESS, level.maxY - 2)
        var y = ceilingPos.y
        while (y <= maxTraverseY) {
            val pos = BlockPos(blockX, y, blockZ)
            val state = level.getBlockState(pos)

            if (isForbiddenBlock(state)) {
                return InfiltrationTarget(
                    valid = false,
                    reason = InfiltrationInvalidReason.UNBREAKABLE_BLOCK,
                    columnX = colX,
                    columnZ = colZ,
                    ceilingHitY = ceilingBottomY,
                    exitFloorY = y.toDouble(),
                    ceilingBlockPos = ceilingPos
                )
            }

            // Candidat de surface : ce bloc a une collision (sol), et au-dessus c'est libre pour un joueur debout
            val floorShape = state.getCollisionShape(level, pos)
            if (!floorShape.isEmpty) {
                val surfaceY = y.toDouble() + floorShape.max(net.minecraft.core.Direction.Axis.Y)
                if (hasStandingRoom(level, colX, surfaceY, colZ)) {
                    return InfiltrationTarget(
                        valid = true,
                        reason = InfiltrationInvalidReason.NONE,
                        columnX = colX,
                        columnZ = colZ,
                        ceilingHitY = ceilingBottomY,
                        exitFloorY = surfaceY,
                        ceilingBlockPos = ceilingPos
                    )
                }
            }
            y++
        }

        val reason = if (ceilingPos.y + MAX_TRAVERSE_THICKNESS >= level.maxY - 2) {
            InfiltrationInvalidReason.OUT_OF_WORLD
        } else {
            InfiltrationInvalidReason.NO_SURFACE_ABOVE
        }

        return InfiltrationTarget(
            valid = false,
            reason = reason,
            columnX = colX,
            columnZ = colZ,
            ceilingHitY = ceilingBottomY,
            exitFloorY = ceilingBottomY,
            ceilingBlockPos = ceilingPos
        )
    }

    private fun isForbiddenBlock(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return state.`is`(Blocks.BEDROCK) || state.`is`(Blocks.BARRIER) || state.`is`(Blocks.END_PORTAL_FRAME)
    }

    /**
     * Vérifie que la hitbox standard d'un joueur (largeur 0.56, hauteur 1.8) ne collide avec aucun bloc à (x, y, z).
     */
    fun hasStandingRoom(level: Level, x: Double, y: Double, z: Double): Boolean {
        if (y + 1.85 > level.maxY) return false
        val halfWidth = 0.28
        val playerBox = AABB(
            x - halfWidth, y + 0.02, z - halfWidth,
            x + halfWidth, y + 1.78, z + halfWidth
        )
        return level.noCollision(playerBox)
    }
}
