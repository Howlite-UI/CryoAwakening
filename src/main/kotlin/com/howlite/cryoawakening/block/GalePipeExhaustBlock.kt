package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.GalePipeExhaustBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.EnumMap

/**
 * GalePipeExhaustBlock (Échappement de Tuyau de Bourrasque)
 *
 * Bloc directionnel (6 orientations) servant d'embout de tuyau propulseur ou d'éjecteur de vent.
 * - Hitbox ajustée uniquement sur le conduit et la bride avant (pas sur le volant).
 * - Orientation intelligente : le volant de vanne est toujours orienté vers le joueur lors de la pose au sol ou plafond.
 * - Réglage in-world style Create mod (maintien du clic-droit + mouvement de souris / molette).
 */
class GalePipeExhaustBlock(properties: Properties) : Block(properties), EntityBlock, SimpleWaterloggedBlock {

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VALVE_FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GalePipeExhaustBlockEntity(pos, state)

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide) {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is GalePipeExhaustBlockEntity) {
                    GalePipeExhaustBlockEntity.serverTick(l, p, s, be)
                }
            }
        } else {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is GalePipeExhaustBlockEntity) {
                    GalePipeExhaustBlockEntity.clientTick(l, p, s, be)
                }
            }
        }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return SHAPES[facing] ?: SHAPES[Direction.NORTH]!!
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val fluidState = context.level.getFluidState(context.clickedPos)
        val clickedFace = context.clickedFace
        val facing = if (context.isSecondaryUseActive) clickedFace.opposite else clickedFace
        val playerFacing = context.horizontalDirection.opposite // Orientation de la vanne vers le joueur

        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(VALVE_FACING, playerFacing)
            .setValue(WATERLOGGED, fluidState.`type` == Fluids.WATER)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        return InteractionResult.CONSUME
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
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state
            .setValue(FACING, rotation.rotate(state.getValue(FACING)))
            .setValue(VALVE_FACING, rotation.rotate(state.getValue(VALVE_FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state
            .rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, VALVE_FACING, WATERLOGGED)
    }

    companion object {
        val CODEC: MapCodec<GalePipeExhaustBlock> = simpleCodec(::GalePipeExhaustBlock)
        val FACING: EnumProperty<Direction> = BlockStateProperties.FACING
        val VALVE_FACING: EnumProperty<Direction> = EnumProperty.create("facing_horizontal", Direction::class.java, Direction.Plane.HORIZONTAL)
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        // Hitbox ajustée fidèlement sur le conduit et la bride avant (sans inclure le volant de vanne)
        private val SHAPES: Map<Direction, VoxelShape> = EnumMap<Direction, VoxelShape>(Direction::class.java).apply {
            put(
                Direction.NORTH,
                Shapes.or(
                    box(3.0, 3.0, 0.0, 13.0, 13.0, 2.0),
                    box(4.0, 4.0, 2.0, 12.0, 12.0, 16.0)
                )
            )
            put(
                Direction.SOUTH,
                Shapes.or(
                    box(3.0, 3.0, 14.0, 13.0, 13.0, 16.0),
                    box(4.0, 4.0, 0.0, 12.0, 12.0, 14.0)
                )
            )
            put(
                Direction.WEST,
                Shapes.or(
                    box(0.0, 3.0, 3.0, 2.0, 13.0, 13.0),
                    box(2.0, 4.0, 4.0, 16.0, 12.0, 12.0)
                )
            )
            put(
                Direction.EAST,
                Shapes.or(
                    box(14.0, 3.0, 3.0, 16.0, 13.0, 13.0),
                    box(0.0, 4.0, 4.0, 14.0, 12.0, 12.0)
                )
            )
            put(
                Direction.UP,
                Shapes.or(
                    box(3.0, 14.0, 3.0, 13.0, 16.0, 13.0),
                    box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0)
                )
            )
            put(
                Direction.DOWN,
                Shapes.or(
                    box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0),
                    box(4.0, 2.0, 4.0, 12.0, 16.0, 12.0)
                )
            )
        }
    }
}
