package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.ModSounds
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import kotlin.math.cos
import kotlin.math.sin

/**
 * BlockEntity pour le CryoVentBlock.
 * - Générateur d'énergie "Vent" : produit 5 V/t en continu jusqu'à 2000 V.
 * - Synchronisation réseau temps réel avec les clients.
 * - Émet les particules de vent en entonnoir et joue le son softwind.ogg.
 */
class CryoVentBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.CRYO_VENT_BLOCK_ENTITY_TYPE, pos, state), IWindHolder {

    val windStorage = WindStorage(capacity = 2000, maxExtract = 50, maxReceive = 50)

    override fun getWindStorage(side: Direction?): WindStorage = windStorage

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

    companion object {
        const val WIND_GENERATION_PER_TICK = 5

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: CryoVentBlockEntity) {
            // Production continue de Vent
            if (blockEntity.windStorage.wind < blockEntity.windStorage.capacity) {
                blockEntity.windStorage.receiveWind(WIND_GENERATION_PER_TICK)
                blockEntity.setChanged()
                level.sendBlockUpdated(pos, state, state, 2)
            }
        }

        fun clientTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: CryoVentBlockEntity) {
            if (!level.isClientSide) return

            val random = level.random

            // Émission des traînées de vent épurées (1 toute les ~15 ticks)
            if (random.nextInt(15) == 0) {
                val cx = pos.x.toDouble() + 0.5
                val cy = pos.y.toDouble() + 1.02
                val cz = pos.z.toDouble() + 0.5

                val angle = random.nextDouble() * Math.PI * 2.0
                val radius = 0.08 + random.nextDouble() * 0.18

                val x = cx + cos(angle) * radius
                val z = cz + sin(angle) * radius

                val funnelSpeed = 0.035 + random.nextDouble() * 0.02
                val vx = cos(angle) * funnelSpeed
                val vy = 0.13 + random.nextDouble() * 0.03
                val vz = sin(angle) * funnelSpeed

                level.addParticle(
                    ModParticleTypes.STYLIZED_WIND,
                    true, true,
                    x, cy, z,
                    vx, vy, vz
                )
            }

            // Jouer le son d'ambiance softwind.ogg périodiquement
            if (random.nextInt(110) == 0) {
                level.playLocalSound(
                    pos.x.toDouble() + 0.5,
                    pos.y.toDouble() + 0.5,
                    pos.z.toDouble() + 0.5,
                    ModSounds.CRYO_VENT_AMBIENT,
                    SoundSource.BLOCKS,
                    0.45f,
                    0.92f + random.nextFloat() * 0.16f,
                    false
                )
            }
        }
    }
}
