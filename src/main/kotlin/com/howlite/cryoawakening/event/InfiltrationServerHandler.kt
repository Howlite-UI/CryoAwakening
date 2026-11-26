package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.infiltration.InfiltrationMath
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.network.InfiltrationActionPayload
import com.howlite.cryoawakening.network.InfiltrationSyncPayload
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * InfiltrationServerHandler
 *
 * Gère l'état autoritaire côté serveur de la Perle d'Infiltration :
 * 1. Validation du plafond et de la surface libre au-dessus.
 * 2. Déplacement fluide sans collision ni dégâts de suffocation :
 *    RISING_TO_CEILING -> SWIMMING_THROUGH -> PEEKING -> (EMERGING | RETURNING) -> IDLE.
 * 3. Consommation d'1 seule perle uniquement à l'émergence finale (CONFIRM_EMERGE).
 *    Annuler en phase PEEKING ramène le joueur à sa position d'origine sans consommer la perle.
 */
object InfiltrationServerHandler {

    data class Session(
        var phase: InfiltrationPhase,
        val originX: Double,
        val originY: Double,
        val originZ: Double,
        val targetX: Double,
        val targetZ: Double,
        val ceilingHitY: Double,
        val exitFloorY: Double,
        val usedHand: InteractionHand,
        var phaseTick: Int = 0,
        var phaseDuration: Int = 1
    )

    private val sessions: MutableMap<UUID, Session> = ConcurrentHashMap()

    @JvmStatic
    fun isPlayerInfiltrating(player: Player): Boolean {
        val session = sessions[player.uuid] ?: return false
        return session.phase.isActiveSequence()
    }

    fun getSession(player: Player): Session? = sessions[player.uuid]

    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(
            InfiltrationActionPayload.ID,
            InfiltrationActionPayload.CODEC
        )
        PayloadTypeRegistry.clientboundPlay().register(
            InfiltrationSyncPayload.ID,
            InfiltrationSyncPayload.CODEC
        )

        ServerPlayNetworking.registerGlobalReceiver(InfiltrationActionPayload.ID) { payload, context ->
            val player = context.player()
            context.server().execute {
                handleAction(player, payload.action)
            }
        }

