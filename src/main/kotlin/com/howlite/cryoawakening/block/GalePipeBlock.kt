package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * GalePipeBlock
 *
 * Tuyau de Bourrasque assurant un acheminement direct Point A -> Point B (sans multiconnexion).
 * S'adapte automatiquement en ligne droite, coude à 90° ou terminaison à bride.
 */
class GalePipeBlock(properties: Properties) : Block(properties), SimpleWaterloggedBlock {

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(SHAPE, GalePipeShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPES_MAP[state.getValue(SHAPE)] ?: CORE_SHAPE
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        val pos = context.clickedPos
        val fluidState = level.getFluidState(pos)
        val preferredAxis = context.clickedFace.axis

        val shape = computeShape(level, pos, preferredAxis)

        return defaultBlockState()
            .setValue(SHAPE, shape)
            .setValue(WATERLOGGED, fluidState.`type` == Fluids.WATER)
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

        val currentShape = state.getValue(SHAPE)
        val preferredAxis = when {
            currentShape.direction1 != null && currentShape.direction2 != null && currentShape.direction1.axis == currentShape.direction2.axis ->
                currentShape.direction1.axis
            currentShape.direction1 != null -> currentShape.direction1.axis
            else -> Direction.Axis.Z
        }

        val newShape = computeShape(level, pos, preferredAxis)
        return state.setValue(SHAPE, newShape)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(SHAPE, WATERLOGGED)
    }

    private fun computeShape(level: BlockGetter, pos: BlockPos, defaultAxis: Direction.Axis): GalePipeShape {
        val connectedDirections = mutableListOf<Direction>()

        for (dir in Direction.entries) {
            val neighborPos = pos.relative(dir)
            val neighborState = level.getBlockState(neighborPos)

            if (canConnectTo(neighborState, dir.opposite)) {
                connectedDirections.add(dir)
                if (connectedDirections.size >= 2) break
            }
        }

        return when (connectedDirections.size) {
            2 -> GalePipeShape.fromDirections(connectedDirections[0], connectedDirections[1])
            1 -> GalePipeShape.fromSingleDirection(connectedDirections[0])
            else -> GalePipeShape.fromAxis(defaultAxis)
        }
    }

    private fun canConnectTo(state: BlockState, fromDirection: Direction): Boolean {
        if (state.`is`(this)) {
            val neighborShape = state.getValue(SHAPE)
            // Connecte si le tuyau voisin est dirigé vers nous ou a une terminaison libre
            return neighborShape.connectsTo(fromDirection) ||
                   (neighborShape.direction2 == null && neighborShape.direction1 != fromDirection.opposite)
        }
        if (state.`is`(ModBlocks.GALE_TANK)) return true
        if (state.`is`(ModBlocks.CRYO_VENT)) return true
        return false
    }

    companion object {
        val SHAPE: EnumProperty<GalePipeShape> = EnumProperty.create("shape", GalePipeShape::class.java)
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        private val CORE_SHAPE: VoxelShape = box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0)
        private val NORTH_ARM: VoxelShape = box(4.0, 4.0, 0.0, 12.0, 12.0, 4.0)
        private val SOUTH_ARM: VoxelShape = box(4.0, 4.0, 12.0, 12.0, 12.0, 16.0)
        private val EAST_ARM: VoxelShape = box(12.0, 4.0, 4.0, 16.0, 12.0, 12.0)
        private val WEST_ARM: VoxelShape = box(0.0, 4.0, 4.0, 4.0, 12.0, 12.0)
        private val UP_ARM: VoxelShape = box(4.0, 12.0, 4.0, 12.0, 16.0, 12.0)
        private val DOWN_ARM: VoxelShape = box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0)

        private fun getArmShape(dir: Direction): VoxelShape = when (dir) {
            Direction.NORTH -> NORTH_ARM
            Direction.SOUTH -> SOUTH_ARM
            Direction.EAST -> EAST_ARM
            Direction.WEST -> WEST_ARM
            Direction.UP -> UP_ARM
            Direction.DOWN -> DOWN_ARM
        }

        val SHAPES_MAP: Map<GalePipeShape, VoxelShape> = GalePipeShape.entries.associateWith { shape ->
            var combined = CORE_SHAPE
            if (shape.direction1 != null) {
                combined = Shapes.or(combined, getArmShape(shape.direction1))
            }
            if (shape.direction2 != null) {
                combined = Shapes.or(combined, getArmShape(shape.direction2))
            }
            combined
        }
    }
}
