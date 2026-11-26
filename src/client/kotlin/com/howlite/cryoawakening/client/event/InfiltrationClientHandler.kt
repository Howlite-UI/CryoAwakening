package com.howlite.cryoawakening.client.event

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.infiltration.InfiltrationClientBridge
import com.howlite.cryoawakening.infiltration.InfiltrationMath
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import com.howlite.cryoawakening.infiltration.InfiltrationTarget
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.network.InfiltrationActionPayload
import com.howlite.cryoawakening.network.InfiltrationSyncPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * InfiltrationClientHandler
 *
 * Contrôleur client complet de la Perle d'Infiltration (style Ascend de Zelda: TOTK) :
 * - 1er Clic Droit : entre en mode AIMING (animation caméra douce vers le plafond + cible 3D temps réel).
 * - Clic Gauche en AIMING : annule proprement.
 * - 2ème Clic Droit (sur plafond valide) : déclenche l'envol, puis le shader vortex plein écran dans la roche.
 * - Phase PEEKING : tête au ras du sol avec caméra 360° libre pour observer la surface.
 *   - Clic Droit : émerge définitivement du sol (consomme la perle).
 *   - Clic Gauche : annule et redescend à la position d'origine (ne consomme pas la perle).
 */
object InfiltrationClientHandler {

    val VORTEX_ASCEND_SHADER: Identifier = CryoAwakening.id("infiltration_vortex")
    val VORTEX_RETURN_SHADER: Identifier = CryoAwakening.id("infiltration_vortex_reverse")

    var phase: InfiltrationPhase = InfiltrationPhase.IDLE
        private set

    var currentTarget: InfiltrationTarget? = null
        private set

    // Coordonnées synchronisées de la traversée en cours
    var originX: Double = 0.0
    var originY: Double = 0.0
    var originZ: Double = 0.0
    var targetX: Double = 0.0
    var targetZ: Double = 0.0
    var ceilingHitY: Double = 0.0
    var exitFloorY: Double = 0.0
    var phaseTick: Int = 0
    var phaseDuration: Int = 1

    // Animation de visée & caméra (interpolées par partialTick)
    var prevAimAnim: Float = 0.0f
    var aimAnim: Float = 0.0f

    var prevCameraDetach: Float = 0.0f
    var cameraDetach: Float = 0.0f

    var prevFovOffset: Float = 0.0f
    var fovOffset: Float = 0.0f

    var prevFlashAlpha: Float = 0.0f
    var flashAlpha: Float = 0.0f
    var flashColorRgb: Int = 0x55FFD6 // Émeraude-cyan TOTK

    // Caméra orbitale libre 360° autour du personnage qui nage dans le vortex
    var orbitYaw: Float = 0.0f
    var prevOrbitYaw: Float = 0.0f
    var orbitPitch: Float = -12.0f
    var prevOrbitPitch: Float = -12.0f
    var swimBodyYaw: Float = 0.0f
    var vortexSeed: Int = 1337

    var invalidFlashTicks: Int = 0
        private set

    private var wasTargetValid: Boolean = false
    private var pitchNudgeTicks: Int = 0
    private var targetAimPitch: Float = -50.0f
    private var activeTimeTicks: Int = 0

    // Evite qu'un seul clic long ne déclenche plusieurs transitions
    private var clickCooldownTicks: Int = 0

    @JvmStatic
    fun isAiming(): Boolean = phase == InfiltrationPhase.AIMING

    @JvmStatic
    fun isActiveSequence(): Boolean = phase.isActiveSequence()

    @JvmStatic
    fun isInsideBlocks(): Boolean = phase.isInsideBlocks()

    @JvmStatic
    fun isPeeking(): Boolean = phase == InfiltrationPhase.PEEKING

    @JvmStatic
    fun shouldHideFirstPersonHands(): Boolean = phase.isActiveSequence()

