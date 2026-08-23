package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class LumeshStemBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.LUMESH_STEM_BLOCK_ENTITY_TYPE, pos, state)
