package com.howlite.cryoawakening.client.render.cave

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Types de voxels identifiés pour le diagramme isométrique de la grotte.
 */
enum class VoxelType(val isOre: Boolean, val priority: Int) {
    BENCH(false, 100),
    BISMUTH(true, 90),
    TELLURIUM(true, 85),
    DIAMOND(true, 80),
    EMERALD(true, 75),
    GOLD(true, 70),
    CRYO_VENT(false, 65),
    LUMESH(false, 60),
    REDSTONE(true, 55),
    LAPIS(true, 50),
    IRON(true, 45),
    COPPER(true, 40),
    COAL(true, 30),
    ICE(false, 20),
    WATER(false, 15),
    LAVA(false, 15),
    ROCK(false, 10),
    OTHER(false, 5)
}

/**
 * Représentation compacte d'un voxel exposé de la grotte.
 */
data class CaveVoxel(
    val relX: Byte,
    val relY: Byte,
    val relZ: Byte,
    val type: VoxelType,
    val baseColor: Int,
    val isFluid: Boolean
)

/**
 * Résultat complet d'un scan souterrain pour l'interface Ecosystem Bench.
 */
data class CaveScanResult(
    val benchPos: BlockPos,
    val voxels: List<CaveVoxel>,
    val airVolume: Int,
    val totalSolidVoxels: Int,
    val bismuthCount: Int,
    val telluriumCount: Int,
    val diamondCount: Int,
    val otherOreCount: Int,
    val waterCount: Int,
    val radiusXZ: Int,
    val radiusY: Int,
    val minRelY: Int,
    val maxRelY: Int
)

/**
 * Scanner de grotte client optimisé pour l'Ecosystem Bench.
 * Échantillonne les blocs et ne retient que les parois exposées (faces visibles de la caverne).
 */
object CaveVoxelScanner {

