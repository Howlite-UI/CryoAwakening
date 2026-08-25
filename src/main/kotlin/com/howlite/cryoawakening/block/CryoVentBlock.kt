package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.CryoVentBlockEntity
import com.howlite.cryoawakening.item.WrenchItem
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class CryoVentBlock(properties: Properties) : Block(properties), EntityBlock {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CryoVentBlockEntity(pos, state)

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level.isClientSide) {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is CryoVentBlockEntity) {
                    CryoVentBlockEntity.clientTick(l, p, s, be)
                }
            }
        } else {
            BlockEntityTicker<T> { l, p, s, be ->
                if (be is CryoVentBlockEntity) {
                    CryoVentBlockEntity.serverTick(l, p, s, be)
                }
            }
        }
    }

    override fun animateTick(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource
    ) {
        // Géré par CryoVentBlockEntity.clientTick
    }
}
