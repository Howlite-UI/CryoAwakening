package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.block.GalePipeExhaustBlock
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * BlockEntity pour le Gale Pipe Exhaust (Échappement de Tuyau de Bourrasque).
 *
 * Fonctionnalités :
 * - Vitesse d'échappement réglable de 0 à 50 V/t (style Create mod).
 * - Aspiration automatique du vent depuis le bloc/tuyau arrière.
 * - Souffle physique propulsant les entités (joueurs, mobs, items) dans la direction du jet.
 * - Génération de flux de particules d'air comprimé.
 * - Alimentation directe d'une machine placée en face si réceptrice.
 */
class GalePipeExhaustBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.GALE_PIPE_EXHAUST_BLOCK_ENTITY_TYPE, pos, state), IWindHolder {

    val windStorage = WindStorage(capacity = 500, maxReceive = 500, maxExtract = 50)

    var outputRate: Int = 0 // V/t (0..50, 0 par défaut)

    var prevVisualAngle: Float = 0.0f
    var visualAngle: Float = 0.0f

    companion object {
        const val MAX_OUTPUT_RATE = 50
        const val MIN_OUTPUT_RATE = 0

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, be: GalePipeExhaustBlockEntity) {
            be.tickServer(level, pos, state)
        }

        fun clientTick(level: Level, pos: BlockPos, state: BlockState, be: GalePipeExhaustBlockEntity) {
            be.tickClient()
        }
    }

    private fun tickClient() {
        prevVisualAngle = visualAngle
        val targetAngle = (outputRate.toFloat() / 50.0f) * 360.0f * 2.0f
        visualAngle += (targetAngle - visualAngle) * 0.35f
    }

    override fun getWindStorage(side: Direction?): WindStorage = windStorage

    fun setSpeed(newSpeed: Int) {
        val clamped = newSpeed.coerceIn(MIN_OUTPUT_RATE, MAX_OUTPUT_RATE)
        if (this.outputRate != clamped) {
            this.outputRate = clamped
            setChanged()
            level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
        }
    }

    private fun tickServer(level: Level, pos: BlockPos, state: BlockState) {
        val prevWind = windStorage.wind
        val facing = if (state.hasProperty(GalePipeExhaustBlock.FACING)) {
            state.getValue(GalePipeExhaustBlock.FACING)
        } else {
            Direction.NORTH
        }

        val backDir = facing.opposite
        val backPos = pos.relative(backDir)
        val backBe = level.getBlockEntity(backPos)

        // 1. Aspiration continue du vent depuis la face arrière (tuyau / réservoir)
        if (backBe is IWindHolder && windStorage.space > 0) {
            val backStorage = backBe.getWindStorage(facing)
            if (backStorage != null && backStorage.wind > 0) {
                val pullAmount = minOf(windStorage.space, maxOf(outputRate, 20), backStorage.wind)
                val extracted = backStorage.extractWind(pullAmount)
                if (extracted > 0) {
                    windStorage.receiveWind(extracted)
                    level.sendBlockUpdated(backPos, level.getBlockState(backPos), level.getBlockState(backPos), 2)
                }
            }
        }

        // 2. Échappement et propulsion si vitesse > 0 et vent disponible
        if (outputRate > 0 && windStorage.wind > 0) {
            val extractAmount = minOf(windStorage.wind, outputRate)
            val used = windStorage.extractWind(extractAmount)

            if (used > 0) {
                // A. Vérifier si une machine réceptrice est en face
                val frontPos = pos.relative(facing)
                val frontBe = level.getBlockEntity(frontPos)
                if (frontBe is IWindHolder) {
                    val frontStorage = frontBe.getWindStorage(backDir)
                    if (frontStorage != null && frontStorage.space > 0) {
                        val received = frontStorage.receiveWind(used)
                        if (used > received) {
                            windStorage.receiveWind(used - received)
                        }
                        level.sendBlockUpdated(frontPos, level.getBlockState(frontPos), level.getBlockState(frontPos), 2)
                    }
                } else {
                    // B. Émission du flux de vent dans le monde (physique + particules)
                    applyWindForceAndParticles(level as ServerLevel, pos, facing, used)
                }
            }
        }

        // 3. Synchronisation immédiate vers le client si la quantité de vent a varié
        if (windStorage.wind != prevWind) {
            setChanged()
            level.sendBlockUpdated(pos, state, state, 2)
        }
    }

    private fun applyWindForceAndParticles(serverLevel: ServerLevel, pos: BlockPos, facing: Direction, windPower: Int) {
        val powerRatio = (windPower.toDouble() / MAX_OUTPUT_RATE.toDouble()).coerceIn(0.1, 1.0)
        val range = 2.5 + powerRatio * 6.5 // Portée de 3 à 9 blocs selon la vitesse
        val force = 0.03 + powerRatio * 0.12 // Force de propulsion

        val origin = Vec3(
            pos.x + 0.5 + facing.stepX * 0.6,
            pos.y + 0.5 + facing.stepY * 0.6,
            pos.z + 0.5 + facing.stepZ * 0.6
        )

        // Boîte d'effet du flux de vent
        val end = origin.add(
            facing.stepX * range,
            facing.stepY * range,
            facing.stepZ * range
        )

        val streamBox = AABB(
            minOf(origin.x - 0.6, end.x - 0.6),
            minOf(origin.y - 0.6, end.y - 0.6),
            minOf(origin.z - 0.6, end.z - 0.6),
            maxOf(origin.x + 0.6, end.x + 0.6),
            maxOf(origin.y + 0.6, end.y + 0.6),
            maxOf(origin.z + 0.6, end.z + 0.6)
        )

        val entities = serverLevel.getEntities(null as Entity?, streamBox)
        for (entity in entities) {
            if (entity.isSpectator) continue

            // Calcul de la distance le long du jet
            val dist = origin.distanceTo(entity.position())
            val falloff = (1.0 - (dist / range).coerceIn(0.0, 1.0)).coerceIn(0.2, 1.0)
            val appliedForce = force * falloff

            val pushX = facing.stepX * appliedForce
            val pushY = if (facing == Direction.UP) appliedForce * 1.2 else facing.stepY * appliedForce
            val pushZ = facing.stepZ * appliedForce

            entity.push(pushX, pushY, pushZ)
            entity.hurtMarked = true

            if (entity is Player && facing == Direction.UP) {
                entity.resetFallDistance()
            }
        }

        // Particules de souffle de vent stylisées personnalisées
        val particleCount = (powerRatio * 3).toInt().coerceAtLeast(1)
        val baseSpeed = 0.18 + powerRatio * 0.35

        for (i in 0 until particleCount) {
            val spreadX = (serverLevel.random.nextDouble() - 0.5) * 0.16
            val spreadY = (serverLevel.random.nextDouble() - 0.5) * 0.16
            val spreadZ = (serverLevel.random.nextDouble() - 0.5) * 0.16

            val spawnX = origin.x + spreadX
            val spawnY = origin.y + spreadY
            val spawnZ = origin.z + spreadZ

            val vx = facing.stepX * baseSpeed + (serverLevel.random.nextDouble() - 0.5) * 0.04
            val vy = facing.stepY * baseSpeed + (serverLevel.random.nextDouble() - 0.5) * 0.04
            val vz = facing.stepZ * baseSpeed + (serverLevel.random.nextDouble() - 0.5) * 0.04

            serverLevel.sendParticles(
                ModParticleTypes.STYLIZED_WIND,
                spawnX, spawnY, spawnZ,
                0,
                vx, vy, vz,
                1.0
            )
        }
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putInt("output_rate", outputRate)
        windStorage.save(output)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        outputRate = input.getInt("output_rate").orElse(0)
        windStorage.load(input)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("output_rate", outputRate)
        tag.putInt("wind_amount", windStorage.wind)
        tag.putInt("wind_capacity", windStorage.capacity)
        return tag
    }
}