    private val NEIGHBOR_OFFSETS = arrayOf(
        BlockPos(0, 1, 0),
        BlockPos(0, -1, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(0, 0, -1)
    )

    fun scanCave(
        level: Level,
        center: BlockPos,
        radiusXZ: Int = 16,
        radiusY: Int = 12
    ): CaveScanResult {
        val voxelList = ArrayList<CaveVoxel>(2048)
        var airVolume = 0
        var bismuthCount = 0
        var telluriumCount = 0
        var diamondCount = 0
        var otherOreCount = 0
        var waterCount = 0
        var minRelY = 0
        var maxRelY = 0

        val mutablePos = BlockPos.MutableBlockPos()
        val neighborPos = BlockPos.MutableBlockPos()

        for (dy in -radiusY..radiusY) {
            for (dz in -radiusXZ..radiusXZ) {
                for (dx in -radiusXZ..radiusXZ) {
                    mutablePos.set(center.x + dx, center.y + dy, center.z + dz)
                    val state = level.getBlockState(mutablePos)

                    if (state.isAir) {
                        airVolume++
                        continue
                    }

                    // Vérifie si le bloc possède au moins une face exposée à l'air ou fluide
                    var isExposed = false
                    for (offset in NEIGHBOR_OFFSETS) {
                        neighborPos.set(mutablePos.x + offset.x, mutablePos.y + offset.y, mutablePos.z + offset.z)
                        val nState = level.getBlockState(neighborPos)
                        if (nState.isAir || !nState.isSolidRender) {
                            isExposed = true
                            break
                        }
                    }

                    // Le bloc central (Ecosystem Bench) est toujours inclus
                    if (dx == 0 && dy == 0 && dz == 0) {
                        isExposed = true
                    }

                    if (!isExposed) continue

                    val type = classifyBlock(state, dx == 0 && dy == 0 && dz == 0)
                    val color = getVoxelColor(type, state)
                    val isFluid = state.block is LiquidBlock

                    when (type) {
                        VoxelType.BISMUTH -> bismuthCount++
                        VoxelType.TELLURIUM -> telluriumCount++
                        VoxelType.DIAMOND -> diamondCount++
                        VoxelType.GOLD, VoxelType.EMERALD, VoxelType.IRON,
                        VoxelType.COPPER, VoxelType.REDSTONE, VoxelType.LAPIS, VoxelType.COAL -> otherOreCount++
                        VoxelType.WATER -> waterCount++
                        else -> {}
                    }

                    if (dy < minRelY) minRelY = dy
                    if (dy > maxRelY) maxRelY = dy

                    voxelList.add(
                        CaveVoxel(
                            relX = dx.toByte(),
                            relY = dy.toByte(),
                            relZ = dz.toByte(),
                            type = type,
                            baseColor = color,
                            isFluid = isFluid
                        )
                    )
                }
            }
        }

        return CaveScanResult(
            benchPos = center,
            voxels = voxelList,
            airVolume = airVolume,
            totalSolidVoxels = voxelList.size,
            bismuthCount = bismuthCount,
            telluriumCount = telluriumCount,
            diamondCount = diamondCount,
            otherOreCount = otherOreCount,
            waterCount = waterCount,
            radiusXZ = radiusXZ,
            radiusY = radiusY,
            minRelY = minRelY,
            maxRelY = maxRelY
        )
    }

    private fun classifyBlock(state: BlockState, isCenter: Boolean): VoxelType {
        if (isCenter || state.`is`(ModBlocks.ECOSYSTEM_BENCH)) return VoxelType.BENCH
        if (state.`is`(ModBlocks.BISMUTH_ORE_SHIVERING_SHALE)) return VoxelType.BISMUTH
        if (state.`is`(ModBlocks.TELLURIUM_ORE)) return VoxelType.TELLURIUM
        if (state.`is`(ModBlocks.CRYO_VENT)) return VoxelType.CRYO_VENT
        if (state.`is`(ModBlocks.LUMESH_STEM) || state.`is`(ModBlocks.ORANGE_LUMESH) || state.`is`(ModBlocks.YELLOW_LUMESH)) return VoxelType.LUMESH

        val block = state.block
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return VoxelType.DIAMOND
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return VoxelType.EMERALD
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return VoxelType.GOLD
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return VoxelType.IRON
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return VoxelType.COPPER
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return VoxelType.REDSTONE
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return VoxelType.LAPIS
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return VoxelType.COAL

        if (block == Blocks.WATER) return VoxelType.WATER
        if (block == Blocks.LAVA) return VoxelType.LAVA
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return VoxelType.ICE

        if (state.`is`(ModBlocks.ICE_SHEET_SHIVERING_SHALE_STONE) ||
            state.`is`(ModBlocks.BLUE_ICE_SHEET_SHIVERING_SHALE_STONE) ||
            state.`is`(ModBlocks.ICE_SHEET_GABBRO_STONE)) {
            return VoxelType.ICE
        }

        return VoxelType.ROCK
    }

    private fun getVoxelColor(type: VoxelType, state: BlockState): Int {
        return when (type) {
            VoxelType.BENCH -> 0xFFF59E0B.toInt()       // Ambre lumineux doré
            VoxelType.BISMUTH -> 0xFFFF2A85.toInt()     // Rose/magenta électrique vibrant
            VoxelType.TELLURIUM -> 0xFFFF9E0B.toInt()   // Ambre solaire chaud
            VoxelType.DIAMOND -> 0xFF00E5FF.toInt()     // Cyan diamant pur
            VoxelType.EMERALD -> 0xFF10B981.toInt()     // Émeraude vive
            VoxelType.GOLD -> 0xFFFFD700.toInt()        // Or éclatant
            VoxelType.IRON -> 0xFFE2E8F0.toInt()        // Métallique clair
            VoxelType.COPPER -> 0xFFF97316.toInt()      // Cuivre oxydé chaud
            VoxelType.REDSTONE -> 0xFFEF4444.toInt()    // Rouge rubis
            VoxelType.LAPIS -> 0xFF3B82F6.toInt()       // Bleu saphir
            VoxelType.COAL -> 0xFF475569.toInt()        // Ardoise anthracite
            VoxelType.CRYO_VENT -> 0xFF38BDF8.toInt()   // Bleu givre actif
            VoxelType.LUMESH -> 0xFF22C55E.toInt()      // Vert bioluminescent
            VoxelType.WATER -> 0x880284C7.toInt()       // Bleu turquoise aquatique
            VoxelType.ICE -> 0xCC7DD3FC.toInt()         // Glace cristalline
            VoxelType.LAVA -> 0xEEF97316.toInt()        // Lave en fusion
            VoxelType.ROCK -> {
                if (state.`is`(ModBlocks.BLUE_FROZEN_FLYSCH)) {
                    0xFF1E3A5F.toInt()
                } else if (state.`is`(ModBlocks.FROZEN_FLYSCH)) {
                    0xFF264653.toInt()
                } else if (state.`is`(ModBlocks.SHIVERING_SHALE_STONE)) {
                    0xFF1E293B.toInt()
                } else if (state.`is`(ModBlocks.GABBRO_STONE)) {
                    0xFF0F172A.toInt()
                } else if (state.block == Blocks.DEEPSLATE) {
                    0xFF1C222D.toInt()
                } else {
                    0xFF283648.toInt() // Ardoise blueprint classique
                }
            }
            VoxelType.OTHER -> 0xFF334155.toInt()
        }
    }
}
