package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.GaleBellowsBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * GaleBellowsBlock (Poumon Mécanique / Soufflet de Bourrasque Automatique)
 *
 * Générateur d'énergie "Vent" autonome fonctionnant en continu avec un rythme lent de respiration.
 * - getShape() (Hitbox de sélection/visée) : Intègre la buse centrale fixe (17 px) et le soufflet animé (4.5..14 px).
 * - getCollisionShape() (Collision physique) : Intègre la buse centrale (17 px) et le socle supérieur (14 px).
 */
class GaleBellowsBlock(properties: Properties) : Block(properties), EntityBlock {

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
        )
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GaleBellowsBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return BlockEntityTicker { lvl, pos, st, be ->
            if (be is GaleBellowsBlockEntity) {
                be.tick(lvl, pos, st)
            }
        }
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val gameTime = if (level is Level) {
            level.gameTime
        } else {
            val be = level.getBlockEntity(pos)
            be?.level?.gameTime ?: 0L
        }
        val tick = ((gameTime % GaleBellowsBlockEntity.CYCLE_TICKS).toInt() + GaleBellowsBlockEntity.CYCLE_TICKS) % GaleBellowsBlockEntity.CYCLE_TICKS
        return SHAPES_BY_TICK[tick]
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = COLLISION_SHAPE

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    companion object {
        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING

        /**
         * Hitbox de la buse centrale (nozzle) : [4..12, 0..17, 4..12]
         */
        val NOZZLE_SHAPE: VoxelShape = box(4.0, 0.0, 4.0, 12.0, 17.0, 12.0)

        /**
         * Collision physique combinant la buse centrale et les plateaux
         */
        val COLLISION_SHAPE: VoxelShape = Shapes.or(
            box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0),
            NOZZLE_SHAPE
        )

        /**
         * Pré-calcul des 70 formes de sélection pour chaque tick du cycle de respiration,
         * fusionnant la buse centrale fixe avec le soufflet animé (Y allant de 4.5 px compressé à 14.0 px déployé).
         */
        val SHAPES_BY_TICK: Array<VoxelShape> = Array(GaleBellowsBlockEntity.CYCLE_TICKS) { tick ->
            val cycle = GaleBellowsBlockEntity.CYCLE_TICKS.toDouble()
            val t = (tick.toDouble() % cycle) / cycle
            val comp = 0.5 - 0.5 * kotlin.math.cos(t * 2.0 * Math.PI)
            val heightPixels = 14.0 - comp * 9.5
            val bellowsShape = box(1.0, 0.0, 1.0, 15.0, heightPixels, 15.0)
            Shapes.or(bellowsShape, NOZZLE_SHAPE)
        }
    }
}
