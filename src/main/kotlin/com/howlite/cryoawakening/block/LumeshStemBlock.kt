package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class LumeshStemBlock(properties: Properties) : BushBlock(properties), BonemealableBlock {

    companion object {
        const val MAX_AGE = 4
        val AGE: IntegerProperty = IntegerProperty.create("age", 0, MAX_AGE)

        private val SHAPES = arrayOf(
            Block.box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0)
        )
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(AGE, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(AGE)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val age = state.getValue(AGE).coerceIn(0, MAX_AGE)
        return SHAPES[age]
    }

    override fun mayPlaceOn(groundState: BlockState, level: BlockGetter, pos: BlockPos): Boolean {
        return groundState.`is`(ModBlocks.RIMECRUST) ||
               groundState.`is`(ModBlocks.RIMECRUST_LICHEN) ||
               groundState.`is`(ModBlocks.RIMEBLOOM) ||
               groundState.`is`(ModBlocks.RIMEBLOOM_GRASS) ||
               groundState.`is`(BlockTags.DIRT) ||
               groundState.`is`(Blocks.FARMLAND)
    }

    override fun randomTick(
        state: BlockState,
        level: ServerLevel,
        pos: BlockPos,
        random: RandomSource
    ) {
        if (level.getRawBrightness(pos, 0) < 8) return

        val age = state.getValue(AGE)
        if (age < MAX_AGE) {
            // Vitesse de croissance naturelle équilibrée (style vanilla)
            if (random.nextInt(25) == 0) {
                val nextAge = age + 1
                level.setBlock(pos, state.setValue(AGE, nextAge), Block.UPDATE_CLIENTS)

                // Si la tige atteint le stade maximal (4), faire pousser le fruit immédiatement au-dessus !
                if (nextAge == MAX_AGE) {
                    spawnFruitAbove(level, pos, random)
                }
            }
        } else {
            // Déjà au stade 4 (après récolte du fruit) : faire repousser un nouveau fruit au fil du temps
            if (random.nextInt(25) == 0) {
                spawnFruitAbove(level, pos, random)
            }
        }
    }

    // --- BonemealableBlock ---
    override fun isValidBonemealTarget(
        level: LevelReader,
        pos: BlockPos,
        state: BlockState
    ): Boolean {
        val age = state.getValue(AGE)
        if (age < MAX_AGE) return true
        val abovePos = pos.above()
        val aboveState = level.getBlockState(abovePos)
        return aboveState.isAir || aboveState.canBeReplaced()
    }

    override fun isBonemealSuccess(
        level: Level,
        random: RandomSource,
        pos: BlockPos,
        state: BlockState
    ): Boolean = true

    override fun performBonemeal(
        level: ServerLevel,
        random: RandomSource,
        pos: BlockPos,
        state: BlockState
    ) {
        val age = state.getValue(AGE)
        if (age < MAX_AGE) {
            val newAge = (age + random.nextIntBetweenInclusive(1, 2)).coerceAtMost(MAX_AGE)
            level.setBlock(pos, state.setValue(AGE, newAge), Block.UPDATE_CLIENTS)

            // Si le coup de poudre d'os amène la tige au stade 4, faire jaillir le fruit immédiatement !
            if (newAge == MAX_AGE) {
                spawnFruitAbove(level, pos, random)
            }
        } else {
            // Déjà au stade 4 : coup de poudre d'os fait repousser le fruit au-dessus !
            spawnFruitAbove(level, pos, random)
        }
    }

    private fun spawnFruitAbove(level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val abovePos = pos.above()
        val aboveState = level.getBlockState(abovePos)
        if (aboveState.isAir || aboveState.canBeReplaced()) {
            val fruitState = if (random.nextBoolean()) {
                ModBlocks.ORANGE_LUMESH.defaultBlockState()
            } else {
                ModBlocks.YELLOW_LUMESH.defaultBlockState()
            }
            level.setBlock(abovePos, fruitState, Block.UPDATE_ALL)
        }
    }
}