        // Tick serveur pour animer la traversée de tous les joueurs en infiltration
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val iterator = sessions.entries.iterator()
            while (iterator.hasNext()) {
                val (uuid, session) = iterator.next()
                val player = server.playerList.getPlayer(uuid)
                if (player == null || player.isRemoved || !player.isAlive) {
                    if (player != null) {
                        restorePlayerPhysics(player)
                    }
                    iterator.remove()
                    continue
                }
                val finished = tickSession(player, session)
                if (finished) {
                    restorePlayerPhysics(player)
                    iterator.remove()
                    syncToClient(player, InfiltrationPhase.IDLE, session)
                }
            }
        }

        // Immunité à la suffocation, chute et impacts muraux pendant toute la séquence
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, _ ->
            if (entity is ServerPlayer) {
                val session = sessions[entity.uuid]
                if (session != null && session.phase.isActiveSequence()) {
                    if (source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                        return@register true
                    }
                    // Totalement invulnérable dans les blocs et à la chute/suffocation
                    if (session.phase.isInsideBlocks() ||
                        session.phase == InfiltrationPhase.RISING_TO_CEILING ||
                        session.phase == InfiltrationPhase.EMERGING ||
                        source.`is`(DamageTypes.IN_WALL) ||
                        source.`is`(DamageTypes.FALL) ||
                        source.`is`(DamageTypes.FLY_INTO_WALL)
                    ) {
                        return@register false
                    }
                }
            }
            true
        }

        // Nettoyage propre à la déconnexion
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            val session = sessions.remove(player.uuid)
            if (session != null) {
                restorePlayerPhysics(player)
                if (session.phase == InfiltrationPhase.SWIMMING_THROUGH ||
                    session.phase == InfiltrationPhase.PEEKING ||
                    session.phase == InfiltrationPhase.RETURNING
                ) {
                    // Remettre le joueur à sa position d'origine sécurisée s'il se déconnecte dans un bloc
                    player.setPos(session.originX, session.originY, session.originZ)
                }
            }
        }
    }

    private fun handleAction(player: ServerPlayer, action: Int) {
        when (action) {
            InfiltrationActionPayload.ACTION_START_ASCEND -> startAscend(player)
            InfiltrationActionPayload.ACTION_CONFIRM_EMERGE -> confirmEmerge(player)
            InfiltrationActionPayload.ACTION_CANCEL_RETURN -> cancelAndReturn(player)
        }
    }

    private fun startAscend(player: ServerPlayer) {
        if (sessions.containsKey(player.uuid)) return
        if (player.isPassenger || player.isSleeping) return

        val hand = findPearlHand(player) ?: return
        val level = player.level()
        val target = InfiltrationMath.findTarget(level, player)

        if (!target.valid) {
            // Rejet propre : renvoie IDLE au client
            syncIdle(player)
            return
        }

        val riseTicks = InfiltrationMath.computeRiseTicks(player.y, target.ceilingHitY)
        val session = Session(
            phase = InfiltrationPhase.RISING_TO_CEILING,
            originX = player.x,
            originY = player.y,
            originZ = player.z,
            targetX = target.columnX,
            targetZ = target.columnZ,
            ceilingHitY = target.ceilingHitY,
            exitFloorY = target.exitFloorY,
            usedHand = hand,
            phaseTick = 0,
            phaseDuration = riseTicks
        )
        sessions[player.uuid] = session

        player.noPhysics = true
        player.isNoGravity = true
        player.deltaMovement = Vec3.ZERO
        player.fallDistance = 0.0

        // Son cristallin + impulsion d'envol
        level.playSound(
            null,
            player.x, player.y, player.z,
            SoundEvents.BEACON_ACTIVATE,
            SoundSource.PLAYERS,
            0.75f,
            1.55f
        )
        level.playSound(
            null,
            player.x, player.y, player.z,
            SoundEvents.TRIDENT_RIPTIDE_2.value(),
            SoundSource.PLAYERS,
            0.55f,
            1.45f
        )

        spawnRingParticles(level, player.x, player.y + 0.15, player.z, 0.9, 18)
        syncToClient(player, session.phase, session)
    }

    private fun confirmEmerge(player: ServerPlayer) {
        val session = sessions[player.uuid] ?: return
        if (session.phase != InfiltrationPhase.PEEKING) return

        // Consommer 1 Perle d'Infiltration (sauf en Créatif)
        if (!player.abilities.instabuild) {
            val consumed = consumePearl(player, session.usedHand)
            if (!consumed) {
                // Si la perle a disparu d'une manière ou d'une autre, annuler vers l'origine
                cancelAndReturn(player)
                return
            }
        }

        session.phase = InfiltrationPhase.EMERGING
        session.phaseTick = 0
        session.phaseDuration = InfiltrationMath.EMERGE_TICKS

        val level = player.level()
        level.playSound(
            null,
            session.targetX, session.exitFloorY, session.targetZ,
            SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
            SoundSource.PLAYERS,
            0.55f,
            1.45f
        )
        level.playSound(
            null,
            session.targetX, session.exitFloorY, session.targetZ,
            SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS,
            1.0f,
            1.35f
        )
        spawnRingParticles(level, session.targetX, session.exitFloorY + 0.1, session.targetZ, 1.1, 28)

        syncToClient(player, session.phase, session)
    }

    private fun cancelAndReturn(player: ServerPlayer) {
        val session = sessions[player.uuid] ?: return
        if (session.phase != InfiltrationPhase.PEEKING) return

        session.phase = InfiltrationPhase.RETURNING
        session.phaseTick = 0
        session.phaseDuration = InfiltrationMath.computeReturnTicks(session.exitFloorY, session.originY)

        val level = player.level()
        level.playSound(
            null,
            session.targetX, session.exitFloorY, session.targetZ,
            SoundEvents.BEACON_DEACTIVATE,
            SoundSource.PLAYERS,
            0.7f,
            1.25f
        )
        spawnRingParticles(level, session.targetX, session.exitFloorY + 0.1, session.targetZ, 0.8, 16)

        syncToClient(player, session.phase, session)
    }

    private fun tickSession(player: ServerPlayer, session: Session): Boolean {
        val level = player.level()
        player.noPhysics = true
        player.isNoGravity = true
        player.fallDistance = 0.0
        player.deltaMovement = Vec3.ZERO

        session.phaseTick++
        val t = (session.phaseTick.toDouble() / session.phaseDuration.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)

        when (session.phase) {
            InfiltrationPhase.RISING_TO_CEILING -> {
                // Courbe d'accélération douce vers le plafond (easeInQuad)
                val easedY = t * t
                val easedXZ = 1.0 - (1.0 - t) * (1.0 - t)
                val enterY = session.ceilingHitY - 1.35
                val curX = session.originX + (session.targetX - session.originX) * easedXZ
                val curZ = session.originZ + (session.targetZ - session.originZ) * easedXZ
                val curY = session.originY + (enterY - session.originY) * easedY

                movePlayerAuthoritatively(player, curX, curY, curZ)

                // Particules tourbillonnantes pendant l'envol
                if (session.phaseTick % 2 == 0) {
                    val angle = session.phaseTick * 0.7
                    level.sendParticles(
                        ParticleTypes.GLOW,
                        curX + cos(angle) * 0.55, curY + 0.4, curZ + sin(angle) * 0.55,
                        2, 0.05, 0.08, 0.05, 0.01
                    )
                }

                if (session.phaseTick >= session.phaseDuration) {
                    // Entrée dans le plafond !
                    session.phase = InfiltrationPhase.SWIMMING_THROUGH
                    session.phaseTick = 0
                    session.phaseDuration = InfiltrationMath.computeSwimTicks(session.ceilingHitY, session.exitFloorY)

                    level.playSound(
                        null,
                        session.targetX, session.ceilingHitY, session.targetZ,
                        SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                        SoundSource.PLAYERS,
                        0.9f,
                        1.35f
                    )
                    level.playSound(
                        null,
                        session.targetX, session.ceilingHitY, session.targetZ,
                        SoundEvents.AMETHYST_CLUSTER_BREAK,
                        SoundSource.PLAYERS,
                        0.6f,
                        1.4f
                    )
                    spawnRingParticles(level, session.targetX, session.ceilingHitY - 0.1, session.targetZ, 1.0, 24)
                    syncToClient(player, session.phase, session)
                }
            }

            InfiltrationPhase.SWIMMING_THROUGH -> {
                val startSwimY = session.ceilingHitY - 1.35
                val peekY = session.exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                // Courbe fluide (smoothstep léger) à travers la roche
                val eased = t * t * (3.0 - 2.0 * t)
                val curY = startSwimY + (peekY - startSwimY) * eased

                movePlayerAuthoritatively(player, session.targetX, curY, session.targetZ)

                if (session.phaseTick % 6 == 0) {
                    level.playSound(
                        null,
                        session.targetX, curY, session.targetZ,
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        0.35f,
                        0.9f + (t.toFloat() * 0.7f)
                    )
                }

                if (session.phaseTick >= session.phaseDuration) {
                    // Arrivée à la surface -> phase PEEKING !
                    session.phase = InfiltrationPhase.PEEKING
                    session.phaseTick = 0
                    session.phaseDuration = 1

                    movePlayerAuthoritatively(player, session.targetX, peekY, session.targetZ)

                    level.playSound(
                        null,
                        session.targetX, session.exitFloorY, session.targetZ,
                        SoundEvents.CONDUIT_ACTIVATE,
                        SoundSource.PLAYERS,
                        0.65f,
                        1.5f
                    )
                    spawnRingParticles(level, session.targetX, session.exitFloorY + 0.08, session.targetZ, 0.9, 22)
                    syncToClient(player, session.phase, session)
                }
            }

            InfiltrationPhase.PEEKING -> {
                val peekY = session.exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                movePlayerAuthoritatively(player, session.targetX, peekY, session.targetZ)

                // Légères particules autour du portail au ras du sol
                if (session.phaseTick % 8 == 0) {
                    val angle = session.phaseTick * 0.45
                    level.sendParticles(
                        ParticleTypes.GLOW,
                        session.targetX + cos(angle) * 0.65,
                        session.exitFloorY + 0.08,
                        session.targetZ + sin(angle) * 0.65,
                        1, 0.02, 0.03, 0.02, 0.005
                    )
                }
            }

            InfiltrationPhase.EMERGING -> {
                val peekY = session.exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                // Petit bond élégant au-dessus du sol puis atterrissage net sur exitFloorY
                val arcY = if (t < 0.65) {
                    val u = t / 0.65
                    val easeOut = 1.0 - (1.0 - u) * (1.0 - u)
                    peekY + (session.exitFloorY + 0.48 - peekY) * easeOut
                } else {
                    val u = (t - 0.65) / 0.35
                    val easeIn = u * u
                    (session.exitFloorY + 0.48) + (session.exitFloorY - (session.exitFloorY + 0.48)) * easeIn
                }

                movePlayerAuthoritatively(player, session.targetX, arcY, session.targetZ)

                if (session.phaseTick >= session.phaseDuration) {
                    movePlayerAuthoritatively(player, session.targetX, session.exitFloorY + 0.02, session.targetZ)
                    player.deltaMovement = Vec3(0.0, 0.05, 0.0)
                    player.hurtMarked = true
                    return true
                }
            }

            InfiltrationPhase.RETURNING -> {
                val peekY = session.exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                val eased = t * t * (3.0 - 2.0 * t)
                val curX = session.targetX + (session.originX - session.targetX) * eased
                val curZ = session.targetZ + (session.originZ - session.targetZ) * eased
                val curY = peekY + (session.originY - peekY) * eased

                movePlayerAuthoritatively(player, curX, curY, curZ)

                if (session.phaseTick >= session.phaseDuration) {
                    movePlayerAuthoritatively(player, session.originX, session.originY + 0.02, session.originZ)
                    level.playSound(
                        null,
                        session.originX, session.originY, session.originZ,
                        SoundEvents.BREEZE_LAND,
                        SoundSource.PLAYERS,
                        0.65f,
                        1.15f
                    )
                    spawnRingParticles(level, session.originX, session.originY + 0.15, session.originZ, 0.85, 16)
                    return true
                }
            }

            else -> return true
        }

        return false
    }

    private fun movePlayerAuthoritatively(player: ServerPlayer, x: Double, y: Double, z: Double) {
        player.setPos(x, y, z)
        player.connection.resetPosition()
    }

    private fun restorePlayerPhysics(player: ServerPlayer) {
        player.noPhysics = player.isSpectator
        player.isNoGravity = false
        player.fallDistance = 0.0
        player.hurtMarked = true
    }

    private fun findPearlHand(player: Player): InteractionHand? {
        if (player.mainHandItem.`is`(ModItems.INFILTRATION_PEARL)) return InteractionHand.MAIN_HAND
        if (player.offhandItem.`is`(ModItems.INFILTRATION_PEARL)) return InteractionHand.OFF_HAND
        return null
    }

    private fun consumePearl(player: ServerPlayer, preferredHand: InteractionHand): Boolean {
        val preferred = player.getItemInHand(preferredHand)
        if (preferred.`is`(ModItems.INFILTRATION_PEARL)) {
            preferred.shrink(1)
            return true
        }
        val otherHand = if (preferredHand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        val other = player.getItemInHand(otherHand)
        if (other.`is`(ModItems.INFILTRATION_PEARL)) {
            other.shrink(1)
            return true
        }
        // Fallback : chercher dans tout l'inventaire
        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (stack.`is`(ModItems.INFILTRATION_PEARL)) {
                stack.shrink(1)
                return true
            }
        }
        return false
    }

    private fun spawnRingParticles(level: ServerLevel, cx: Double, cy: Double, cz: Double, radius: Double, count: Int) {
        for (i in 0 until count) {
            val a = (i.toDouble() / count.toDouble()) * PI * 2.0
            val px = cx + cos(a) * radius
            val pz = cz + sin(a) * radius
            level.sendParticles(
                ParticleTypes.GLOW,
                px, cy, pz,
                1, 0.02, 0.04, 0.02, 0.01
            )
            if (i % 3 == 0) {
                level.sendParticles(
                    ParticleTypes.END_ROD,
                    px, cy, pz,
                    1, 0.0, 0.05, 0.0, 0.015
                )
            }
        }
    }

    private fun syncToClient(player: ServerPlayer, phase: InfiltrationPhase, session: Session) {
        ServerPlayNetworking.send(
            player,
            InfiltrationSyncPayload(
                phaseId = phase.id,
                originX = session.originX,
                originY = session.originY,
                originZ = session.originZ,
                targetX = session.targetX,
                targetZ = session.targetZ,
                ceilingHitY = session.ceilingHitY,
                exitFloorY = session.exitFloorY,
                phaseTick = session.phaseTick,
                phaseDuration = session.phaseDuration
            )
        )
    }

    private fun syncIdle(player: ServerPlayer) {
        ServerPlayNetworking.send(
            player,
            InfiltrationSyncPayload(
                phaseId = InfiltrationPhase.IDLE.id,
                originX = player.x,
                originY = player.y,
                originZ = player.z,
                targetX = player.x,
                targetZ = player.z,
                ceilingHitY = player.y,
                exitFloorY = player.y,
                phaseTick = 0,
                phaseDuration = 1
            )
        )
    }
}
