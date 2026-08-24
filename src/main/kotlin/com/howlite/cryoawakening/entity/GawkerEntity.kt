package com.howlite.cryoawakening.entity

import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import com.howlite.cryoawakening.item.ModItems
import net.minecraft.core.particles.ColorParticleOption
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
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * GawkerEntity
 *
 * Petite créature cubique curieuse composée d'une grande bouche articulée,
 * de 4 petites pattes trottinantes et de 2 yeux sur tige type escargot.
 *
 * Interactions spéciales :
 * - Clic Droit avec Gunpowder : gavage jusqu'à 3 charges (vibration croissante, bouche béante, explosion surpuissante).
 * - Shift + Clic Droit à mains nues : le joueur le soulève au-dessus de sa tête.
 * - En vol après un lancer (chargé ou simple) : explose au contact d'un bloc ou d'une entité
 *   en créant une déflagration de glace (dégâts de zone, gel intense et effet Lenteur III).
 */
class GawkerEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    val isCarried: Boolean
        get() = entityData.get(CARRIER_ID) != -1

    val carrierId: Int
        get() = entityData.get(CARRIER_ID)

    var isThrown: Boolean
        get() = entityData.get(IS_THROWN)
        set(value) = entityData.set(IS_THROWN, value)

    var powderCharge: Int
        get() = entityData.get(POWDER_CHARGE)
        set(value) = entityData.set(POWDER_CHARGE, value.coerceIn(0, 3))

    var drossCharge: Int
        get() = entityData.get(DROSS_CHARGE)
        set(value) = entityData.set(DROSS_CHARGE, value.coerceIn(0, 3))

    var throwerUuid: UUID? = null
    var flightTicks: Int = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CARRIER_ID, -1)
        builder.define(IS_THROWN, false)
        builder.define(POWDER_CHARGE, 0)
        builder.define(DROSS_CHARGE, 0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, MeleeAttackGoal(this, 1.25, false))
        goalSelector.addGoal(2, TemptGoal(this, 1.15, { isTemptingItem(it) }, false))
        goalSelector.addGoal(3, WaterAvoidingRandomStrollGoal(this, 0.75))
        goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 12.0f))
        goalSelector.addGoal(5, RandomLookAroundGoal(this))

        // Riposte s'il est attaqué
        targetSelector.addGoal(1, HurtByTargetGoal(this))
    }

    private fun isTemptingItem(stack: ItemStack): Boolean {
        return stack.`is`(ModItems.RAW_BISMUTH) || stack.`is`(Items.GUNPOWDER) || stack.`is`(ModItems.BISMUTH_DROSS)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val heldItem = player.getItemInHand(hand)

        // 1. Gavage à la poudre à canon (Gunpowder) : jusqu'à 3 charges
        if (heldItem.`is`(Items.GUNPOWDER) && !isThrown && drossCharge == 0) {
            if (powderCharge < 3) {
                if (!player.abilities.instabuild) {
                    heldItem.shrink(1)
                }
                powderCharge++

                val pitch = 1.0f + (powderCharge * 0.2f)
                level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.TNT_PRIMED,
                    SoundSource.NEUTRAL,
                    1.0f,
                    pitch
                )
                level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.GENERIC_EAT.value(),
                    SoundSource.NEUTRAL,
                    1.0f,
                    1.2f
                )

                // Particules d'étincelles et de fumée au moment du gavage
                if (level() is ServerLevel) {
                    val serverLevel = level() as ServerLevel
                    val pos = position().add(0.0, 0.35, 0.0)
                    serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 12, 0.15, 0.15, 0.15, 0.05)
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.02)
                }

                return InteractionResult.SUCCESS
            }
        }

        // 1b. Gavage au Bismuth Dross (Feu d'artifice) : jusqu'à 3 charges
        if (heldItem.`is`(ModItems.BISMUTH_DROSS) && !isThrown && powderCharge == 0) {
            if (drossCharge < 3) {
                if (!player.abilities.instabuild) {
                    heldItem.shrink(1)
                }
                drossCharge++

                val pitch = 1.0f + (drossCharge * 0.25f)
                level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.NEUTRAL,
                    1.0f,
                    pitch
                )
                level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.GENERIC_EAT.value(),
                    SoundSource.NEUTRAL,
                    1.0f,
                    1.3f
                )

                // Particules d'étincelles féeriques et de feu d'artifice au gavage
                if (level() is ServerLevel) {
                    val serverLevel = level() as ServerLevel
                    val pos = position().add(0.0, 0.35, 0.0)
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, 16, 0.2, 0.2, 0.2, 0.08)
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.05)
                }

                return InteractionResult.SUCCESS
            }
        }

        // 2. Shift + Clic Droit avec main vide : soulever le Gawker au-dessus de la tête
        if (player.isShiftKeyDown && heldItem.isEmpty && !isCarried && !isThrown) {
            if (!level().isClientSide) {
                this.entityData.set(CARRIER_ID, player.id)
                this.throwerUuid = player.uuid
                player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.3f
                )
            }
            return InteractionResult.SUCCESS
        }

        return super.mobInteract(player, hand)
    }

    override fun isPushable(): Boolean = !isCarried && !isThrown && super.isPushable()

    override fun canCollideWith(entity: Entity): Boolean {
        if (isCarried && entity.id == carrierId) return false
        return super.canCollideWith(entity)
    }

    /**
     * Lance le Gawker comme un projectile balistique (façon bombe de Zelda).
     */
    fun launch(thrower: Player, force: Float) {
        this.entityData.set(CARRIER_ID, -1)
        this.isThrown = true
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

    override fun tick() {
        super.tick()

        // Volutes de fumée continues si chargé à la gunpowder
        if (powderCharge > 0) {
            val freq = (4 - powderCharge).coerceAtLeast(1)
            if (tickCount % freq == 0) {
                val pos = position().add(0.0, 0.35, 0.0)
                if (level().isClientSide) {
                    level().addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0, 0.03 * powderCharge, 0.0)
                    if (powderCharge == 3 && tickCount % 2 == 0) {
                        level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05)
                    }
                }
            }
        }

        // Étincelles féeriques continues si chargé au Bismuth Dross (Feu d'artifice)
        if (drossCharge > 0) {
            val freq = (4 - drossCharge).coerceAtLeast(1)
            if (tickCount % freq == 0) {
                val pos = position().add(0.0, 0.35, 0.0)
                if (level().isClientSide) {
                    level().addParticle(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, (random.nextDouble() - 0.5) * 0.08, 0.03 * drossCharge, (random.nextDouble() - 0.5) * 0.08)
                    if (drossCharge == 3 && tickCount % 3 == 0) {
                        level().addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 1.0f, 0.85f, 0.2f), pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
                    }
                }
            }
        }

        // 1. Gestion du portage au-dessus de la tête
        if (isCarried) {
            val carrier = level().getEntity(carrierId) as? LivingEntity
            if (carrier == null || !carrier.isAlive || (carrier is Player && carrier.isSpectator)) {
                if (!level().isClientSide) {
                    entityData.set(CARRIER_ID, -1)
                }
            } else {
                // Positionnement fluide au-dessus de la tête du joueur
                val headPos = carrier.position().add(0.0, carrier.bbHeight + 0.15, 0.0)
                setPos(headPos.x, headPos.y, headPos.z)
                setDeltaMovement(0.0, 0.0, 0.0)
                fallDistance = 0.0

                yBodyRot = carrier.yBodyRot
                yHeadRot = carrier.yHeadRot
                xRot = 0.0f

                if (!level().isClientSide && carrier is Player) {
                    carrier.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 10, 0, false, false, false))
                }
            }
        }

        // 2. Gestion de l'état en vol après avoir été lancé
        if (isThrown) {
            flightTicks++

            if (level().isClientSide) {
                val pos = position().add(0.0, 0.25, 0.0)
                if (drossCharge > 0) {
                    // Traînée féerique de feu d'artifice
                    level().addParticle(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, -deltaMovement.x * 0.2, 0.05, -deltaMovement.z * 0.2)
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 0.0, 0.02, 0.0)
                } else {
                    // Traînée de particules de givre pendant le vol
                    level().addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, -deltaMovement.x * 0.15, 0.05, -deltaMovement.z * 0.15)
                    level().addParticle(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
                    if (powderCharge > 0) {
                        level().addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0.0, 0.05, 0.0)
                    }
                }
            } else {
                // Trajectoire balistique avec gravité
                deltaMovement = Vec3(deltaMovement.x * 0.98, deltaMovement.y - 0.045, deltaMovement.z * 0.98)

                // Détection d'impact (blocs, sol, ou entité vivante)
                val box = boundingBox.inflate(0.25)
                val hitEntity = level().getEntitiesOfClass(LivingEntity::class.java, box) { entity ->
                    entity != this && (entity.uuid != throwerUuid || flightTicks > 5)
                }.firstOrNull()

                if (horizontalCollision || verticalCollision || onGround() || hitEntity != null || flightTicks > 120) {
                    if (drossCharge > 0) {
                        explodeFirework()
                    } else {
                        explodeFrost()
                    }
                }
            }
        }
    }

    /**
     * Déclenche une explosion de feu d'artifice spectaculaire au contact d'un bloc ou d'une entité.
     * Les dégâts, la propulsion verticale et les gerbes de couleurs sont amplifiés par le niveau de charge (DrossCharge).
     */
    fun explodeFirework() {
        if (level().isClientSide) return
        val serverLevel = level() as? ServerLevel ?: return
        val center = position().add(0.0, 0.35, 0.0)

        val charge = drossCharge
        val radius = 4.0 + (charge * 0.9) // 4.0, 4.9, 5.8, 6.7 blocs
        val sparkCount = 60 + (charge * 35)

        // Effets spectaculaires de Feux d'artifice !
        serverLevel.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z, sparkCount, radius * 0.4, 1.2, radius * 0.4, 0.25)
        serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 1.0f, 0.85f, 0.2f), center.x, center.y, center.z, 1 + charge, 0.3, 0.3, 0.3, 0.0)
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1 + charge, 0.2, 0.2, 0.2, 0.0)
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 30 + charge * 15, 0.5, 0.5, 0.5, 0.1)

        // Sons d'explosion de fusée et d'étincelles féeriques
        level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 1.5f + charge * 0.3f, 0.9f)
        level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 1.5f + charge * 0.3f, 1.1f)
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f + charge * 0.2f, 1.3f)

        // Dégâts et projection aérienne spectaculaire (comme un feu d'artifice propulsé dans le ciel !)
        val victims = serverLevel.getEntitiesOfClass(
            LivingEntity::class.java,
            AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius)
        )

        for (target in victims) {
            if (target == this) continue
            val dist = target.position().distanceTo(center)
            if (dist <= radius) {
                val factor = (1.0 - (dist / radius)).coerceIn(0.0, 1.0)
                val baseDmg = 4.0 + (charge * 2.0)
                val maxDmgAdd = 6.0 + (charge * 3.0)
                val damage = (factor * maxDmgAdd + baseDmg).toFloat()

                val thrower = throwerUuid?.let { serverLevel.getEntity(it) as? LivingEntity }
                target.hurtServer(serverLevel, target.damageSources().explosion(this, thrower), damage)

                // Effet de lumière/brillance et propulsion vers le haut
                target.addEffect(MobEffectInstance(MobEffects.GLOWING, 100 + charge * 60, 0))

                val dir = target.position().subtract(center).normalize()
                val upForce = 0.6 + (charge * 0.35)
                val knockForce = 0.7f + (charge * 0.3f)
                target.push(dir.x * factor * knockForce, upForce * factor, dir.z * factor * knockForce)
                target.hurtMarked = true
            }
        }

        // Lâche une fourrure et des résidus de bismuth
        spawnAtLocation(serverLevel, ModItems.GAWKER_FUR)
        if (serverLevel.random.nextFloat() < 0.4f + charge * 0.2f) {
            spawnAtLocation(serverLevel, ModItems.BISMUTH_DROSS)
        }
        discard()
    }

    /**
     * Déclenche une explosion de glace au contact d'un bloc ou d'une entité.
     * Les dégâts, le rayon et les particules sont amplifiés par le niveau de charge (PowderCharge).
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

        // 1. Déclenchement d'une explosion physique modérée (Niveau 3 = petite explosion type mini-TNT)
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

        // 2. Dégâts et effets de gel de zone proportionnels
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

                // Application de gel et de ralentissement
                target.ticksFrozen = (target.ticksFrozen + 250 + charge * 80).coerceAtMost(600)
                val slownessAmp = (1 + (charge / 2)).coerceAtMost(3)
                target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 100 + charge * 30, slownessAmp))

                // Répulsion modérée
                val dir = target.position().subtract(center).normalize()
                val knockForce = 0.5f + (charge * 0.25f)
                target.push(dir.x * factor * knockForce, (0.25 + charge * 0.1) * factor, dir.z * factor * knockForce)
                target.hurtMarked = true
            }
        }

        // Lâche une fourrure à l'explosion
        spawnAtLocation(serverLevel, ModItems.GAWKER_FUR)
        discard()
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putInt("CarrierId", carrierId)
        output.putBoolean("IsThrown", isThrown)
        output.putInt("PowderCharge", powderCharge)
        output.putInt("DrossCharge", drossCharge)
        throwerUuid?.let { output.putString("ThrowerUuid", it.toString()) }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        val cid = input.getIntOr("CarrierId", -1)
        entityData.set(CARRIER_ID, cid)
        isThrown = input.getBooleanOr("IsThrown", false)
        powderCharge = input.getIntOr("PowderCharge", 0)
        drossCharge = input.getIntOr("DrossCharge", 0)
        val throwerStr = input.getStringOr("ThrowerUuid", "")
        if (throwerStr.isNotEmpty()) {
            try {
                throwerUuid = UUID.fromString(throwerStr)
            } catch (_: Exception) {}
        }
    }

    override fun getAmbientSound(): SoundEvent? = SoundEvents.RABBIT_AMBIENT
    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.RABBIT_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.RABBIT_DEATH

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Animations gérées de manière procédurale dans GawkerModel
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    companion object {
        val CARRIER_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GawkerEntity::class.java, EntityDataSerializers.INT)

        val IS_THROWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(GawkerEntity::class.java, EntityDataSerializers.BOOLEAN)

        val POWDER_CHARGE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GawkerEntity::class.java, EntityDataSerializers.INT)

        val DROSS_CHARGE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GawkerEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16) // Vitesse réduite
                .add(Attributes.STEP_HEIGHT, 0.6)
                .add(Attributes.TEMPT_RANGE, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
        }
    }
}
