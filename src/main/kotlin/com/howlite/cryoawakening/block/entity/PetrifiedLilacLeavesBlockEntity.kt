package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.item.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * PetrifiedLilacLeavesBlockEntity
 *
 * BlockEntity permettant le brossage progressif au Brush (Pinceau)
 * sur les couches de feuilles de lilas pétrifiées.
 */
class PetrifiedLilacLeavesBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.PETRIFIED_LILAC_LEAVES_BLOCK_ENTITY_TYPE, pos, state) {

    private var brushPulses: Int = 0

    fun brush(
        gameTime: Long,
        serverLevel: ServerLevel,
        entity: LivingEntity,
        direction: Direction,
        stack: ItemStack
    ): Boolean {
        brushPulses++

        val state = blockState
        val layers = if (state.hasProperty(SnowLayerBlock.LAYERS)) {
            state.getValue(SnowLayerBlock.LAYERS)
        } else {
            1
        }

        // Particules et sons intermédiaires de brossage
        serverLevel.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, state),
            worldPosition.x + 0.5,
            worldPosition.y + (layers * 2.0 / 16.0) / 2.0,
            worldPosition.z + 0.5,
            8,
            0.2,
            0.05,
            0.2,
            0.03
        )

        serverLevel.playSound(
            null,
            worldPosition,
            SoundEvents.BRUSH_GENERIC,
            SoundSource.BLOCKS,
            1.0f,
            1.0f
        )

        // Terminé après 3 pulsations (~1.5s de maintien du clic droit)
        if (brushPulses >= 3) {
            val minDrops = (layers + 1) / 2
            val maxDrops = layers
            val dropCount = if (minDrops >= maxDrops) minDrops else serverLevel.random.nextInt(maxDrops - minDrops + 1) + minDrops

            // Pop des feuilles fossilisées selon l'épaisseur
            val dropStack = ItemStack(ModItems.FOSSILIZED_LILAC_LEAF, dropCount)
            Block.popResource(serverLevel, worldPosition, dropStack)

            // Effets de complétion
            serverLevel.sendParticles(
                BlockParticleOption(ParticleTypes.BLOCK, state),
                worldPosition.x + 0.5,
                worldPosition.y + (layers * 2.0 / 16.0) / 2.0,
                worldPosition.z + 0.5,
                22 + layers * 3,
                0.3,
                0.1,
                0.3,
                0.05
            )

            serverLevel.playSound(
                null,
                worldPosition,
                SoundEvents.BRUSH_SAND_COMPLETED,
                SoundSource.BLOCKS,
                1.0f,
                1.0f
            )
            serverLevel.playSound(
                null,
                worldPosition,
                SoundEvents.GRASS_BREAK,
                SoundSource.BLOCKS,
                0.8f,
                0.9f
            )

            // Retrait du bloc
            serverLevel.removeBlock(worldPosition, false)
            return true
        }

        return false
    }
}
