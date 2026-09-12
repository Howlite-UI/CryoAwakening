package com.howlite.cryoawakening.entity

import com.howlite.cryoawakening.enchantment.ModEnchantments
import com.howlite.cryoawakening.item.ClawshotItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
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
import net.minecraft.world.item.ItemStack
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

        val IS_CLINGING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.BOOLEAN)

        val SLACK_DISTANCE: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.FLOAT)

        val FROSTWIRE_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val SLINGSHOT_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val DISARM_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val EXTENDED_CHAIN_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val RAPID_REEL_LEVEL: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ClawshotAnchorEntity::class.java, EntityDataSerializers.INT)

        val PIERCING_SPIKE_LEVEL: EntityDataAccessor<Int> =
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

    var isClinging: Boolean
        get() = entityData.get(IS_CLINGING)
        set(value) = entityData.set(IS_CLINGING, value)

    var slackDistance: Float
        get() = entityData.get(SLACK_DISTANCE)
        set(value) = entityData.set(SLACK_DISTANCE, value.coerceAtLeast(0.0f))

    var frostwireLevel: Int
        get() = entityData.get(FROSTWIRE_LEVEL)
        set(value) = entityData.set(FROSTWIRE_LEVEL, value)

    var slingshotLevel: Int
        get() = entityData.get(SLINGSHOT_LEVEL)
        set(value) = entityData.set(SLINGSHOT_LEVEL, value)

    var disarmLevel: Int
        get() = entityData.get(DISARM_LEVEL)
        set(value) = entityData.set(DISARM_LEVEL, value)

    var extendedChainLevel: Int
        get() = entityData.get(EXTENDED_CHAIN_LEVEL)
        set(value) = entityData.set(EXTENDED_CHAIN_LEVEL, value)

    var rapidReelLevel: Int
        get() = entityData.get(RAPID_REEL_LEVEL)
        set(value) = entityData.set(RAPID_REEL_LEVEL, value)

    var piercingSpikeLevel: Int
        get() = entityData.get(PIERCING_SPIKE_LEVEL)
        set(value) = entityData.set(PIERCING_SPIKE_LEVEL, value)

    fun getMaxRange(): Double = MAX_RANGE + 6.0 * extendedChainLevel

    var launchedStack: ItemStack = ItemStack.EMPTY
    var rappelDirection: Int = 0
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
        builder.define(IS_CLINGING, false)
        builder.define(SLACK_DISTANCE, 0.0f)
        builder.define(FROSTWIRE_LEVEL, 0)
        builder.define(SLINGSHOT_LEVEL, 0)
        builder.define(DISARM_LEVEL, 0)
        builder.define(EXTENDED_CHAIN_LEVEL, 0)
        builder.define(RAPID_REEL_LEVEL, 0)
        builder.define(PIERCING_SPIKE_LEVEL, 0)
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
        this.slackDistance = 0.0f
        this.rappelDirection = 0
        this.launchedStack = player.getItemInHand(hand)
        if (!launchedStack.isEmpty) {
            this.frostwireLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.FROSTWIRE, level())
            this.slingshotLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.SLINGSHOT, level())
            this.disarmLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.DISARM, level())
            this.extendedChainLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.EXTENDED_CHAIN, level())
            this.rapidReelLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.RAPID_REEL, level())
            this.piercingSpikeLevel = ModEnchantments.getLevel(launchedStack, ModEnchantments.PIERCING_SPIKE, level())
        }
        this.setNoGravity(true)
        this.noPhysics = true

        val look = player.lookAngle.normalize()
        val eyePos = player.eyePosition
        val startPos = eyePos.add(look.scale(0.35))
        this.flightOrigin = startPos
        this.setPos(startPos.x, startPos.y, startPos.z)
        val flySpeedMultiplier = 1.0 + 0.25 * rapidReelLevel
        this.deltaMovement = look.scale(FLY_SPEED * flySpeedMultiplier)
        this.hookPosition = startPos
        this.hookDirection = -1

        val yaw = player.yRot
        val pitch = player.xRot
        this.setYRot(yaw)
        this.setXRot(pitch)
        this.yRotO = yaw
        this.xRotO = pitch
    }

    /**
     * Rappelle immédiatement le grappin vers le joueur.
     */
    fun retract() {
        if (isClinging) {
            isClinging = false
            slackDistance = 0.0f
            rappelDirection = 0
            getOwnerEntity()?.let {
                it.setNoGravity(false)
                cancelPlayerFall(it)
            }
        }
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

    override fun remove(reason: Entity.RemovalReason) {
        if (isClinging) {
            isClinging = false
            slackDistance = 0.0f
            rappelDirection = 0
            getOwnerEntity()?.let {
                it.setNoGravity(false)
                cancelPlayerFall(it)
            }
        }
        super.remove(reason)
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

    override fun shouldRender(camX: Double, camY: Double, camZ: Double): Boolean {
        if (super.shouldRender(camX, camY, camZ)) return true
        val owner = getOwnerEntity() ?: return false
        return owner.shouldRender(camX, camY, camZ)
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

        // Si le joueur ne tient plus le grappin dans la main du tir ou change de slot hotbar, annule immédiatement
        val currentStack = owner.getItemInHand(usedHand)
        val isHoldingClawshot = currentStack.item is ClawshotItem &&
            (launchedStack.isEmpty || currentStack === launchedStack)

        if (!isHoldingClawshot) {
            if (isClinging) {
                isClinging = false
                slackDistance = 0.0f
                rappelDirection = 0
                owner.setNoGravity(false)
                cancelPlayerFall(owner)
            }
            if (!level().isClientSide && anchorState != AnchorState.RETRACTING) {
                retract()
            }
        }

        // Côté Client : Uniquement interpolation visuelle et suivi d'affichage
        if (level().isClientSide) {
            tickClient(owner)
            return
        }

        val serverLevel = level() as? ServerLevel ?: return

        if (frostwireLevel > 0) {
            tickFrostwire(serverLevel, owner)
        }

        when (anchorState) {
            AnchorState.FLYING -> tickFlying(serverLevel, owner)
            AnchorState.HOOKED_BLOCK -> tickHookedBlock(serverLevel, owner)
            AnchorState.HOOKED_ENTITY -> tickHookedEntity(serverLevel, owner)
            AnchorState.RETRACTING -> tickRetracting(serverLevel, owner)
        }
    }

    private fun tickClient(owner: Player) {
        // Détection de changement d'item côté client pour décrochage immédiat
        val currentStack = owner.getItemInHand(usedHand)
        val isHoldingClawshot = currentStack.item is ClawshotItem &&
            (launchedStack.isEmpty || currentStack === launchedStack)

        if (!isHoldingClawshot && owner.isLocalPlayer) {
            if (isClinging) {
                isClinging = false
                slackDistance = 0.0f
                rappelDirection = 0
                if (owner.isNoGravity) {
                    owner.setNoGravity(false)
                }
                cancelPlayerFall(owner)
            }
        }

        if (frostwireLevel > 0 && flightTicks % 3 == 0) {
            val start = owner.position().add(0.0, 0.8, 0.0)
            val end = position()
            val diff = end.subtract(start)
            val len = diff.length()
            if (len > 0.5) {
                val dir = diff.normalize()
                val offset = level().random.nextDouble() * len
                val p = start.add(dir.scale(offset))
                level().addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 0.0, 0.01, 0.0)
            }
        }

        when (anchorState) {
            AnchorState.FLYING -> {
                clawOpenAmount = (clawOpenAmount + 0.25f).coerceAtMost(1.0f)
            }
            AnchorState.HOOKED_BLOCK -> {
                clawOpenAmount = (clawOpenAmount - 0.35f).coerceAtLeast(0.0f)
                val anchorPos = hookPosition
                setPos(anchorPos.x, anchorPos.y, anchorPos.z)

                // Stabilisation côté client pour le joueur contrôlé localement (évite tout tremblement / gravité)
                if (owner.isLocalPlayer) {
                    cancelPlayerFall(owner)
                    if (owner.isShiftKeyDown) {
                        if (owner.isNoGravity) {
                            owner.setNoGravity(false)
                        }
                        return
                    }

                    val normal = getFacingNormal()
                    val targetPos = computeClingingPosition(anchorPos, normal, slackDistance.toDouble())
                    val playerPos = owner.position()
                    val distToTarget = targetPos.distanceTo(playerPos)
                    val distToAnchor = anchorPos.distanceTo(playerPos)

                    if (isClinging || distToTarget <= 1.25 || distToAnchor <= 2.35) {
                        owner.setNoGravity(true)
                        applyClingingSuspension(owner, targetPos, playerPos)
                    }
                }
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
                if (owner.isLocalPlayer && owner.isNoGravity) {
                    owner.setNoGravity(false)
                }
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
        val blockDist = if (blockHit.type != HitResult.Type.MISS) currentPos.distanceTo(blockHit.location) else Double.MAX_VALUE

        // 2. Détection de collision d'entités vivantes ou items sur tout le vecteur de vol
        val entityHit = findEntityOnPath(currentPos, nextPos, owner)

        if (entityHit != null && entityHit.distance <= blockDist) {
            val targetEntity = entityHit.entity
            // Impact sur une entité
            hookedTargetId = targetEntity.id
            anchorState = AnchorState.HOOKED_ENTITY
            val hitPos = targetEntity.position().add(0.0, targetEntity.bbHeight * 0.5, 0.0)
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

                // Double Clawshot (Zelda TP) :
                // Si le joueur était déjà suspendu à un mur avec son autre grappin,
                // l'ancien grappin se détache automatiquement pour enchaîner les prises !
                val otherHand = if (usedHand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
                val otherAnchor = ClawshotItem.findActiveAnchor(serverLevel, owner, otherHand)
                if (otherAnchor != null && otherAnchor.isAlive) {
                    otherAnchor.retract()
                }

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

        // Limite de portée maximale (allongée par Extended Chain)
        val maxAllowedRange = getMaxRange()
        val distFromOwner = nextPos.distanceTo(owner.eyePosition)
        val maxFlightTicks = 35 + extendedChainLevel * 8
        if (distFromOwner > maxAllowedRange || flightTicks > maxFlightTicks) {
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

        val normal = getFacingNormal()
        val baseTargetPos = computeClingingPosition(anchorPos, normal, 0.0)
        val playerPos = owner.position()
        val toBaseTarget = baseTargetPos.subtract(playerPos)
        val distToBaseTarget = toBaseTarget.length()
        val distToAnchor = anchorPos.distanceTo(playerPos)

        // 1. ANNULATION CONTINUE DES DÉGÂTS DE CHUTE
        cancelPlayerFall(owner)

        // 2. Décrochage manuel : Le joueur s'accroupit (Shift / Sneak) pour lâcher prise (avec élan Slingshot si en vol)
        if (owner.isShiftKeyDown) {
            handleManualRelease(owner)
            return
        }

        // 3. Vérification que le bloc cible existe toujours
        val hitBlockPos = BlockPos.containing(anchorPos.subtract(normal.scale(0.2)))
        val hitState = serverLevel.getBlockState(hitBlockPos)
        if (!isValidHookTarget(hitState, hitBlockPos) && hookedTicks > 5) {
            retract()
            return
        }

        // 4. Physique Zelda TP : Traction initiale vers la cible, puis verrouillage stable (latch)
        if (!isClinging) {
            if (distToBaseTarget <= 1.25 || distToAnchor <= 2.35) {
                isClinging = true
                owner.setNoGravity(true)
                owner.setDeltaMovement(0.0, 0.0, 0.0)
                owner.hurtMarked = true
            } else {
                applySmoothPull(serverLevel, owner, toBaseTarget, distToBaseTarget)
                return
            }
        }

        // 5. Maintien fixe et gestion du rappel / mou de chaîne (Zelda TP)
        if (isClinging) {
            if (rappelDirection == -1) {
                // Descendre le long de la chaîne (lâcher du mou)
                val currentTarget = computeClingingPosition(anchorPos, normal, slackDistance.toDouble())
                if (canDescendFurther(serverLevel, owner, currentTarget)) {
                    val maxSlack = (getMaxRange() - 2.5).toFloat()
                    if (slackDistance < maxSlack) {
                        slackDistance = (slackDistance + 0.18f).coerceAtMost(maxSlack)
                        if (hookedTicks % 4 == 0) {
                            serverLevel.playSound(
                                null,
                                owner.blockPosition(),
                                SoundEvents.CHAIN_STEP,
                                SoundSource.PLAYERS,
                                0.45f,
                                1.35f + (hookedTicks % 6) * 0.03f
                            )
                        }
                    }
                }
            } else if (rappelDirection == 1) {
                // Remonter le long de la chaîne
                if (slackDistance > 0.0f) {
                    slackDistance = (slackDistance - 0.18f).coerceAtLeast(0.0f)
                    if (hookedTicks % 4 == 0) {
                        serverLevel.playSound(
                            null,
                            owner.blockPosition(),
                            SoundEvents.CHAIN_STEP,
                            SoundSource.PLAYERS,
                            0.45f,
                            1.55f + (hookedTicks % 6) * 0.03f
                        )
                    }
                }
            }
        }

        owner.setNoGravity(true)
        val targetPosWithSlack = computeClingingPosition(anchorPos, normal, slackDistance.toDouble())
        applyClingingSuspension(owner, targetPosWithSlack, playerPos)
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

            // Dégâts d'impact et désarmement lors de la prise initiale
            if (hookedTicks == 1 && target is LivingEntity) {
                applyEntityImpact(serverLevel, owner, target)
            }

            if (dist < 2.0 || owner.isShiftKeyDown || hookedTicks > 100) {
                if (owner.isShiftKeyDown && slingshotLevel > 0 && dist > 1.2) {
                    applySlingshotBoost(owner)
                } else {
                    releasePlayerAtDestination(owner, dist < 2.5)
                }
                retract()
                return
            }

            applySmoothPull(serverLevel, owner, toTarget, dist)
        } else {
            // --- MOB LÉGER : Le mob est tracté vers le joueur ---
            val toPlayer = owner.eyePosition.subtract(target.position())
            val dist = toPlayer.length()

            // Dégâts et désarmement côté serveur
            if (hookedTicks == 1 && target is LivingEntity) {
                applyEntityImpact(serverLevel, owner, target)
            }

            if (dist < 1.8 || hookedTicks > 60) {
                target.setDeltaMovement(0.0, 0.1, 0.0)
                target.hurtMarked = true
                retract()
                return
            }

            // Traction du mob vers Link/le joueur (accélérée par Rapid Reel)
            val pullDir = toPlayer.normalize()
            val pullSpeedMultiplier = 1.0 + 0.25 * rapidReelLevel
            val pullSpeed = 0.85 * pullSpeedMultiplier
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
    private fun applySmoothPull(serverLevel: ServerLevel, player: Player, toTarget: Vec3, dist: Double) {
        val dir = toTarget.normalize()
        val speedMultiplier = 1.0 + 0.25 * rapidReelLevel
        val speed = (PULL_SPEED * speedMultiplier).coerceAtMost((dist * 0.75).coerceAtLeast(0.35))
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
        isClinging = false
        slackDistance = 0.0f
        rappelDirection = 0
        player.setNoGravity(false)
        cancelPlayerFall(player)
        if (giveHop) {
            val currentVel = player.deltaMovement
            player.setDeltaMovement(currentVel.x * 0.5, 0.28, currentVel.z * 0.5)
            player.hurtMarked = true
        }
    }

    /**
     * Gère le décrochage manuel (Shift ou re-clic droit). Si Slingshot est actif pendant la traction,
     * convertit l'inertie en projection catapultée !
     */
    fun handleManualRelease(player: Player) {
        val normal = getFacingNormal()
        val baseTargetPos = computeClingingPosition(hookPosition, normal, 0.0)
        val distToBaseTarget = baseTargetPos.subtract(player.position()).length()

        if (slingshotLevel > 0 && !isClinging && anchorState == AnchorState.HOOKED_BLOCK && distToBaseTarget > 1.2) {
            applySlingshotBoost(player)
        } else {
            releasePlayerAtDestination(player, false)
        }
        retract()
    }

    /**
     * Propulse le joueur avec une violente impulsion cinétique (Enchantement Élan Cinétique / Slingshot).
     */
    fun applySlingshotBoost(player: Player) {
        val serverLevel = level() as? ServerLevel
        isClinging = false
        slackDistance = 0.0f
        rappelDirection = 0
        player.setNoGravity(false)
        cancelPlayerFall(player)

        val lookDir = player.lookAngle.normalize()
        val slingshotMultiplier = 1.25 + 0.45 * slingshotLevel
        val speedMultiplier = 1.0 + 0.25 * rapidReelLevel
        val boostSpeed = PULL_SPEED * slingshotMultiplier * speedMultiplier
        val upwardBoost = 0.28 + 0.12 * slingshotLevel
        val boostedVel = lookDir.scale(boostSpeed).add(0.0, upwardBoost, 0.0)

        player.setDeltaMovement(boostedVel.x, boostedVel.y, boostedVel.z)
        player.hurtMarked = true

        if (serverLevel != null) {
            serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS,
                1.0f,
                1.2f + slingshotLevel * 0.15f
            )
            serverLevel.sendParticles(
                ParticleTypes.GUST,
                player.x, player.y + 0.5, player.z,
                6, 0.2, 0.2, 0.2, 0.08
            )
        }
    }

    /**
     * Gère l'impact initial sur une créature vivante (dégâts Harpon Piquant + Désarmement).
     */
    private fun applyEntityImpact(serverLevel: ServerLevel, owner: Player, target: LivingEntity) {
        // 1. Dégâts d'impact augmentés par Piercing Spike
        val damage = 2.0f + 3.0f * piercingSpikeLevel
        target.hurtServer(
            serverLevel,
            target.damageSources().mobAttack(owner),
            damage
        )

        // 2. Enchantement Désarmement (Disarm) : arrache l'arme en main principale
        if (disarmLevel > 0) {
            val heldItem = target.getItemInHand(InteractionHand.MAIN_HAND)
            if (!heldItem.isEmpty) {
                val snatched = heldItem.copy()
                target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)

                val dropPos = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
                val itemEntity = ItemEntity(serverLevel, dropPos.x, dropPos.y, dropPos.z, snatched)
                itemEntity.setPickUpDelay(0)
                val toOwner = owner.eyePosition.subtract(dropPos).normalize().scale(0.85)
                itemEntity.deltaMovement = toOwner
                serverLevel.addFreshEntity(itemEntity)

                serverLevel.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.ITEM_BREAK.value(),
                    SoundSource.PLAYERS,
                    0.85f,
                    1.3f
                )
                serverLevel.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.CHAIN_HIT,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.6f
                )
            }
        }
    }

    /**
     * Gère la chaîne cryogénique Fil de Givre (Frostwire) : inflige gel et ralentissement aux entités qui la coupent.
     */
    private fun tickFrostwire(serverLevel: ServerLevel, owner: Player) {
        val start = owner.position().add(0.0, 0.8, 0.0)
        val end = position()
        val diff = end.subtract(start)
        val length = diff.length()
        if (length <= 0.5) return

        val dir = diff.normalize()
        // Particules de flocons de givre le long de la ligne
        if (flightTicks % 2 == 0) {
            var step = 0.5
            while (step < length) {
                val pPos = start.add(dir.scale(step))
                serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    pPos.x, pPos.y, pPos.z,
                    1, 0.05, 0.05, 0.05, 0.01
                )
                step += 1.25
            }
        }

        if (flightTicks % 20 == 0) {
            val center = start.add(diff.scale(0.5))
            serverLevel.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS,
                0.4f,
                1.7f
            )
        }

        val box = AABB(start.x, start.y, start.z, end.x, end.y, end.z).inflate(0.75)
        val targets = serverLevel.getEntities(this, box) {
            it != owner && it != this && it.isAlive && it is LivingEntity
        }

        for (target in targets) {
            if (target !is LivingEntity) continue
            val p = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
            val distToSegment = distanceToSegment(p, start, end)
            if (distToSegment <= (target.bbWidth * 0.5 + 0.4)) {
                target.ticksFrozen = (target.ticksFrozen + 25 * frostwireLevel).coerceAtMost(300)
                target.addEffect(
                    MobEffectInstance(
                        MobEffects.SLOWNESS,
                        40,
                        (frostwireLevel - 1).coerceAtLeast(0),
                        false,
                        true
                    )
                )
                if (flightTicks % 8 == 0) {
                    target.hurtServer(serverLevel, target.damageSources().freeze(), 1.5f * frostwireLevel)
                }
            }
        }
    }

    private fun distanceToSegment(point: Vec3, segStart: Vec3, segEnd: Vec3): Double {
        val segVec = segEnd.subtract(segStart)
        val segLenSq = segVec.lengthSqr()
        if (segLenSq < 1e-6) {
            return point.distanceTo(segStart)
        }
        val toPoint = point.subtract(segStart)
        val t = (toPoint.dot(segVec) / segLenSq).coerceIn(0.0, 1.0)
        val projection = segStart.add(segVec.scale(t))
        return point.distanceTo(projection)
    }

    /**
     * Renvoie la normale orientée de la face de bloc accrochée.
     */
    fun getFacingNormal(): Vec3 {
        return if (hookDirection >= 0) {
            val dir = Direction.from3DDataValue(hookDirection)
            Vec3(dir.stepX.toDouble(), dir.stepY.toDouble(), dir.stepZ.toDouble())
        } else {
            Vec3.ZERO
        }
    }

    /**
     * Calcule la position idéale où le joueur doit rester suspendu sans heurter les colliders de blocs,
     * en prenant en compte le mou relâché sur la chaîne (rappel).
     */
    fun computeClingingPosition(anchorPos: Vec3, normal: Vec3, slack: Double = slackDistance.toDouble()): Vec3 {
        return when (hookDirection) {
            Direction.DOWN.get3DDataValue() -> {
                // Plafond : Pieds à 1.95m + slack sous l'ancre (tête à 15cm sous le plafond à slack=0)
                Vec3(anchorPos.x, anchorPos.y - 1.95 - slack, anchorPos.z)
            }
            Direction.UP.get3DDataValue() -> {
                // Sol : Pieds posés au-dessus de l'ancre
                Vec3(anchorPos.x, anchorPos.y + 0.05, anchorPos.z)
            }
            else -> {
                // Mur vertical ou angle : Pieds à 1.35m + slack sous l'ancre et centre à 0.50m du mur
                val hx = if (normal.lengthSqr() > 0.01) normal.x else {
                    if (hookDirection >= 0) Direction.from3DDataValue(hookDirection).stepX.toDouble() else 0.0
                }
                val hz = if (normal.lengthSqr() > 0.01) normal.z else {
                    if (hookDirection >= 0) Direction.from3DDataValue(hookDirection).stepZ.toDouble() else 0.0
                }
                val hLen = Math.sqrt(hx * hx + hz * hz)
                if (hLen > 0.01) {
                    val normX = hx / hLen
                    val normZ = hz / hLen
                    Vec3(
                        anchorPos.x + normX * 0.50,
                        anchorPos.y - 1.35 - slack,
                        anchorPos.z + normZ * 0.50
                    )
                } else {
                    Vec3(anchorPos.x, anchorPos.y - 1.35 - slack, anchorPos.z)
                }
            }
        }
    }

    /**
     * Détermine si le joueur peut encore descendre le long de la chaîne sans traverser le sol.
     */
    @Suppress("DEPRECATION")
    fun canDescendFurther(level: Level, player: Player, currentTargetPos: Vec3): Boolean {
        val feetY = currentTargetPos.y
        // Vérifie si le bloc sous les pieds du joueur est un sol solide bloquant le passage
        val checkBlockPos = BlockPos.containing(currentTargetPos.x, feetY - 0.05, currentTargetPos.z)
        val state = level.getBlockState(checkBlockPos)
        if (state.blocksMotion() && !state.isAir) {
            val shape = state.getCollisionShape(level, checkBlockPos)
            if (!shape.isEmpty) {
                val blockTopY = checkBlockPos.y + shape.max(Direction.Axis.Y)
                if (feetY <= blockTopY + 0.02) {
                    return false
                }
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Maintient fermement le joueur suspendu à la paroi en annulant la gravité et les tremblements.
     */
    fun applyClingingSuspension(player: Player, targetPos: Vec3, playerPos: Vec3) {
        cancelPlayerFall(player)
        val offset = targetPos.subtract(playerPos)
        val offsetLen = offset.length()

        if (offsetLen > 0.35) {
            // Rattrapage amorti si décalé
            val correction = offset.normalize().scale(0.25)
            player.setDeltaMovement(correction.x, correction.y, correction.z)
            player.hurtMarked = true
        } else if (offsetLen > 0.02) {
            // Doux rappel vers la position cible dès 2cm d'écart (absorbe les micro-mouvements sans à-coups)
            val correction = offset.scale(0.4)
            player.setDeltaMovement(correction.x, correction.y, correction.z)
            player.hurtMarked = true
        } else {
            // Parfaitement immobile
            if (player.deltaMovement.lengthSqr() > 0.0001) {
                player.setDeltaMovement(0.0, 0.0, 0.0)
                player.hurtMarked = true
            }
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

    data class EntityHitResultData(val entity: Entity, val location: Vec3, val distance: Double)

    private fun findEntityOnPath(from: Vec3, to: Vec3, owner: Player): EntityHitResultData? {
        val motion = to.subtract(from)
        val box = boundingBox.expandTowards(motion).inflate(1.2)
        val candidates = level().getEntities(this, box) { e ->
            e != owner && e != this && e.isAlive && !e.isSpectator && (e is LivingEntity || e is ItemEntity)
        }

        var closestHit: EntityHitResultData? = null
        var closestDist = Double.MAX_VALUE

        for (candidate in candidates) {
            // Marge généreuse adaptée à l'envergure des 3 griffes ouvertes du Clawshot (~0.45m)
            val candBox = candidate.boundingBox.inflate(0.45)

            if (candBox.contains(from)) {
                // L'entité englobe déjà l'ancre au début du tick
                val dist = 0.0
                if (dist < closestDist) {
                    closestDist = dist
                    closestHit = EntityHitResultData(candidate, from, dist)
                }
            } else {
                val clip = candBox.clip(from, to)
                if (clip.isPresent) {
                    val hitLoc = clip.get()
                    val dist = from.distanceTo(hitLoc)
                    if (dist < closestDist) {
                        closestDist = dist
                        closestHit = EntityHitResultData(candidate, hitLoc, dist)
                    }
                }
            }
        }

        return closestHit
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
        output.putBoolean("IsClinging", isClinging)
        output.putFloat("SlackDistance", slackDistance)
        output.putInt("HookDirection", hookDirection)
        output.putInt("FrostwireLevel", frostwireLevel)
        output.putInt("SlingshotLevel", slingshotLevel)
        output.putInt("DisarmLevel", disarmLevel)
        output.putInt("ExtendedChainLevel", extendedChainLevel)
        output.putInt("RapidReelLevel", rapidReelLevel)
        output.putInt("PiercingSpikeLevel", piercingSpikeLevel)
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
        isClinging = input.getBooleanOr("IsClinging", false)
        slackDistance = input.getFloatOr("SlackDistance", 0.0f)
        hookDirection = input.getIntOr("HookDirection", -1)
        frostwireLevel = input.getIntOr("FrostwireLevel", 0)
        slingshotLevel = input.getIntOr("SlingshotLevel", 0)
        disarmLevel = input.getIntOr("DisarmLevel", 0)
        extendedChainLevel = input.getIntOr("ExtendedChainLevel", 0)
        rapidReelLevel = input.getIntOr("RapidReelLevel", 0)
        piercingSpikeLevel = input.getIntOr("PiercingSpikeLevel", 0)
        val ownerStr = input.getStringOr("OwnerUuid", "")
        if (ownerStr.isNotEmpty()) {
            try {
                ownerUuid = UUID.fromString(ownerStr)
            } catch (_: Exception) {}
        }
    }
}
