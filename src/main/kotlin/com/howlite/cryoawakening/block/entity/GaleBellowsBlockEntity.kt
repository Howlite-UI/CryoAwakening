package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.block.GaleBellowsBlock
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * BlockEntity pour le Gale Bellows (Poumon Mécanique / Soufflet Automatique).
 *
 * Fonctionnement continu et automatique :
 * - Respiration lente et fluide (cycle de ~3.5 secondes)
 * - Génération continue de vent (+15 V par expiration)
 * - Distribution constante vers les tuyaux et réservoirs adjacents
 */
class GaleBellowsBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.GALE_BELLOWS_BLOCK_ENTITY_TYPE, pos, state), IWindHolder {

    val windStorage = WindStorage(capacity = 500, maxReceive = 500, maxExtract = 50)

    var animationTicks: Int = 0

    companion object {
        const val CYCLE_TICKS = 70 // ~3.5 secondes par cycle complet de respiration
        const val WIND_PER_BREATH = 15
    }

    override fun getWindStorage(side: Direction?): WindStorage = windStorage

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        animationTicks++

        // Génération de vent au point culminant de l'expiration (milieu du cycle)
        if (animationTicks % CYCLE_TICKS == (CYCLE_TICKS / 2)) {
            if (!level.isClientSide) {
                windStorage.receiveWind(WIND_PER_BREATH)
                distributeWind(level, pos, state)
                level.sendBlockUpdated(pos, state, state, 2)
            }
        }

        // Sur serveur, continuer à vider le tampon d'énergie vers les voisins
        if (!level.isClientSide && windStorage.wind > 0) {
            distributeWind(level, pos, state)
        }
    }

    private fun distributeWind(level: Level, pos: BlockPos, state: BlockState) {
        val facing = state.getValue(GaleBellowsBlock.FACING)
        // Distribue prioritairement dans la direction du soufflet, puis sur les autres faces
        val directions = listOf(facing, Direction.DOWN, Direction.UP, facing.clockWise, facing.counterClockWise, facing.opposite)

        for (dir in directions) {
            if (windStorage.wind <= 0) break
            val targetPos = pos.relative(dir)
            val targetBe = level.getBlockEntity(targetPos)
            if (targetBe is IWindHolder) {
                val targetStorage = targetBe.getWindStorage(dir.opposite)
                if (targetStorage != null && targetStorage.space > 0) {
                    val extractAmount = minOf(windStorage.wind, 15, targetStorage.space)
                    val extracted = windStorage.extractWind(extractAmount)
                    val received = targetStorage.receiveWind(extracted)
                    if (extracted > received) {
                        windStorage.receiveWind(extracted - received)
                    }
                    if (received > 0) {
                        level.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 2)
                        level.sendBlockUpdated(pos, state, state, 2)
                    }
                }
            }
        }
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
        tag.putInt("wind_amount", windStorage.wind)
        tag.putInt("wind_capacity", windStorage.capacity)
        return tag
    }
}
