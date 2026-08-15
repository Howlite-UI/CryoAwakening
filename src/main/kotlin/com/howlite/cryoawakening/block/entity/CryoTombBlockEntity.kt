package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB

/**
 * CryoTombBlockEntity - Entité de bloc attachée à la partie inférieure (LOWER) de la Cryo-Tomb.
 *
 * Gère le stockage dynamique du type de mob emprisonné (Zombie par défaut, modifiable par Spawn Egg),
 * la détection de joueur dans un rayon de 5 blocs, la progression du dégel, les effets sonores/particules,
 * et la libération de l'entité configurée.
 */
class CryoTombBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.CRYO_TOMB_BLOCK_ENTITY_TYPE, pos, state) {

    var thawProgress: Int = 0
    var entityTypeId: Identifier = Identifier.fromNamespaceAndPath("minecraft", "zombie")

    companion object {
        const val MAX_THAW_PROGRESS: Int = 60
        const val DETECTION_RADIUS: Double = 5.0

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: CryoTombBlockEntity) {
            if (level !is ServerLevel) return

            val searchBox = AABB(pos).inflate(DETECTION_RADIUS)
            val nearbyPlayers = level.getEntitiesOfClass(Player::class.java, searchBox) { player ->
                !player.isSpectator
            }

            if (nearbyPlayers.isNotEmpty()) {
                // Un joueur est dans la zone : la glace se fissure progressivement
                blockEntity.thawProgress++
                blockEntity.setChanged()

                // Joue occasionnellement un son de fissure et émet des particules de neige
                if (blockEntity.thawProgress % 15 == 0) {
                    level.playSound(
                        null,
                        pos,
                        SoundEvents.GLASS_BREAK,
                        SoundSource.BLOCKS,
                        0.35f,
                        0.5f + (level.random.nextFloat() * 0.2f)
                    )

                    level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        pos.x + 0.5,
                        pos.y + 1.0,
                        pos.z + 0.5,
                        6,
                        0.35, 0.7, 0.35,
                        0.02
                    )
                }

                // Seuil atteint : la glace éclate et libère le monstre !
                if (blockEntity.thawProgress >= MAX_THAW_PROGRESS) {
                    level.playSound(
                        null,
                        pos,
                        SoundEvents.GLASS_BREAK,
                        SoundSource.BLOCKS,
                        1.2f,
                        0.75f
                    )
                    level.playSound(
                        null,
                        pos,
                        SoundEvents.GLASS_FALL,
                        SoundSource.BLOCKS,
                        1.0f,
                        0.9f
                    )

                    // Effets de particules intenses
                    level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        pos.x + 0.5,
                        pos.y + 1.0,
                        pos.z + 0.5,
                        40,
                        0.5, 0.9, 0.5,
                        0.1
                    )
                    level.sendParticles(
                        ParticleTypes.ITEM_SNOWBALL,
                        pos.x + 0.5,
                        pos.y + 1.0,
                        pos.z + 0.5,
                        25,
                        0.4, 0.8, 0.4,
                        0.08
                    )

                    // Remplacement des deux moitiés par Blocks.AIR
                    val upperPos = pos.above()
                    level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)

                    // =========================================================================
                    // SPAWN DU MONSTRE CONFIGURÉ
                    // =========================================================================
                    val targetType = blockEntity.getEntityType()
                    val monster = targetType.spawn(
                        level,
                        pos,
                        net.minecraft.world.entity.EntitySpawnReason.TRIGGERED
                    )
                    if (monster != null) {
                        monster.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
                        monster.yRot = level.random.nextFloat() * 360.0f
                    }
                }
            } else {
                // Si le joueur s'éloigne, le compteur redescend lentement
                if (blockEntity.thawProgress > 0) {
                    blockEntity.thawProgress--
                    blockEntity.setChanged()
                }
            }
        }
    }

    fun getEntityType(): EntityType<*> {
        val opt = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId)
        if (opt.isPresent) {
            return opt.get().value()
        }
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "zombie"))
    }

    fun setEntityType(type: EntityType<*>) {
        this.entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(type)
        setChanged()
        level?.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_ALL)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putInt("thaw_progress", thawProgress)
        output.putString("entity_type", entityTypeId.toString())
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        thawProgress = input.getInt("thaw_progress").orElse(0)
        val savedEntity = input.getString("entity_type").orElse("")
        if (savedEntity.isNotEmpty()) {
            val parsed = Identifier.tryParse(savedEntity)
            if (parsed != null) {
                entityTypeId = parsed
            }
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("thaw_progress", thawProgress)
        tag.putString("entity_type", entityTypeId.toString())
        return tag
    }
}
