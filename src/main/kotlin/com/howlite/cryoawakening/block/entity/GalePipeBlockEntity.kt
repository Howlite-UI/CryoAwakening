package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.block.GalePipeBlock
import com.howlite.cryoawakening.block.PipeConnectionState
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.ArrayDeque
import kotlin.math.min

/**
 * BlockEntity pour le Gale Pipe.
 * Gère le transport d'énergie "Vent" (Volume) à travers le réseau de tuyaux connecté.
 */
class GalePipeBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.GALE_PIPE_BLOCK_ENTITY_TYPE, pos, state) {

    companion object {
        const val MAX_TRANSFER_PER_TICK = 20
        const val MAX_NETWORK_SEARCH_DEPTH = 64

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: GalePipeBlockEntity) {
            // Rechercher chaque branche configurée en mode EXTRACTION
            for (dir in Direction.entries) {
                val prop = GalePipeBlock.PROPERTY_BY_DIRECTION[dir] ?: continue
                if (state.getValue(prop) == PipeConnectionState.EXTRACT) {
                    val sourcePos = pos.relative(dir)
                    val sourceBe = level.getBlockEntity(sourcePos) as? IWindHolder ?: continue
                    val sourceStorage = sourceBe.getWindStorage(dir.opposite) ?: continue

                    if (sourceStorage.wind <= 0) continue

                    // Trouver les destinations connectées via le réseau de tuyaux
                    val destinations = findDestinations(level, pos, dir)
                    if (destinations.isEmpty()) continue

                    var remainingToExtract = min(MAX_TRANSFER_PER_TICK, sourceStorage.wind)

                    for ((destBe, destSide) in destinations) {
                        if (remainingToExtract <= 0) break
                        val destStorage = destBe.getWindStorage(destSide) ?: continue
                        if (destStorage === sourceStorage || destStorage.isFull) continue

                        val toTransfer = min(remainingToExtract, destStorage.space)
                        if (toTransfer > 0) {
                            val extracted = sourceStorage.extractWind(toTransfer)
                            val received = destStorage.receiveWind(extracted)
                            remainingToExtract -= received

                            if (sourceBe is BlockEntity) {
                                sourceBe.setChanged()
                                level.sendBlockUpdated(sourceBe.blockPos, level.getBlockState(sourceBe.blockPos), level.getBlockState(sourceBe.blockPos), 2)
                            }
                            if (destBe is BlockEntity) {
                                destBe.setChanged()
                                level.sendBlockUpdated(destBe.blockPos, level.getBlockState(destBe.blockPos), level.getBlockState(destBe.blockPos), 2)
                            }
                        }
                    }
                }
            }
        }

        private fun findDestinations(
            level: Level,
            startPipePos: BlockPos,
            extractionDirection: Direction
        ): List<Pair<IWindHolder, Direction>> {
            val results = ArrayList<Pair<IWindHolder, Direction>>()
            val visited = HashSet<BlockPos>()
            val queue = ArrayDeque<BlockPos>()

            queue.add(startPipePos)
            visited.add(startPipePos)

            while (queue.isNotEmpty() && visited.size < MAX_NETWORK_SEARCH_DEPTH) {
                val currentPos = queue.poll()
                val currentState = level.getBlockState(currentPos)
                if (currentState.block !is GalePipeBlock) continue

                for (dir in Direction.entries) {
                    val prop = GalePipeBlock.PROPERTY_BY_DIRECTION[dir] ?: continue
                    val conn = currentState.getValue(prop)
                    if (!conn.isConnected()) continue

                    // Ne pas ré-injecter directement vers la source de départ
                    if (currentPos == startPipePos && dir == extractionDirection) continue

                    val neighborPos = currentPos.relative(dir)
                    val neighborState = level.getBlockState(neighborPos)

                    if (neighborState.block is GalePipeBlock) {
                        // Continuer l'exploration à travers le tuyau voisin si la connexion opposée est active
                        val neighborProp = GalePipeBlock.PROPERTY_BY_DIRECTION[dir.opposite]
                        if (neighborProp != null && neighborState.getValue(neighborProp).isConnected()) {
                            if (visited.add(neighborPos)) {
                                queue.add(neighborPos)
                            }
                        }
                    } else {
                        // Machine voisine candidate à l'injection
                        val neighborBe = level.getBlockEntity(neighborPos) as? IWindHolder
                        if (neighborBe != null && conn == PipeConnectionState.NORMAL) {
                            results.add(Pair(neighborBe, dir.opposite))
                        }
                    }
                }
            }

            return results
        }
    }
}
