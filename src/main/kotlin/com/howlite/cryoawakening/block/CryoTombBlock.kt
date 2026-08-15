package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.CryoTombBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult

/**
 * CryoTombBlock - Bloc de glace de 2 blocs de haut renfermant un monstre ancien.
 *
 * Supporte le changement de mob par clic droit avec n'importe quel œuf d'apparition (Spawn Egg).
 * Utilise la propriété DoubleBlockHalf (LOWER et UPPER) sans démarcation interne visible.
 */
class CryoTombBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val HALF: EnumProperty<DoubleBlockHalf> = BlockStateProperties.DOUBLE_BLOCK_HALF

        fun getEntityTypeFromEgg(item: Item): EntityType<*>? {
            return BuiltInRegistries.ENTITY_TYPE.firstOrNull { type ->
                SpawnEggItem.byId(type).map { it.value() == item }.orElse(false)
            }
        }
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HALF)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun skipRendering(state: BlockState, adjacentBlockState: BlockState, direction: Direction): Boolean {
        if (adjacentBlockState.`is`(this)) {
            val half = state.getValue(HALF)
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) return true
            if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) return true
        }
        return super.skipRendering(state, adjacentBlockState, direction)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): InteractionResult {
        val item = stack.item
        if (item is SpawnEggItem) {
            val entityType = getEntityTypeFromEgg(item)
            if (entityType != null) {
                val lowerPos = if (state.getValue(HALF) == DoubleBlockHalf.UPPER) pos.below() else pos
                val blockEntity = level.getBlockEntity(lowerPos) as? CryoTombBlockEntity

                if (blockEntity != null) {
                    if (!level.isClientSide) {
                        blockEntity.setEntityType(entityType)

                        // Particules et son de succès
                        (level as? ServerLevel)?.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            lowerPos.x + 0.5,
                            lowerPos.y + 1.0,
                            lowerPos.z + 0.5,
                            12,
                            0.35, 0.6, 0.35,
                            0.05
                        )
                        (level as? ServerLevel)?.sendParticles(
                            ParticleTypes.SNOWFLAKE,
                            lowerPos.x + 0.5,
                            lowerPos.y + 1.0,
                            lowerPos.z + 0.5,
                            15,
                            0.35, 0.6, 0.35,
                            0.02
                        )

                        level.playSound(
                            null,
                            lowerPos,
                            SoundEvents.ITEM_FRAME_ADD_ITEM,
                            SoundSource.BLOCKS,
                            1.0f,
                            1.2f
                        )

                        if (!player.isCreative) {
                            stack.shrink(1)
                        }
                    }
                    return InteractionResult.SUCCESS
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val pos = context.clickedPos
        val level = context.level
        return if (!level.isOutsideBuildHeight(pos.above()) && level.getBlockState(pos.above()).canBeReplaced(context)) {
            defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER)
        } else {
            null
        }
    }

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            val belowState = level.getBlockState(pos.below())
            belowState.`is`(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER
        } else {
            super.canSurvive(state, level, pos)
        }
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        ticks: ScheduledTickAccess,
        pos: BlockPos,
        directionToNeighbour: Direction,
        neighbourPos: BlockPos,
        neighbourState: BlockState,
        random: RandomSource
    ): BlockState {
        val half = state.getValue(HALF)
        if (directionToNeighbour.axis == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && directionToNeighbour == Direction.UP) {
                if (!neighbourState.`is`(this) || neighbourState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                    return Blocks.AIR.defaultBlockState()
                }
            } else if (half == DoubleBlockHalf.UPPER && directionToNeighbour == Direction.DOWN) {
                if (!neighbourState.`is`(this) || neighbourState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                    return Blocks.AIR.defaultBlockState()
                }
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random)
    }

    override fun playerWillDestroy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        player: Player
    ): BlockState {
        if (!level.isClientSide) {
            val half = state.getValue(HALF)
            val otherPos = if (half == DoubleBlockHalf.LOWER) pos.above() else pos.below()
            val otherState = level.getBlockState(otherPos)

            if (otherState.`is`(this) && otherState.getValue(HALF) != half) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL or Block.UPDATE_SUPPRESS_DROPS)
                level.levelEvent(player, 2001, otherPos, getId(otherState))
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            CryoTombBlockEntity(pos, state)
        } else {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is CryoTombBlockEntity) {
                    CryoTombBlockEntity.serverTick(l, p, s, be)
                }
            }
        } else {
            null
        }
    }
}
