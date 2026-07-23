package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.cos
import kotlin.math.sin

/**
 * BlockEntity pour le CryoVentBlock.
 *
 * Émet les particules de vent en entonnoir et joue le son d'ambiance softwind.ogg
 * lorsque le joueur se trouve à proximité du bloc.
 */
class CryoVentBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.CRYO_VENT_BLOCK_ENTITY_TYPE, pos, state) {

    companion object {
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

            // Jouer le son d'ambiance softwind.ogg périodiquement (vent doux immersif)
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