    @JvmStatic
    fun shouldPoseArmsOverhead(): Boolean =
        phase == InfiltrationPhase.RISING_TO_CEILING ||
            phase == InfiltrationPhase.SWIMMING_THROUGH ||
            phase == InfiltrationPhase.EMERGING ||
            phase == InfiltrationPhase.RETURNING

    @JvmStatic
    fun getActivePostEffectId(): Identifier? = when (phase) {
        InfiltrationPhase.SWIMMING_THROUGH -> VORTEX_ASCEND_SHADER
        InfiltrationPhase.RETURNING -> VORTEX_RETURN_SHADER
        else -> null
    }

    fun getAimAnim(partialTick: Float): Float = Mth.lerp(partialTick, prevAimAnim, aimAnim)
    fun getCameraDetach(partialTick: Float): Float = Mth.lerp(partialTick, prevCameraDetach, cameraDetach)
    fun getFovOffset(partialTick: Float): Float = Mth.lerp(partialTick, prevFovOffset, fovOffset)
    fun getFlashAlpha(partialTick: Float): Float = Mth.lerp(partialTick, prevFlashAlpha, flashAlpha)
    fun getOrbitYaw(partialTick: Float): Float = Mth.lerp(partialTick, prevOrbitYaw, orbitYaw)
    fun getOrbitPitch(partialTick: Float): Float = Mth.lerp(partialTick, prevOrbitPitch, orbitPitch)
    fun getPhaseProgress(partialTick: Float): Float =
        ((phaseTick + partialTick) / phaseDuration.coerceAtLeast(1).toFloat()).coerceIn(0.0f, 1.0f)

    /**
     * Applique le mouvement de souris libre pour orbiter autour du personnage
     * pendant la nage dans le vortex (sans faire tourner le corps du perso).
     */
    @JvmStatic
    fun handleOrbitMouseMove(dx: Double, dy: Double) {
        val sens = Minecraft.getInstance().options.sensitivity().get().toFloat() * 0.6f + 0.2f
        val mult = sens * sens * sens * 1.2f
        orbitYaw += (dx * mult).toFloat()
        orbitPitch = Mth.clamp(orbitPitch + (dy * mult).toFloat(), -62.0f, 62.0f)
    }

