package com.howlite.cryoawakening.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChainBlock
import net.minecraft.world.level.block.IronBarsBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * ClawshotAnchorEntity
 *
 * Entité projectile représentant la tête du grappin (anchor_core) inspirée de Zelda: Twilight Princess.
 *
 * Machine à états :
 * - FLYING : Trajectoire rapide en ligne droite, griffes ouvertes.
 * - HOOKED_BLOCK : Planté dans une surface solide, griffes refermées, tracte le joueur et annule les dégâts de chute.
 * - HOOKED_ENTITY : Accroché à une entité (mob léger attiré vers le joueur / mob lourd attirant le joueur).
 * - RETRACTING : Rappel rapide vers la main du joueur avec fermeture des griffes.
 */
class ClawshotAnchorEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level) {

    enum class AnchorState(val id: Int) {
        FLYING(0),
        HOOKED_BLOCK(1),
        HOOKED_ENTITY(2),
        RETRACTING(3);

        companion object {
            fun fromId(id: Int): AnchorState = entries.firstOrNull { it.id == id } ?: FLYING
        }
    }

    companion object {
        const val MAX_RANGE: Double = 28.0
        const val FLY_SPEED: Double = 2.4
        const val PULL_SPEED: Double = 1.15
        const val RETRACT_SPEED: Double = 2.2

        val ANCHOR_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val OWNER_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val HOOKED_ENTITY_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val HOOK_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.FLOAT)

