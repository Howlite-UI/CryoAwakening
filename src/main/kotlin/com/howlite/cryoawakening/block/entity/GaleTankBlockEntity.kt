package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.block.GaleTankBlock
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * BlockEntity pour le Gale Tank (Réservoir de Bourrasque).
 * - Capacité de stockage : 20 000 V
 * - Synchronisé et partagé entre les deux moitiés du bloc.
 */
class GaleTankBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.GALE_TANK_BLOCK_ENTITY_TYPE, pos, state), IWindHolder {

    val windStorage = WindStorage(capacity = 20_000, maxReceive = 200, maxExtract = 200)

    override fun getWindStorage(side: Direction?): WindStorage? {
        val half = blockState.getValue(GaleTankBlock.HALF)
        if (half == DoubleBlockHalf.UPPER) {
            val lowerBe = level?.getBlockEntity(blockPos.below()) as? GaleTankBlockEntity
            return lowerBe?.windStorage ?: windStorage
        }
        return windStorage
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        windStorage.save(output)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        windStorage.load(input)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        val storage = getWindStorage(null) ?: windStorage
        tag.putInt("wind_amount", storage.wind)
        tag.putInt("wind_capacity", storage.capacity)
        return tag
    }
}
