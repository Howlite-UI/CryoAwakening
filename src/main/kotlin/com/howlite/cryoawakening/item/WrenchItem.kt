package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.block.GalePipeBlock
import com.howlite.cryoawakening.block.GaleTankBlock
import com.howlite.cryoawakening.block.PipeConnectionState
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Clé / Wrench multifonction :
 * - Clic Droit sur un Gale Pipe :
 *   - Si connecté à un autre tuyau : bascule entre Normal et Déconnecté (Fermé).
 *   - Si connecté à une machine (Gale Tank, Cryo Vent) : bascule entre Normal, Extraction (Pompe), et Déconnecté.
 * - Clic Droit sur un bloc orientable : fait pivoter le bloc (Gale Tank, Cryo Vent, etc.).
 * - Shift + Clic Droit sur une machine ou un tuyau : démantèlement instantané propre (Dismantle).
 */
class WrenchItem(properties: Properties) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS
        val state = level.getBlockState(pos)
        val hit = context.clickLocation

        // 1. Shift + Clic Droit : Démantèlement instantané (Dismantle)
        if (player.isShiftKeyDown) {
            if (isDismantlable(state)) {
                if (!level.isClientSide) {
                    dismantleBlock(level, pos, state, player, context.itemInHand)
                }
                return InteractionResult.SUCCESS
            }
        }

        // 2. Clic Droit sur un Gale Pipe : Modification du mode de connexion de la branche ciblée
        if (state.block is GalePipeBlock) {
            val targetDir = getTargetPipeDirection(pos, hit, context.clickedFace)
            val prop = GalePipeBlock.PROPERTY_BY_DIRECTION[targetDir] ?: return InteractionResult.PASS
            val currentMode = state.getValue(prop)

            val neighborPos = pos.relative(targetDir)
            val neighborState = level.getBlockState(neighborPos)
            val isNeighborPipe = neighborState.block is GalePipeBlock
            val isNeighborMachine = neighborState.`is`(ModBlocks.GALE_TANK) || neighborState.`is`(ModBlocks.CRYO_VENT)

            val nextMode: PipeConnectionState = when {
                // Raccordement entre 2 tuyaux : Uniquement Normal <-> Déconnecté
                isNeighborPipe -> {
                    if (currentMode.isConnected()) PipeConnectionState.NONE else PipeConnectionState.NORMAL
                }
                // Raccordement vers une machine : Normal -> Extraction -> Déconnecté -> Normal
                isNeighborMachine -> {
                    when (currentMode) {
                        PipeConnectionState.NORMAL -> PipeConnectionState.EXTRACT
                        PipeConnectionState.EXTRACT -> PipeConnectionState.NONE
                        PipeConnectionState.NONE -> PipeConnectionState.NORMAL
                    }
                }
                // Aucune machine ni tuyau adjacent : Bascule simple
                else -> {
                    if (currentMode.isConnected()) PipeConnectionState.NONE else PipeConnectionState.NORMAL
                }
            }

            if (!level.isClientSide) {
                // Met à jour le tuyau actuel
                level.setBlock(pos, state.setValue(prop, nextMode), 3)

                // Si le voisin est un autre tuyau, synchroniser sa connexion opposée
                if (isNeighborPipe) {
                    val neighborProp = GalePipeBlock.PROPERTY_BY_DIRECTION[targetDir.opposite]
                    if (neighborProp != null) {
                        level.setBlock(neighborPos, neighborState.setValue(neighborProp, nextMode), 3)
                    }
                }

                sendPipeFeedback(player, targetDir, nextMode, isNeighborMachine)
                playWrenchSound(level, pos)
                spawnWrenchParticles(level as ServerLevel, hit)
                context.itemInHand.hurtAndBreak(1, player, context.hand)
            }
            return InteractionResult.SUCCESS
        }

        // 3. Clic Droit sur un bloc orientable : Rotation du bloc
        if (canRotate(state)) {
            if (!level.isClientSide) {
                rotateBlock(level, pos, state)
                playWrenchSound(level, pos)
                spawnWrenchParticles(level as ServerLevel, hit)
                context.itemInHand.hurtAndBreak(1, player, context.hand)
            }
            return InteractionResult.SUCCESS
        }

        return super.useOn(context)
    }

    private fun getTargetPipeDirection(pos: BlockPos, hitLocation: Vec3, clickedFace: Direction): Direction {
        val center = Vec3.atCenterOf(pos)
        val delta = hitLocation.subtract(center)

        val maxAxis = maxOf(abs(delta.x), abs(delta.y), abs(delta.z))
        if (maxAxis > 0.2) {
            return when {
                abs(delta.x) == maxAxis -> if (delta.x > 0) Direction.EAST else Direction.WEST
                abs(delta.y) == maxAxis -> if (delta.y > 0) Direction.UP else Direction.DOWN
                else -> if (delta.z > 0) Direction.SOUTH else Direction.NORTH
            }
        }
        return clickedFace
    }

    private fun sendPipeFeedback(player: Player, dir: Direction, mode: PipeConnectionState, isMachine: Boolean) {
        val dirFr = when (dir) {
            Direction.NORTH -> "Nord"
            Direction.SOUTH -> "Sud"
            Direction.EAST -> "Est"
            Direction.WEST -> "Ouest"
            Direction.UP -> "Haut"
            Direction.DOWN -> "Bas"
        }
        val msg = when (mode) {
            PipeConnectionState.NORMAL -> if (isMachine) "§bNormal (Injection vers la machine)" else "§bConnecté (Normal)"
            PipeConnectionState.EXTRACT -> "§6Extraction (Pompe depuis la machine)"
            PipeConnectionState.NONE -> "§cFermé (Déconnecté)"
        }
        player.sendOverlayMessage(Component.literal("§e[Clé] §fBranche §6$dirFr §f: $msg"))
    }

    private fun isDismantlable(state: BlockState): Boolean {
        val name = state.block.descriptionId
        return name.contains("pipe") || name.contains("tank") || name.contains("vent") || name.contains("tomb")
    }

    private fun dismantleBlock(level: Level, pos: BlockPos, state: BlockState, player: Player, tool: ItemStack) {
        val drops = Block.getDrops(state, level as ServerLevel, pos, level.getBlockEntity(pos), player, tool)
        level.destroyBlock(pos, false, player)
        for (drop in drops) {
            if (!player.inventory.add(drop)) {
                Block.popResource(level, pos, drop)
            }
        }
        playDismantleSound(level, pos)
        level.sendParticles(ParticleTypes.POOF, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 8, 0.2, 0.2, 0.2, 0.05)
        player.sendOverlayMessage(Component.literal("§a[Clé] Machine démantelée proprement."))
    }

    private fun canRotate(state: BlockState): Boolean {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ||
               state.hasProperty(BlockStateProperties.FACING) ||
               state.hasProperty(BlockStateProperties.AXIS)
    }

    private fun rotateBlock(level: Level, pos: BlockPos, state: BlockState) {
        if (state.block is GaleTankBlock) {
            val half = state.getValue(GaleTankBlock.HALF)
            val currentFacing = state.getValue(GaleTankBlock.FACING)
            val nextFacing = currentFacing.clockWise

            val lowerPos = if (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) pos else pos.below()
            val upperPos = lowerPos.above()

            val lowerState = level.getBlockState(lowerPos)
            val upperState = level.getBlockState(upperPos)

            if (lowerState.`is`(state.block) && upperState.`is`(state.block)) {
                level.setBlock(lowerPos, lowerState.setValue(GaleTankBlock.FACING, nextFacing), 3)
                level.setBlock(upperPos, upperState.setValue(GaleTankBlock.FACING, nextFacing), 3)
            }
            return
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            val current = state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, current.clockWise), 3)
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            val current = state.getValue(BlockStateProperties.FACING)
            val next = Direction.entries[(current.ordinal + 1) % Direction.entries.size]
            level.setBlock(pos, state.setValue(BlockStateProperties.FACING, next), 3)
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            val current = state.getValue(BlockStateProperties.AXIS)
            val next = Direction.Axis.entries[(current.ordinal + 1) % Direction.Axis.entries.size]
            level.setBlock(pos, state.setValue(BlockStateProperties.AXIS, next), 3)
        }
    }

    private fun playWrenchSound(level: Level, pos: BlockPos) {
        level.playSound(
            null,
            pos,
            SoundEvents.ITEM_FRAME_ROTATE_ITEM,
            SoundSource.BLOCKS,
            1.0f,
            1.3f
        )
    }

    private fun playDismantleSound(level: Level, pos: BlockPos) {
        level.playSound(
            null,
            pos,
            SoundEvents.COPPER_BREAK,
            SoundSource.BLOCKS,
            1.0f,
            1.0f
        )
    }

    private fun spawnWrenchParticles(level: ServerLevel, hitLocation: Vec3) {
        level.sendParticles(
            ParticleTypes.CRIT,
            hitLocation.x,
            hitLocation.y,
            hitLocation.z,
            6,
            0.05,
            0.05,
            0.05,
            0.08
        )
    }
}
