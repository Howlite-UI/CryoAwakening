package com.howlite.cryoawakening.entity

import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import com.howlite.cryoawakening.item.GawkBombItem
import com.howlite.cryoawakening.item.ModItems
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * GawkBombEntity
 *
 * Entité 3D de la Gawk-Bomb (modèle Gawker avec texture dead_gawker.png).
 * Fonctionne en deux modes :
 * 1. Mode Lancer : Projectile balistique qui explose à l'impact.
 * 2. Mode Mine Terrestre : Piège statique posé au sol qui s'arme et explose à l'approche d'un ennemi.
 */
class GawkBombEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    var isMine: Boolean
        get() = entityData.get(IS_MINE)
        set(value) = entityData.set(IS_MINE, value)

    var powderCharge: Int
        get() = entityData.get(POWDER_CHARGE)
        set(value) = entityData.set(POWDER_CHARGE, value.coerceIn(0, 3))

    var isArmed: Boolean
        get() = entityData.get(IS_ARMED)
        set(value) = entityData.set(IS_ARMED, value)

    var fuseTicks: Int
        get() = entityData.get(FUSE_TICKS)
        set(value) = entityData.set(FUSE_TICKS, value)

    var throwerUuid: UUID? = null
    var flightTicks: Int = 0
    private var armTimer: Int = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(IS_MINE, false)
        builder.define(POWDER_CHARGE, 0)
        builder.define(IS_ARMED, false)
        builder.define(FUSE_TICKS, -1)
    }

    override fun registerGoals() {
        // Pas d'IA classique, l'entité est contrôlée par sa physique de projectile ou son rôle de mine
    }

    /**
     * Initialise la Gawk-Bomb en projectile lancé.
     */
    fun setupThrow(thrower: LivingEntity, force: Float, charge: Int) {
        this.isMine = false
        this.powderCharge = charge
        this.throwerUuid = thrower.uuid
        this.flightTicks = 0

        val look = thrower.lookAngle
        val velX = look.x * force
        val velY = look.y * force + 0.35 * (force / 1.5)
        val velZ = look.z * force
        this.deltaMovement = Vec3(velX, velY, velZ)
        this.hurtMarked = true

        level().playSound(
            null,
            thrower.blockPosition(),
            SoundEvents.SNOWBALL_THROW,
            SoundSource.PLAYERS,
            1.2f,
            0.8f + (force * 0.25f)
        )
    }

    /**
     * Initialise la Gawk-Bomb en mine posée au sol.
     */
    fun setupMine(x: Double, y: Double, z: Double, charge: Int, placer: Player?) {
        this.isMine = true
        this.powderCharge = charge
        this.isArmed = false
        this.fuseTicks = -1
        this.armTimer = 0
        this.throwerUuid = placer?.uuid
        setPos(x, y, z)
        setDeltaMovement(0.0, 0.0, 0.0)

        level().playSound(
            null,
            blockPosition(),
            SoundEvents.STONE_PLACE,
            SoundSource.BLOCKS,
            1.0f,
            1.1f
        )
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        // Récupérer la mine posée au sol si elle n'est pas en train d'exploser
        if (isMine && fuseTicks == -1 && !level().isClientSide) {
            val dropStack = GawkBombItem.createWithCharge(powderCharge)
            spawnAtLocation(level() as ServerLevel, dropStack)
            player.level().playSound(
                null,
                blockPosition(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                1.0f,
                1.2f
            )
            discard()
            return InteractionResult.SUCCESS
        }
        return super.mobInteract(player, hand)
    }

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, amount: Float): Boolean {
        if (isMine && fuseTicks == -1) {
            val attacker = damageSource.entity
            if (attacker is Player) {
                val dropStack = GawkBombItem.createWithCharge(powderCharge)
                spawnAtLocation(serverLevel, dropStack)
                serverLevel.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.2f
                )
                discard()
                return true
            }
            // Si attaquée par un monstre / dégât quelconque : amorcer la détonation
            fuseTicks = 8
            return true
        }
        return super.hurtServer(serverLevel, damageSource, amount)
    }

    override fun tick() {
        super.tick()

        // 1. Mode Mine au sol
        if (isMine) {
            if (!isNoGravity()) {
                deltaMovement = Vec3(0.0, -0.08, 0.0)
            }

            if (!level().isClientSide) {
                // Phase d'armement initial (1 seconde = 20 ticks)
                if (!isArmed) {
                    armTimer++
                    if (armTimer >= 20) {
                        isArmed = true
                        level().playSound(
                            null,
                            blockPosition(),
                            SoundEvents.TRIPWIRE_ATTACH,
                            SoundSource.BLOCKS,
                            0.8f,
                            1.4f
                        )
                        val pos = position().add(0.0, 0.3, 0.0)
                        (level() as ServerLevel).sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 5, 0.1, 0.1, 0.1, 0.02)
                    }
                } else if (fuseTicks == -1) {
                    // Détection de proximité des ennemis (2.5 blocs)
                    val detectionBox = boundingBox.inflate(2.5)
                    val nearbyEnemy = level().getEntitiesOfClass(LivingEntity::class.java, detectionBox) { entity ->
                        entity != this && (entity is Enemy || entity.type.category == MobCategory.MONSTER) && entity.isAlive
                    }.firstOrNull()

                    if (nearbyEnemy != null) {
                        // Détection ! Allumage de la mèche
                        fuseTicks = 12
                        level().playSound(
                            null,
                            blockPosition(),
                            SoundEvents.TNT_PRIMED,
                            SoundSource.BLOCKS,
                            1.0f,
                            1.2f + (powderCharge * 0.2f)
                        )
                    }
                } else if (fuseTicks > 0) {
                    fuseTicks--
                    val pos = position().add(0.0, 0.35, 0.0)
                    (level() as ServerLevel).sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 3, 0.08, 0.08, 0.08, 0.02)
                    if (fuseTicks == 0) {
                        explodeFrost()
                    }
                }
            }
            return
        }

        // 2. Mode Projectile lancé
        flightTicks++
        if (level().isClientSide) {
            val pos = position().add(0.0, 0.25, 0.0)
            level().addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, -deltaMovement.x * 0.15, 0.05, -deltaMovement.z * 0.15)
            level().addParticle(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
            if (powderCharge > 0) {
                level().addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0, 0.04, 0.0)
            }
        } else {
            deltaMovement = Vec3(deltaMovement.x * 0.98, deltaMovement.y - 0.045, deltaMovement.z * 0.98)

            val box = boundingBox.inflate(0.25)
            val hitEntity = level().getEntitiesOfClass(LivingEntity::class.java, box) { entity ->
                entity != this && (entity.uuid != throwerUuid || flightTicks > 5)
            }.firstOrNull()

            if (horizontalCollision || verticalCollision || onGround() || hitEntity != null || flightTicks > 120) {
                explodeFrost()
            }
        }
    }

    /**
     * Déclenche la détonation de glace étalonnée selon la charge (PowderCharge).
     */
    fun explodeFrost() {
        if (level().isClientSide) return
        val serverLevel = level() as? ServerLevel ?: return
        val center = position().add(0.0, 0.25, 0.0)

        val charge = powderCharge
        val radius = 3.5 + (charge * 0.7) // 3.5, 4.2, 4.9, 5.6 blocs
        val snowflakeCount = 40 + (charge * 20)
        val explosionCount = 1 + (charge * 1)

        // Effets visuels & sonores
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, explosionCount, 0.2 * charge, 0.2 * charge, 0.2 * charge, 0.0)
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, snowflakeCount, radius * 0.35, 1.0, radius * 0.35, 0.15)
        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, center.x, center.y, center.z, 20 + charge * 10, 0.8, 0.8, 0.8, 0.1)

        val soundPitch = (1.2f - charge * 0.12f).coerceAtLeast(0.8f)
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f + charge * 0.2f, soundPitch)
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f + charge * 0.2f, 0.8f)
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.2f + charge * 0.1f, 0.9f)

        // 1. Explosion physique modérée selon le niveau (Niveau 3 = petite explosion de TNT)
        if (charge >= 1) {
            val explosionPower = when (charge) {
                1 -> 0.5f
                2 -> 1.0f
                else -> 1.6f
            }
            val thrower = throwerUuid?.let { serverLevel.getEntity(it) as? LivingEntity }
            val dmgSource = damageSources().explosion(this, thrower)
            serverLevel.explode(
                this,
                dmgSource,
                null,
                center.x,
                center.y,
                center.z,
                explosionPower,
                false,
                Level.ExplosionInteraction.MOB
            )
        }

        // 2. Onde de choc et dégâts de gel
        val victims = serverLevel.getEntitiesOfClass(
            LivingEntity::class.java,
            AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius)
        )

        for (target in victims) {
            if (target == this) continue
            val dist = target.position().distanceTo(center)
            if (dist <= radius) {
                val factor = (1.0 - (dist / radius)).coerceIn(0.0, 1.0)
                val baseDmg = 2.5 + (charge * 1.2)
                val maxDmgAdd = 4.5 + (charge * 2.0)
                val damage = (factor * maxDmgAdd + baseDmg).toFloat()
                target.hurtServer(serverLevel, target.damageSources().freeze(), damage)

                // Gel et ralentissement
                target.ticksFrozen = (target.ticksFrozen + 250 + charge * 80).coerceAtMost(600)
                val slownessAmp = (1 + (charge / 2)).coerceAtMost(3)
                target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 100 + charge * 30, slownessAmp))

                // Répulsion
                val dir = target.position().subtract(center).normalize()
                val knockForce = 0.5f + (charge * 0.25f)
                target.push(dir.x * factor * knockForce, (0.25 + charge * 0.1) * factor, dir.z * factor * knockForce)
                target.hurtMarked = true
            }
        }

        discard()
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putBoolean("IsMine", isMine)
        output.putInt("PowderCharge", powderCharge)
        output.putBoolean("IsArmed", isArmed)
        output.putInt("FuseTicks", fuseTicks)
        throwerUuid?.let { output.putString("ThrowerUuid", it.toString()) }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        isMine = input.getBooleanOr("IsMine", false)
        powderCharge = input.getIntOr("PowderCharge", 0)
        isArmed = input.getBooleanOr("IsArmed", false)
        fuseTicks = input.getIntOr("FuseTicks", -1)
        val throwerStr = input.getStringOr("ThrowerUuid", "")
        if (throwerStr.isNotEmpty()) {
            try {
                throwerUuid = UUID.fromString(throwerStr)
            } catch (_: Exception) {}
        }
    }

    override fun getAmbientSound(): SoundEvent? = null
    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.SNOW_HIT
    override fun getDeathSound(): SoundEvent = SoundEvents.SNOW_BREAK

    override fun isPushable(): Boolean = !isMine && super.isPushable()

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Animations gérées de manière procédurale dans GawkBombModel
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    companion object {
        val IS_MINE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(GawkBombEntity::class.java, EntityDataSerializers.BOOLEAN)

        val POWDER_CHARGE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GawkBombEntity::class.java, EntityDataSerializers.INT)

        val IS_ARMED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(GawkBombEntity::class.java, EntityDataSerializers.BOOLEAN)

        val FUSE_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GawkBombEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.STEP_HEIGHT, 0.5)
        }
    }
}
