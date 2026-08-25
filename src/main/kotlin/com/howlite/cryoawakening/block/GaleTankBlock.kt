package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.GaleTankBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult

/**
 * GaleTankBlock
 *
 * Réservoir de bourrasque double-bloc (2 blocs de haut, comme une porte ou une haute plante).
 * Partie basse (half=lower) et partie haute (half=upper).
 */
class GaleTankBlock(properties: Properties) : Block(properties), EntityBlock {

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
        )
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GaleTankBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level.isClientSide) {
            BlockEntityTicker { lvl, pos, st, be ->
                if (be is GaleTankBlockEntity && st.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    be.clientTick(lvl, pos, st)
                }
            }
        } else null
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val pos = context.clickedPos
        val level = context.level

        // Vérifier que le bloc au-dessus est libre et peut accueillir la partie haute
        if (pos.y < level.maxY - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState()
                .setValue(FACING, context.horizontalDirection.opposite)
                .setValue(HALF, DoubleBlockHalf.LOWER)
        }
        return null
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        // Placer automatiquement la partie supérieure
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        val half = state.getValue(HALF)
        return if (half == DoubleBlockHalf.LOWER) {
            level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
        } else {
            val belowState = level.getBlockState(pos.below())
            belowState.`is`(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER
        }
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        scheduledTickAccess: ScheduledTickAccess,
        pos: BlockPos,
        direction: Direction,
        neighborPos: BlockPos,
        neighborState: BlockState,
        random: RandomSource
    ): BlockState {
        val half = state.getValue(HALF)

        // Si la moitié correspondante (haut pour bas, bas pour haut) est détruite, casser le bloc
        if (direction.axis == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER == (direction == Direction.UP))) {
            return if (neighborState.`is`(this) && neighborState.getValue(HALF) != half) {
                state
            } else {
                Blocks.AIR.defaultBlockState()
            }
        }

        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState()
        }

        return state
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!level.isClientSide) {
            if (player.isCreative) {
                preventDropFromBottomPart(level, pos, state, player)
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    private fun preventDropFromBottomPart(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        val half = state.getValue(HALF)
        if (half == DoubleBlockHalf.UPPER) {
            val belowPos = pos.below()
            val belowState = level.getBlockState(belowPos)
            if (belowState.`is`(state.block) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                val fluidState = level.getFluidState(belowPos)
                val replaceState = if (fluidState.`is`(Fluids.WATER)) Blocks.WATER.defaultBlockState() else Blocks.AIR.defaultBlockState()
                level.setBlock(belowPos, replaceState, 35)
                level.levelEvent(player, 2001, belowPos, getId(belowState))
            }
        }
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, HALF)
    }

    companion object {
        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING
        val HALF: EnumProperty<DoubleBlockHalf> = BlockStateProperties.DOUBLE_BLOCK_HALF
    }
}