    fun register() {
        // Pont vers les mixins communs (PlayerInfiltrationMixin, InventoryMixin)
        InfiltrationClientBridge.clientInfiltratingCheck = { player ->
            val local = Minecraft.getInstance().player
            local != null && local.uuid == player.uuid && phase.isActiveSequence()
        }

        // Réception de l'état autoritaire serveur
        ClientPlayNetworking.registerGlobalReceiver(InfiltrationSyncPayload.ID) { payload, context ->
            context.client().execute {
                applyServerSync(payload)
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            tick(client)
        }
    }

    private fun applyServerSync(payload: InfiltrationSyncPayload) {
        val newPhase = InfiltrationPhase.byId(payload.phaseId)
        val prevPhase = phase

        originX = payload.originX
        originY = payload.originY
        originZ = payload.originZ
        targetX = payload.targetX
        targetZ = payload.targetZ
        ceilingHitY = payload.ceilingHitY
        exitFloorY = payload.exitFloorY
        phaseTick = payload.phaseTick
        phaseDuration = payload.phaseDuration.coerceAtLeast(1)
        phase = newPhase

        // Flash lumineux aux transitions clés (entrée plafond, arrivée surface, émergence, retour)
        if (prevPhase != newPhase) {
            val player = Minecraft.getInstance().player
            when (newPhase) {
                InfiltrationPhase.SWIMMING_THROUGH -> {
                    flashColorRgb = 0x55FFD6
                    flashAlpha = 0.85f
                    prevFlashAlpha = 0.85f
                    vortexSeed = (System.nanoTime() xor (payload.targetX.toBits())).toInt()
                    if (player != null) {
                        swimBodyYaw = player.yRot
                        orbitYaw = player.yRot
                        prevOrbitYaw = player.yRot
                        orbitPitch = -12.0f
                        prevOrbitPitch = -12.0f
                    }
                    cameraDetach = 1.0f
                    prevCameraDetach = 1.0f
                }
                InfiltrationPhase.PEEKING -> {
                    flashColorRgb = 0x8CFFE6
                    flashAlpha = 0.90f
                    prevFlashAlpha = 0.90f
                    // Synchroniser l'orientation du joueur avec l'angle de caméra orbitale + vue plongeante douce sur le perso à moitié sorti
                    if (player != null && prevPhase == InfiltrationPhase.SWIMMING_THROUGH) {
                        player.yRot = orbitYaw
                        player.yRotO = orbitYaw
                        player.xRot = 24.0f
                        player.xRotO = 24.0f
                    }
                }
                InfiltrationPhase.EMERGING -> {
                    flashColorRgb = 0xA8FFF0
                    flashAlpha = 0.55f
                    prevFlashAlpha = 0.55f
                }
                InfiltrationPhase.RETURNING -> {
                    flashColorRgb = 0x74C8FF
                    flashAlpha = 0.65f
                    prevFlashAlpha = 0.65f
                    vortexSeed = (System.nanoTime() xor (payload.originY.toBits())).toInt()
                    if (player != null) {
                        swimBodyYaw = player.yRot
                        orbitYaw = player.yRot
                        prevOrbitYaw = player.yRot
                        orbitPitch = 14.0f
                        prevOrbitPitch = 14.0f
                    }
                    cameraDetach = 1.0f
                    prevCameraDetach = 1.0f
                }
                InfiltrationPhase.IDLE -> {
                    cameraDetach = 0.0f
                    prevCameraDetach = 0.0f
                    if (player != null) {
                        player.noPhysics = player.isSpectator
                        player.isNoGravity = false
                        player.fallDistance = 0.0
                        if (prevPhase == InfiltrationPhase.RETURNING) {
                            player.yRot = orbitYaw
                            player.yRotO = orbitYaw
                        }
                    }
                    if (prevPhase == InfiltrationPhase.RETURNING) {
                        flashColorRgb = 0x74C8FF
                        flashAlpha = 0.45f
                    }
                }
                else -> {}
            }
        }
    }

    private fun tick(client: Minecraft) {
        val player = client.player
        val level = client.level

        if (player == null || level == null || !player.isAlive) {
            resetAll()
            return
        }

        activeTimeTicks++
        if (clickCooldownTicks > 0) clickCooldownTicks--
        if (invalidFlashTicks > 0) invalidFlashTicks--

        // Sauvegarder les valeurs précédentes pour interpolation fluide (partialTick)
        prevAimAnim = aimAnim
        prevCameraDetach = cameraDetach
        prevFovOffset = fovOffset
        prevFlashAlpha = flashAlpha
        prevOrbitYaw = orbitYaw
        prevOrbitPitch = orbitPitch

        // Décroissance douce du flash de transition
        flashAlpha = (flashAlpha - 0.085f).coerceAtLeast(0.0f)

        // Annuler le mode AIMING si un menu s'ouvre ou si le joueur ne tient plus la perle
        if (phase == InfiltrationPhase.AIMING) {
            if (client.gui.screen() != null || !isHoldingPearl(player) || player.isPassenger || player.isSleeping) {
                cancelAiming(client, silent = true)
            }
        }

        when (phase) {
            InfiltrationPhase.IDLE -> {
                currentTarget = null
                wasTargetValid = false
                aimAnim = Mth.lerp(0.25f, aimAnim, 0.0f)
                if (aimAnim < 0.005f) aimAnim = 0.0f
                cameraDetach = Mth.lerp(0.22f, cameraDetach, 0.0f)
                if (cameraDetach < 0.005f) cameraDetach = 0.0f
                fovOffset = Mth.lerp(0.22f, fovOffset, 0.0f)
                if (kotlin.math.abs(fovOffset) < 0.05f) fovOffset = 0.0f
            }

            InfiltrationPhase.AIMING -> {
                val target = InfiltrationMath.findTarget(level, player)
                currentTarget = target

                // Petit carillon cristallin au moment où la cible devient valide
                if (target.valid && !wasTargetValid) {
                    client.soundManager.play(
                        SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.55f, 0.55f)
                    )
                }
                wasTargetValid = target.valid

                // Animation caméra douce : léger regard vers le haut à l'ouverture
                if (pitchNudgeTicks > 0) {
                    pitchNudgeTicks--
                    val currentPitch = player.xRot
                    val nextPitch = Mth.lerp(0.26f, currentPitch, targetAimPitch)
                    player.xRot = nextPitch.coerceIn(-89.0f, 89.0f)
                }

                aimAnim = Mth.lerp(0.28f, aimAnim, 1.0f)
                cameraDetach = Mth.lerp(0.2f, cameraDetach, 0.0f)
                val desiredFov = if (target.valid) -8.5f else -5.0f
                fovOffset = Mth.lerp(0.22f, fovOffset, desiredFov)

                // Particules montantes légères vers le plafond quand le ciblage est valide
                if (target.valid && activeTimeTicks % 2 == 0) {
                    val angle = activeTimeTicks * 0.45
                    val r = 0.45 + level.random.nextDouble() * 0.25
                    val px = target.columnX + cos(angle) * r
                    val pz = target.columnZ + sin(angle) * r
                    val py = player.y + 0.3 + level.random.nextDouble() * (target.ceilingHitY - player.y - 0.5).coerceAtLeast(0.5)
                    level.addParticle(
                        ParticleTypes.GLOW,
                        px, py, pz,
                        0.0, 0.07, 0.0
                    )
                }
            }

            InfiltrationPhase.RISING_TO_CEILING -> {
                aimAnim = Mth.lerp(0.3f, aimAnim, 0.0f)
                cameraDetach = Mth.lerp(0.26f, cameraDetach, 1.0f)
                fovOffset = Mth.lerp(0.22f, fovOffset, 10.0f)

                phaseTick = (phaseTick + 1).coerceAtMost(phaseDuration)
                val t = (phaseTick.toDouble() / phaseDuration.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                val easedY = t * t
                val easedXZ = 1.0 - (1.0 - t) * (1.0 - t)
                val enterY = ceilingHitY - 1.35
                val curX = originX + (targetX - originX) * easedXZ
                val curZ = originZ + (targetZ - originZ) * easedXZ
                val curY = originY + (enterY - originY) * easedY

                applyClientPosition(player, curX, curY, curZ)

                // Double spirale lumineuse autour du joueur qui s'envole
                for (i in 0 until 2) {
                    val a = activeTimeTicks * 0.85 + i * PI
                    level.addParticle(
                        ParticleTypes.END_ROD,
                        curX + cos(a) * 0.6, curY + 0.5, curZ + sin(a) * 0.6,
                        0.0, -0.04, 0.0
                    )
                }
            }

            InfiltrationPhase.SWIMMING_THROUGH -> {
                aimAnim = 0.0f
                cameraDetach = Mth.lerp(0.28f, cameraDetach, 1.0f)
                fovOffset = Mth.lerp(0.22f, fovOffset, 7.5f)

                phaseTick = (phaseTick + 1).coerceAtMost(phaseDuration)
                val t = (phaseTick.toDouble() / phaseDuration.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                val startSwimY = ceilingHitY - 1.35
                val peekY = exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                val eased = t * t * (3.0 - 2.0 * t)
                val curY = startSwimY + (peekY - startSwimY) * eased

                applyClientPosition(player, targetX, curY, targetZ)
            }

            InfiltrationPhase.PEEKING -> {
                aimAnim = 0.0f
                cameraDetach = Mth.lerp(0.24f, cameraDetach, 1.0f)
                fovOffset = Mth.lerp(0.22f, fovOffset, -2.5f)

                phaseTick++
                val peekY = exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                applyClientPosition(player, targetX, peekY, targetZ)

                // Particules d'aura autour du trou lumineux au ras du sol
                if (activeTimeTicks % 3 == 0) {
                    val a = activeTimeTicks * 0.55
                    level.addParticle(
                        ParticleTypes.GLOW,
                        targetX + cos(a) * 0.68,
                        exitFloorY + 0.06,
                        targetZ + sin(a) * 0.68,
                        0.0, 0.025, 0.0
                    )
                }
            }

            InfiltrationPhase.EMERGING -> {
                aimAnim = 0.0f
                // Rapproche la caméra jusqu'au seuil sûr juste derrière la tête, puis bascule net en 1ère personne en fin de bond
                phaseTick = (phaseTick + 1).coerceAtMost(phaseDuration)
                val t = (phaseTick.toDouble() / phaseDuration.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                cameraDetach = if (t < 0.78) {
                    Mth.lerp((t / 0.78).toFloat(), 1.0f, 0.42f)
                } else {
                    0.0f
                }
                if (t >= 0.78) prevCameraDetach = 0.0f
                fovOffset = Mth.lerp(0.22f, fovOffset, 2.5f)

                val peekY = exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                val arcY = if (t < 0.65) {
                    val u = t / 0.65
                    val easeOut = 1.0 - (1.0 - u) * (1.0 - u)
                    peekY + (exitFloorY + 0.48 - peekY) * easeOut
                } else {
                    val u = (t - 0.65) / 0.35
                    val easeIn = u * u
                    (exitFloorY + 0.48) + (exitFloorY - (exitFloorY + 0.48)) * easeIn
                }
                applyClientPosition(player, targetX, arcY, targetZ)
            }

            InfiltrationPhase.RETURNING -> {
                aimAnim = 0.0f
                cameraDetach = Mth.lerp(0.28f, cameraDetach, 1.0f)
                fovOffset = Mth.lerp(0.22f, fovOffset, 6.5f)

                phaseTick = (phaseTick + 1).coerceAtMost(phaseDuration)
                val t = (phaseTick.toDouble() / phaseDuration.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                val peekY = exitFloorY - InfiltrationMath.PEEK_HEAD_OFFSET
                val eased = t * t * (3.0 - 2.0 * t)
                val curX = targetX + (originX - targetX) * eased
                val curZ = targetZ + (originZ - targetZ) * eased
                val curY = peekY + (originY - peekY) * eased

                applyClientPosition(player, curX, curY, curZ)
            }
        }
    }

    private fun applyClientPosition(player: net.minecraft.client.player.LocalPlayer, x: Double, y: Double, z: Double) {
        player.noPhysics = true
        player.isNoGravity = true
        player.fallDistance = 0.0
        player.deltaMovement = Vec3.ZERO
        // Met à jour la position tout en conservant xOld/yOld/zOld pour un rendu 144Hz sans saccade
        player.setPos(x, y, z)
    }

    /**
     * Appelé lorsque le joueur bouge la souris : annule immédiatement le nudge automatique de pitch
     * pour laisser un contrôle total et naturel au joueur.
     */
    @JvmStatic
    fun onPlayerMouseLook() {
        pitchNudgeTicks = 0
    }

    /**
     * Intercepte le Clic Droit (MinecraftMixin.startUseItem).
     * Retourne true si l'événement a été consommé par la Perle d'Infiltration.
     */
    @JvmStatic
    fun handleRightClick(): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        val level = client.level ?: return false
        if (client.gui.screen() != null) return false

        return when (phase) {
            InfiltrationPhase.IDLE -> {
                if (!isHoldingPearl(player) || player.isPassenger || player.isSleeping) return false
                if (clickCooldownTicks > 0) return true

                // 1er Clic Droit : Activation du mode visée du plafond !
                phase = InfiltrationPhase.AIMING
                clickCooldownTicks = 5
                val target = InfiltrationMath.findTarget(level, player)
                currentTarget = target
                wasTargetValid = target.valid

                // Petite animation caméra vers le haut si le joueur ne regarde pas déjà le plafond
                if (player.xRot > -34.0f) {
                    targetAimPitch = -54.0f
                    pitchNudgeTicks = 10
                } else {
                    pitchNudgeTicks = 0
                }

                client.soundManager.play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.35f, 0.65f)
                )
                true
            }

            InfiltrationPhase.AIMING -> {
                if (clickCooldownTicks > 0) return true
                clickCooldownTicks = 6

                // 2ème Clic Droit : Lancement vers le plafond si la zone est valide !
                val target = InfiltrationMath.findTarget(level, player)
                currentTarget = target
                if (target.valid) {
                    ClientPlayNetworking.send(InfiltrationActionPayload(InfiltrationActionPayload.ACTION_START_ASCEND))

                    // Démarrage optimiste immédiat côté client pour 0 latence ressentie
                    originX = player.x
                    originY = player.y
                    originZ = player.z
                    targetX = target.columnX
                    targetZ = target.columnZ
                    ceilingHitY = target.ceilingHitY
                    exitFloorY = target.exitFloorY
                    phaseTick = 0
                    phaseDuration = InfiltrationMath.computeRiseTicks(player.y, target.ceilingHitY)
                    phase = InfiltrationPhase.RISING_TO_CEILING
                    cameraDetach = 0.38f
                    prevCameraDetach = 0.38f
                    pitchNudgeTicks = 0
                } else {
                    invalidFlashTicks = 14
                    client.soundManager.play(
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.6f)
                    )
                }
                true
            }

            InfiltrationPhase.PEEKING -> {
                if (clickCooldownTicks > 0) return true
                clickCooldownTicks = 6
                // Clic Droit en phase PEEKING -> Sortie définitive du sol (consomme la perle) !
                ClientPlayNetworking.send(InfiltrationActionPayload(InfiltrationActionPayload.ACTION_CONFIRM_EMERGE))
                true
            }

            else -> {
                // Bloque toute interaction parasite pendant l'envol / traversée / émergence / retour
                true
            }
        }
    }

