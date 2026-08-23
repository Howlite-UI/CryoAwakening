package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.world.AncientLilacTreeGenerator
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.PressurePlateBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class ModDoorBlock(type: BlockSetType, properties: Properties) : DoorBlock(type, properties)

class ModTrapDoorBlock(type: BlockSetType, properties: Properties) : TrapDoorBlock(type, properties)

class ModButtonBlock(type: BlockSetType, ticksToStayPressed: Int, properties: Properties) :
    ButtonBlock(type, ticksToStayPressed, properties)

class ModPressurePlateBlock(type: BlockSetType, properties: Properties) :
    PressurePlateBlock(type, properties)

class ModStairBlock(baseState: BlockState, properties: Properties) :
    StairBlock(baseState, properties)

class ModBushBlock(properties: Properties) :
    BushBlock(properties)

class ModLeavesBlock(properties: Properties) : LeavesBlock(0.0f, properties) {
    override fun spawnFallingLeavesParticle(level: Level, pos: BlockPos, random: RandomSource) {
        // Aucune particule de feuille tombante
    }

    override fun codec(): MapCodec<out LeavesBlock> = CODEC

    companion object {
        val CODEC: MapCodec<ModLeavesBlock> = simpleCodec(::ModLeavesBlock)
    }
}

class ModSaplingBlock(properties: Properties) : BushBlock(properties), BonemealableBlock {
    companion object {
        val SHAPE: VoxelShape = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPE

    override fun isValidBonemealTarget(
        level: LevelReader,
        pos: BlockPos,
        state: BlockState
    ): Boolean = true

    override fun isBonemealSuccess(
        level: Level,
        random: RandomSource,
        pos: BlockPos,
        state: BlockState
    ): Boolean = (random.nextFloat() < 0.45f)

    override fun performBonemeal(
        serverLevel: ServerLevel,
        random: RandomSource,
        pos: BlockPos,
        state: BlockState
    ) {
        AncientLilacTreeGenerator.generate(serverLevel, pos, random)
    }

    override fun randomTick(
        state: BlockState,
        serverLevel: ServerLevel,
        pos: BlockPos,
        random: RandomSource
    ) {
        if (random.nextInt(7) == 0 && serverLevel.getMaxLocalRawBrightness(pos.above()) >= 8) {
            AncientLilacTreeGenerator.generate(serverLevel, pos, random)
        }
    }
}

class LichenBushBlock(properties: Properties) : BushBlock(properties) {
    companion object {
        val SHAPE: VoxelShape = Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPE
}