        val HOOK_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.FLOAT)

        val HOOK_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.FLOAT)

        val CLAW_OPEN_AMOUNT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.FLOAT)

        val USED_HAND: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val HOOK_DIRECTION: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FLYING_SPEED, 2.0)
        }
    }

    var anchorState: AnchorState
        get() = AnchorState.fromId(entityData.get(ANCHOR_STATE))
        set(value) = entityData.set(ANCHOR_STATE, value.id)

    var ownerEntityId: Int
        get() = entityData.get(OWNER_ID)
        set(value) = entityData.set(OWNER_ID, value)

    var hookedTargetId: Int
        get() = entityData.get(HOOKED_ENTITY_ID)
        set(value) = entityData.set(HOOKED_ENTITY_ID, value)

    var hookPosition: Vec3
        get() = Vec3(
            entityData.get(HOOK_X).toDouble(),
            entityData.get(HOOK_Y).toDouble(),
            entityData.get(HOOK_Z).toDouble()
        )
        set(value) {
            entityData.set(HOOK_X, value.x.toFloat())
            entityData.set(HOOK_Y, value.y.toFloat())
            entityData.set(HOOK_Z, value.z.toFloat())
        }

    var clawOpenAmount: Float
        get() = entityData.get(CLAW_OPEN_AMOUNT)
        set(value) = entityData.set(CLAW_OPEN_AMOUNT, value.coerceIn(0.0f, 1.0f))

    var usedHand: InteractionHand
        get() = if (entityData.get(USED_HAND) == 1) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        set(value) = entityData.set(USED_HAND, if (value == InteractionHand.OFF_HAND) 1 else 0)

    var hookDirection: Int
        get() = entityData.get(HOOK_DIRECTION)
        set(value) = entityData.set(HOOK_DIRECTION, value)

    var prevClawOpen: Float = 1.0f
    var ownerUuid: UUID? = null
    var flightOrigin: Vec3 = Vec3.ZERO
    var flightTicks: Int = 0
    var hookedTicks: Int = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(ANCHOR_STATE, AnchorState.FLYING.id)
        builder.define(OWNER_ID, -1)
        builder.define(HOOKED_ENTITY_ID, -1)
        builder.define(HOOK_X, 0.0f)
        builder.define(HOOK_Y, 0.0f)
        builder.define(HOOK_Z, 0.0f)
        builder.define(CLAW_OPEN_AMOUNT, 1.0f)
        builder.define(USED_HAND, 0)
        builder.define(HOOK_DIRECTION, -1)
    }

    override fun registerGoals() {
        // Pas d'IA vanilla : la physique procédurale gère tous les déplacements
    }

    /**
     * Initialise la projection du grappin depuis les yeux du joueur.
     */
    fun setupLaunch(player: Player, hand: InteractionHand) {
        this.ownerUuid = player.uuid
        this.ownerEntityId = player.id
        this.usedHand = hand
        this.anchorState = AnchorState.FLYING
        this.clawOpenAmount = 1.0f
        this.prevClawOpen = 1.0f
        this.flightTicks = 0
        this.hookedTicks = 0
        this.setNoGravity(true)
        this.noPhysics = true

        val look = player.lookAngle.normalize()
        val eyePos = player.eyePosition
        val startPos = eyePos.add(look.scale(0.35))
        this.flightOrigin = startPos
        this.setPos(startPos.x, startPos.y, startPos.z)
        this.deltaMovement = look.scale(FLY_SPEED)
        this.hookPosition = startPos
        this.hookDirection = -1
    }

    /**
     * Rappelle immédiatement le grappin vers le joueur.
     */
    fun retract() {
        if (anchorState != AnchorState.RETRACTING) {
            anchorState = AnchorState.RETRACTING
            hookedTargetId = -1
            hookDirection = -1
            deltaMovement = Vec3.ZERO
            level().playSound(
                null,
                blockPosition(),
                SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS,
                0.8f,
                1.4f
            )
        }
    }

    fun getOwnerEntity(): Player? {
        val uuid = ownerUuid
        if (uuid != null) {
            val playerByUuid = level().getPlayerByUUID(uuid)
            if (playerByUuid != null) return playerByUuid
        }
        val id = ownerEntityId
        if (id != -1) {
            return level().getEntity(id) as? Player
        }
        return null
    }

    override fun tick() {
        super.tick()
        prevClawOpen = clawOpenAmount
        flightTicks++

        val owner = getOwnerEntity()

        // Si le propriétaire n'existe plus ou est mort, détruit l'ancre
        if (owner == null || !owner.isAlive) {
            if (!level().isClientSide) discard()
            return
        }

        // Côté Client : Uniquement interpolation visuelle et suivi d'affichage
        if (level().isClientSide) {
            tickClient(owner)
            return
        }

        val serverLevel = level() as? ServerLevel ?: return

        when (anchorState) {
            AnchorState.FLYING -> tickFlying(serverLevel, owner)
            AnchorState.HOOKED_BLOCK -> tickHookedBlock(serverLevel, owner)
            AnchorState.HOOKED_ENTITY -> tickHookedEntity(serverLevel, owner)
            AnchorState.RETRACTING -> tickRetracting(serverLevel, owner)
        }
    }

    private fun tickClient(owner: Player) {
        when (anchorState) {
            AnchorState.FLYING -> {
                clawOpenAmount = (clawOpenAmount + 0.25f).coerceAtMost(1.0f)
            }
            AnchorState.HOOKED_BLOCK -> {
                clawOpenAmount = (clawOpenAmount - 0.35f).coerceAtLeast(0.0f)
                val anchorPos = hookPosition
                setPos(anchorPos.x, anchorPos.y, anchorPos.z)
            }
            AnchorState.HOOKED_ENTITY -> {
                clawOpenAmount = (clawOpenAmount - 0.35f).coerceAtLeast(0.0f)
                val target = level().getEntity(hookedTargetId)
                if (target != null && target.isAlive) {
                    val targetCenter = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
                    setPos(targetCenter.x, targetCenter.y, targetCenter.z)
                }
            }
            AnchorState.RETRACTING -> {
                clawOpenAmount = (clawOpenAmount - 0.3f).coerceAtLeast(0.0f)
                val targetPos = owner.eyePosition.add(0.0, -0.2, 0.0)
                val toOwner = targetPos.subtract(position())
                if (toOwner.length() > 0.1) {
                    val moveDir = toOwner.normalize().scale(RETRACT_SPEED)
                    setPos(x + moveDir.x, y + moveDir.y, z + moveDir.z)
                }
            }
        }
    }

    private fun tickFlying(serverLevel: ServerLevel, owner: Player) {
        // En vol, ouverture maximale des 3 griffes
        clawOpenAmount = (clawOpenAmount + 0.25f).coerceAtMost(1.0f)

        val currentPos = position()
        val motion = deltaMovement
        val nextPos = currentPos.add(motion)

        // 1. Détection de collision de blocs par Raycast
        val blockHit = serverLevel.clip(
            ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        )

        val targetEndPos = if (blockHit.type != HitResult.Type.MISS) blockHit.location else nextPos

        // 2. Détection de collision d'entités vivantes
        val entityHit = findEntityOnPath(currentPos, targetEndPos, owner)

        if (entityHit != null) {
            // Impact sur une entité
            hookedTargetId = entityHit.id
            anchorState = AnchorState.HOOKED_ENTITY
            val hitPos = entityHit.position().add(0.0, entityHit.bbHeight * 0.5, 0.0)
            hookPosition = hitPos
            setPos(hitPos.x, hitPos.y, hitPos.z)
            deltaMovement = Vec3.ZERO

            // Sons et effets d'impact
            playImpactEffects(serverLevel, hitPos)
            return
        }

        if (blockHit.type == HitResult.Type.BLOCK) {
            val hitPos = blockHit.location
            val hitBlockPos = blockHit.blockPos
            val hitState = serverLevel.getBlockState(hitBlockPos)

            if (isValidHookTarget(hitState, hitBlockPos)) {
                // Accrochage réussi dans le bloc
                anchorState = AnchorState.HOOKED_BLOCK
                hookPosition = hitPos
                hookDirection = blockHit.direction.get3DDataValue()
                setPos(hitPos.x, hitPos.y, hitPos.z)
                deltaMovement = Vec3.ZERO

                // Sons de verrouillage métallique Zelda TP
                playImpactEffects(serverLevel, hitPos)

                // Particules d'impact étincelantes
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    hitPos.x, hitPos.y, hitPos.z,
                    12, 0.1, 0.1, 0.1, 0.15
                )
                return
            } else {
                // Rebond/Surface non compatible -> rétractation immédiate
                retract()
                return
            }
        }

        // Déplacement normal en vol
        setPos(nextPos.x, nextPos.y, nextPos.z)

        // Limite de portée maximale
        val distFromOwner = nextPos.distanceTo(owner.eyePosition)
        if (distFromOwner > MAX_RANGE || flightTicks > 35) {
            retract()
        }
    }

    private fun tickHookedBlock(serverLevel: ServerLevel, owner: Player) {
        hookedTicks++
        // Griffes refermées fermement sur la prise
        clawOpenAmount = (clawOpenAmount - 0.35f).coerceAtLeast(0.0f)

        // Maintien de l'ancre fixe au point d'impact
        val anchorPos = hookPosition
        setPos(anchorPos.x, anchorPos.y, anchorPos.z)
        deltaMovement = Vec3.ZERO

        val playerPos = owner.position()
        val toAnchor = anchorPos.subtract(playerPos)
        val dist = toAnchor.length()

        // 1. ANNULATION CONTINUE DES DÉGÂTS DE CHUTE
        cancelPlayerFall(owner)

        // 2. Conditions de détachement :
        // - Proximité atteinte (< 1.7 blocs)
        // - Joueur accroupi (Shift) ou sautant
        // - Temps de traction maximal dépassé (100 ticks = 5s)
        if (dist < 1.7 || owner.isShiftKeyDown || hookedTicks > 100) {
            releasePlayerAtDestination(owner, dist < 2.0)
            retract()
            return
        }

        // 3. Application de la traction fluide vers le point d'impact
        applySmoothPull(serverLevel, owner, toAnchor, dist)
    }

    private fun tickHookedEntity(serverLevel: ServerLevel, owner: Player) {
        hookedTicks++
        clawOpenAmount = (clawOpenAmount - 0.35f).coerceAtLeast(0.0f)

        val target = serverLevel.getEntity(hookedTargetId)
        if (target == null || !target.isAlive) {
            retract()
            return
        }

        // Alignement de l'ancre sur la cible
        val targetCenter = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
        setPos(targetCenter.x, targetCenter.y, targetCenter.z)
        hookPosition = targetCenter
        deltaMovement = Vec3.ZERO

        val isHeavy = isHeavyEntity(target)

        if (isHeavy) {
            // --- MOB LOURD : Le joueur est tracté vers le mob ---
            cancelPlayerFall(owner)

            val playerPos = owner.position()
            val toTarget = targetCenter.subtract(playerPos)
            val dist = toTarget.length()

            if (dist < 2.0 || owner.isShiftKeyDown || hookedTicks > 100) {
                releasePlayerAtDestination(owner, dist < 2.5)
                retract()
                return
            }

            applySmoothPull(serverLevel, owner, toTarget, dist)
        } else {
            // --- MOB LÉGER : Le mob est tracté vers le joueur ---
            val toPlayer = owner.eyePosition.subtract(target.position())
            val dist = toPlayer.length()

            // Dégâts légers et étourdissement du mob côté serveur
            if (hookedTicks == 1 && target is LivingEntity) {
                target.hurtServer(
                    serverLevel,
                    target.damageSources().mobAttack(owner),
                    2.0f
                )
            }

            if (dist < 1.8 || hookedTicks > 60) {
                target.setDeltaMovement(0.0, 0.1, 0.0)
                target.hurtMarked = true
                retract()
                return
            }

            // Traction du mob vers Link/le joueur
            val pullDir = toPlayer.normalize()
            val pullSpeed = 0.85
            target.setDeltaMovement(pullDir.x * pullSpeed, pullDir.y * pullSpeed + 0.08, pullDir.z * pullSpeed)
            target.hurtMarked = true
            target.resetFallDistance()
        }
    }

    private fun tickRetracting(serverLevel: ServerLevel, owner: Player) {
        // En rétractation, les griffes restent closes
        clawOpenAmount = (clawOpenAmount - 0.3f).coerceAtLeast(0.0f)

        val targetPos = owner.eyePosition.add(0.0, -0.2, 0.0)
        val toOwner = targetPos.subtract(position())
        val dist = toOwner.length()

        if (dist < 1.4 || flightTicks > 300) {
            // Rentrée dans le gantelet terminée
            serverLevel.playSound(
                null,
                owner.blockPosition(),
                SoundEvents.CHAIN_STEP,
                SoundSource.PLAYERS,
                0.6f,
                1.6f
            )
            discard()
            return
        }

        // Vol retour très rapide vers la main
        val moveDir = toOwner.normalize().scale(RETRACT_SPEED)
        setPos(position().x + moveDir.x, position().y + moveDir.y, position().z + moveDir.z)
    }

    /**
     * Applique une vélocité progressive et amortie au joueur vers la cible.
     */
    private fun applySmoothPull(serverLevel: ServerLevel, player: Player, toAnchor: Vec3, dist: Double) {
        val dir = toAnchor.normalize()
        val speed = PULL_SPEED.coerceAtMost(dist * 0.9)
        val velocity = dir.scale(speed)

        player.setDeltaMovement(velocity.x, velocity.y, velocity.z)
        player.hurtMarked = true

        // Son de chaîne métallique qui s'enroule
        if (hookedTicks % 4 == 0) {
            serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS,
                0.4f,
                1.3f + (hookedTicks % 8) * 0.05f
            )
        }
    }


    /**
     * Relâche le joueur au point d'accroche avec un léger saut vers le haut façon Zelda TP.
     */
    private fun releasePlayerAtDestination(player: Player, giveHop: Boolean) {
        cancelPlayerFall(player)
        if (giveHop) {
            val currentVel = player.deltaMovement
            player.setDeltaMovement(currentVel.x * 0.5, 0.28, currentVel.z * 0.5)
            player.hurtMarked = true
        }
    }

    /**
     * Annule strictement les dégâts de chute.
     */
    private fun cancelPlayerFall(player: Player) {
        player.resetFallDistance()
        player.fallDistance = 0.0
    }

    /**
     * Détermine si un bloc est une cible valide pour s'accrocher.
     */
    @Suppress("DEPRECATION")
    private fun isValidHookTarget(state: BlockState, pos: BlockPos): Boolean {
        if (state.isAir) return false
        // Barres de fer, grilles, chaînes
        val block = state.block
        if (block is IronBarsBlock || block is ChainBlock) return true
        // Tout bloc solide ou bloquant le passage
        return state.blocksMotion() || state.isSolid
    }

    /**
     * Détermine si une entité est considérée comme "lourde" (tracte le joueur) ou "légère" (attirée par le joueur).
     */
    fun isHeavyEntity(entity: Entity): Boolean {
        if (entity is Player) return true
        if (entity is ItemEntity) return false
        if (entity is LivingEntity) {
            // Résistance au knockback élevée ou grand gabarit
            val kbRes = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.value ?: 0.0
            if (kbRes >= 0.6) return true
            val volume = entity.bbWidth * entity.bbWidth * entity.bbHeight
            if (volume >= 1.2f) return true
            if (entity.type.category == MobCategory.MONSTER && volume >= 0.8f) return true
        }
        return false
    }

    private fun findEntityOnPath(from: Vec3, to: Vec3, owner: Player): Entity? {
        val box = AABB(from, to).inflate(0.5)
        val candidates = level().getEntities(this, box) { e ->
            e != owner && e != this && e.isAlive && (e is LivingEntity || e is ItemEntity)
        }

        var closestEntity: Entity? = null
        var closestDist = Double.MAX_VALUE

        for (candidate in candidates) {
            val candBox = candidate.boundingBox.inflate(0.3)
            val clip = candBox.clip(from, to)
            if (clip.isPresent) {
                val dist = from.distanceTo(clip.get())
                if (dist < closestDist) {
                    closestDist = dist
                    closestEntity = candidate
                }
            }
        }

        return closestEntity
    }

    private fun playImpactEffects(serverLevel: ServerLevel, pos: Vec3) {
        serverLevel.playSound(
            null,
            BlockPos.containing(pos),
            SoundEvents.ANVIL_LAND,
            SoundSource.PLAYERS,
            0.55f,
            1.85f
        )
        serverLevel.playSound(
            null,
            BlockPos.containing(pos),
            SoundEvents.CHAIN_HIT,
            SoundSource.PLAYERS,
            0.9f,
            1.0f
        )
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putInt("AnchorState", anchorState.id)
        output.putInt("HookedEntityId", hookedTargetId)
        output.putFloat("HookX", hookPosition.x.toFloat())
        output.putFloat("HookY", hookPosition.y.toFloat())
        output.putFloat("HookZ", hookPosition.z.toFloat())
        output.putFloat("ClawOpen", clawOpenAmount)
        ownerUuid?.let { output.putString("OwnerUuid", it.toString()) }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        anchorState = AnchorState.fromId(input.getIntOr("AnchorState", 0))
        hookedTargetId = input.getIntOr("HookedEntityId", -1)
        val hx = input.getFloatOr("HookX", 0.0f)
        val hy = input.getFloatOr("HookY", 0.0f)
        val hz = input.getFloatOr("HookZ", 0.0f)
        hookPosition = Vec3(hx.toDouble(), hy.toDouble(), hz.toDouble())
        clawOpenAmount = input.getFloatOr("ClawOpen", 1.0f)
        prevClawOpen = clawOpenAmount
        val ownerStr = input.getStringOr("OwnerUuid", "")
        if (ownerStr.isNotEmpty()) {
            try {
                ownerUuid = UUID.fromString(ownerStr)
            } catch (_: Exception) {}
        }
    }
}
