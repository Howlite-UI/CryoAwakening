package com.howlite.cryoawakening.block

import com.howlite.cryoawakening.block.entity.PetrifiedLilacLeavesBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * PetrifiedLilacLeavesBlock
 *
 * Bloc de feuilles de lilas pétrifié en couches superposables (1 à 8 couches).
 * - Ne fond jamais avec la lumière ou la chaleur.
 * - Ne drop RIEN s'il est cassé normalement (à la main ou avec un outil).
 * - Lorsqu'un joueur maintient un Brush (Pinceau) dessus, il se brosse progressivement
 *   pendant ~1.5s avant de se désagréger et de faire tomber des 'Fossilized Lilac Leaf'.
 */
class PetrifiedLilacLeavesBlock(properties: Properties) : SnowLayerBlock(properties), EntityBlock {

    companion object {
        val SHAPES: Array<VoxelShape> = Array(9) { i ->
            Block.box(0.0, 0.0, 0.0, 16.0, (i * 2.0).coerceAtLeast(2.0), 16.0)
        }
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val layers = if (state.hasProperty(LAYERS)) state.getValue(LAYERS) else 1
        return SHAPES[layers]
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val layers = if (state.hasProperty(LAYERS)) state.getValue(LAYERS) else 1
        // Forme de collision physique de 2 pixels même pour 1 seule couche,
        // permettant au raycast du Brush de cibler le tas sans traverser vers le bloc inférieur !
        return SHAPES[layers]
    }

    override fun randomTick(
        state: BlockState,
        level: ServerLevel,
        pos: BlockPos,
        random: RandomSource
    ) {
        // Ne fond jamais avec la lumière ou la chaleur !
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PetrifiedLilacLeavesBlockEntity(pos, state)
    }
}
