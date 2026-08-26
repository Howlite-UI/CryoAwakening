package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.ModParticleTypes
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
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * BlockEntity pour le Gale Tank (Réservoir de Bourrasque).
 * - Capacité de stockage : 20 000 V
 * - Synchronisé, partagé et sauvegardé de manière unifiée entre les moitiés haute et basse.
 */
class GaleTankBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.GALE_TANK_BLOCK_ENTITY_TYPE, pos, state), IWindHolder {

    val windStorage = WindStorage(capacity = 20_000, maxReceive = 200, maxExtract = 200)

    val isUpper: Boolean
        get() = blockState.hasProperty(GaleTankBlock.HALF) && blockState.getValue(GaleTankBlock.HALF) == DoubleBlockHalf.UPPER

    val lowerPos: BlockPos
        get() = if (isUpper) blockPos.below() else blockPos

    val upperPos: BlockPos
        get() = if (isUpper) blockPos else blockPos.above()

    fun getLowerEntity(): GaleTankBlockEntity? {
        val lvl = level ?: return null
        return if (isUpper) lvl.getBlockEntity(lowerPos) as? GaleTankBlockEntity else this
    }

    override fun getWindStorage(side: Direction?): WindStorage {
        val lower = getLowerEntity()
        return lower?.windStorage ?: windStorage
    }

    fun syncBothHalves() {
        val lvl = level ?: return
        if (!lvl.isClientSide) {
            val lowerState = lvl.getBlockState(lowerPos)
            val upperState = lvl.getBlockState(upperPos)
            if (lowerState.`is`(ModBlocks.GALE_TANK)) {
                lvl.sendBlockUpdated(lowerPos, lowerState, lowerState, 2)
            }
            if (upperState.`is`(ModBlocks.GALE_TANK)) {
                lvl.sendBlockUpdated(upperPos, upperState, upperState, 2)
            }
        }
    }

    override fun setChanged() {
        super.setChanged()
        getLowerEntity()?.let {
            if (it !== this) it.setChanged()
        }
        syncBothHalves()
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        getWindStorage(null).save(output)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        windStorage.load(input)
        getLowerEntity()?.let {
            if (it !== this) {
                it.windStorage.load(input)
            }
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        val storage = getWindStorage(null)
        tag.putInt("wind_amount", storage.wind)
        tag.putInt("wind_capacity", storage.capacity)
        return tag
    }

    fun clientTick(level: Level, pos: BlockPos, state: BlockState) {
        val storage = getWindStorage(null)
        val wind = storage.wind
        val capacity = storage.capacity
        if (wind <= 0 || capacity <= 0) return

        val fillRatio = (wind.toFloat() / capacity.toFloat()).coerceIn(0.0f, 1.0f)
        if (fillRatio < 0.01f) return

        val cx = pos.x.toDouble() + 0.5
        val cz = pos.z.toDouble() + 0.5
        val baseY = pos.y.toDouble() + 0.32

        // Cadence légère et épurée (1 particule tous les 3 à 5 ticks selon le remplissage)
        val spawnChance = 0.12f + 0.30f * fillRatio
        if (level.random.nextFloat() >= spawnChance) return

        // Angle de départ de la spirale
        val startAngle = level.random.nextDouble() * Math.PI * 2.0
        val baseRadius = 0.12

        val px = cx + kotlin.math.cos(startAngle) * baseRadius
        val py = baseY
        val pz = cz + kotlin.math.sin(startAngle) * baseRadius

        // Vitesse angulaire proportionnelle au remplissage
        val rotSpeed = 0.14 + 0.14 * fillRatio.toDouble()
        val vx = -kotlin.math.sin(startAngle) * rotSpeed
        val vy = 0.040 + 0.025 * fillRatio.toDouble()
        val vz = kotlin.math.cos(startAngle) * rotSpeed

        level.addParticle(
            ModParticleTypes.STYLIZED_WIND,
            true, true,
            px, py, pz,
            vx, vy, vz
        )
    }
}