    /**
     * Intercepte le Clic Gauche (MinecraftMixin.startAttack / continueAttack).
     * Retourne true si l'événement a été consommé.
     */
    @JvmStatic
    fun handleLeftClick(): Boolean {
        val client = Minecraft.getInstance()
        if (client.gui.screen() != null) return false

        return when (phase) {
            InfiltrationPhase.AIMING -> {
                if (clickCooldownTicks > 0) return true
                clickCooldownTicks = 5
                cancelAiming(client, silent = false)
                true
            }

            InfiltrationPhase.PEEKING -> {
                if (clickCooldownTicks > 0) return true
                clickCooldownTicks = 6
                // Clic Gauche en phase PEEKING -> Annule et retourne à la position d'origine !
                ClientPlayNetworking.send(InfiltrationActionPayload(InfiltrationActionPayload.ACTION_CANCEL_RETURN))
                true
            }

            InfiltrationPhase.IDLE -> false

            else -> {
                // Bloque les attaques/minages pendant la séquence active
                true
            }
        }
    }

    private fun cancelAiming(client: Minecraft, silent: Boolean) {
        phase = InfiltrationPhase.IDLE
        currentTarget = null
        wasTargetValid = false
        pitchNudgeTicks = 0
        if (!silent) {
            client.soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_STEP, 0.85f, 0.5f)
            )
        }
    }

    private fun isHoldingPearl(player: net.minecraft.world.entity.player.Player): Boolean {
        return player.mainHandItem.`is`(ModItems.INFILTRATION_PEARL) ||
            player.offhandItem.`is`(ModItems.INFILTRATION_PEARL)
    }

    private fun resetAll() {
        phase = InfiltrationPhase.IDLE
        currentTarget = null
        wasTargetValid = false
        aimAnim = 0.0f
        prevAimAnim = 0.0f
        cameraDetach = 0.0f
        prevCameraDetach = 0.0f
        fovOffset = 0.0f
        prevFovOffset = 0.0f
        flashAlpha = 0.0f
        prevFlashAlpha = 0.0f
        pitchNudgeTicks = 0
        clickCooldownTicks = 0
    }
}
