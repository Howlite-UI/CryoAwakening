package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.block.entity.GalePipeBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
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
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.EnumMap

/**
 * GalePipeBlock
 *
 * Tuyau de Bourrasque multidirectionnel (jusqu'à 6 connexions simultanées : Nord, Sud, Est, Ouest, Haut, Bas).
 * Supporte les états : Déconnecté (none), Normal (normal), et Extraction (extract).
 */
class GalePipeBlock(properties: Properties) : Block(properties), SimpleWaterloggedBlock, EntityBlock {

    private val shapesByState: Map<BlockState, VoxelShape>

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(NORTH, PipeConnectionState.NONE)
                .setValue(EAST, PipeConnectionState.NONE)
                .setValue(SOUTH, PipeConnectionState.NONE)
                .setValue(WEST, PipeConnectionState.NONE)
                .setValue(UP, PipeConnectionState.NONE)
                .setValue(DOWN, PipeConnectionState.NONE)
                .setValue(WATERLOGGED, false)
        )
        shapesByState = buildShapesMap()
    }

    override fun codec(): MapCodec<out Block> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GalePipeBlockEntity(pos, state)

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide) {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is GalePipeBlockEntity) {
                    GalePipeBlockEntity.serverTick(l, p, s, be)
                }
            }
        } else null
    }

    private fun buildShapesMap(): Map<BlockState, VoxelShape> {
        val map = HashMap<BlockState, VoxelShape>()
        val core = box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0)
        val northArm = box(4.0, 4.0, 0.0, 12.0, 12.0, 4.0)
        val southArm = box(4.0, 4.0, 12.0, 12.0, 12.0, 16.0)
        val eastArm = box(12.0, 4.0, 4.0, 16.0, 12.0, 12.0)
        val westArm = box(0.0, 4.0, 4.0, 4.0, 12.0, 12.0)
        val upArm = box(4.0, 12.0, 4.0, 12.0, 16.0, 12.0)
        val downArm = box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0)

        for (state in stateDefinition.possibleStates) {
            var shape = core
            if (state.getValue(NORTH).isConnected()) shape = Shapes.or(shape, northArm)
            if (state.getValue(SOUTH).isConnected()) shape = Shapes.or(shape, southArm)
            if (state.getValue(EAST).isConnected()) shape = Shapes.or(shape, eastArm)
            if (state.getValue(WEST).isConnected()) shape = Shapes.or(shape, westArm)
            if (state.getValue(UP).isConnected()) shape = Shapes.or(shape, upArm)
            if (state.getValue(DOWN).isConnected()) shape = Shapes.or(shape, downArm)
            map[state] = shape
        }
        return map
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return shapesByState[state] ?: box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        val pos = context.clickedPos
        val fluidState = level.getFluidState(pos)

        var state = defaultBlockState().setValue(WATERLOGGED, fluidState.`type` == Fluids.WATER)

        var hasAnyConnection = false
        for (dir in Direction.entries) {
            val neighborPos = pos.relative(dir)
            val neighborState = level.getBlockState(neighborPos)
            if (canConnectTo(neighborState)) {
                state = state.setValue(PROPERTY_BY_DIRECTION[dir]!!, PipeConnectionState.NORMAL)
                hasAnyConnection = true
            }
        }

        // Si aucune connexion adjacente, connecter le long de l'axe cliqué
        if (!hasAnyConnection) {
            when (context.clickedFace.axis) {
                Direction.Axis.X -> state = state.setValue(EAST, PipeConnectionState.NORMAL).setValue(WEST, PipeConnectionState.NORMAL)
                Direction.Axis.Y -> state = state.setValue(UP, PipeConnectionState.NORMAL).setValue(DOWN, PipeConnectionState.NORMAL)
                Direction.Axis.Z -> state = state.setValue(NORTH, PipeConnectionState.NORMAL).setValue(SOUTH, PipeConnectionState.NORMAL)
            }
        }

        return state
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

        val prop = PROPERTY_BY_DIRECTION[direction]!!
        val currentConnection = state.getValue(prop)

        // Si la connexion était désactivée volontairement par le joueur, conserver NONE
        if (currentConnection == PipeConnectionState.NONE && !canConnectTo(neighborState)) {
            return state
        }

        // Si le bloc voisin a été cassé/retiré, repasser en NONE
        if (currentConnection.isConnected() && !canConnectTo(neighborState)) {
            return state.setValue(prop, PipeConnectionState.NONE)
        }

        // Si un bloc connectable est apparu et qu'on était déconnecté, se connecter en NORMAL
        if (currentConnection == PipeConnectionState.NONE && canConnectTo(neighborState)) {
            return state.setValue(prop, PipeConnectionState.NORMAL)
        }

        return state
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED)
    }

    fun canConnectTo(state: BlockState): Boolean {
        if (state.`is`(this)) return true
        if (state.`is`(ModBlocks.GALE_PIPE_EXHAUST)) return true
        if (state.`is`(ModBlocks.GALE_TANK)) return true
        if (state.`is`(ModBlocks.CRYO_VENT)) return true
        if (state.`is`(ModBlocks.GALE_BELLOWS)) return true
        if (state.`is`(ModBlocks.BREEZE_FOUNDRY)) return true
        return false
    }

    companion object {
        val CODEC: MapCodec<GalePipeBlock> = simpleCodec(::GalePipeBlock)

        val NORTH: EnumProperty<PipeConnectionState> = EnumProperty.create("north", PipeConnectionState::class.java)
        val EAST: EnumProperty<PipeConnectionState> = EnumProperty.create("east", PipeConnectionState::class.java)
        val SOUTH: EnumProperty<PipeConnectionState> = EnumProperty.create("south", PipeConnectionState::class.java)
        val WEST: EnumProperty<PipeConnectionState> = EnumProperty.create("west", PipeConnectionState::class.java)
        val UP: EnumProperty<PipeConnectionState> = EnumProperty.create("up", PipeConnectionState::class.java)
        val DOWN: EnumProperty<PipeConnectionState> = EnumProperty.create("down", PipeConnectionState::class.java)
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        val PROPERTY_BY_DIRECTION: Map<Direction, EnumProperty<PipeConnectionState>> = EnumMap<Direction, EnumProperty<PipeConnectionState>>(Direction::class.java).apply {
            put(Direction.NORTH, NORTH)
            put(Direction.EAST, EAST)
            put(Direction.SOUTH, SOUTH)
            put(Direction.WEST, WEST)
            put(Direction.UP, UP)
            put(Direction.DOWN, DOWN)
        }
    }
}
