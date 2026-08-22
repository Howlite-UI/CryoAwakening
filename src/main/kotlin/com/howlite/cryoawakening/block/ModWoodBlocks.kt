package com.howlite.cryoawakening.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.DoorBlock
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

class ModSaplingBlock(properties: Properties) : BushBlock(properties) {
    companion object {
        val SHAPE: VoxelShape = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPE
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
