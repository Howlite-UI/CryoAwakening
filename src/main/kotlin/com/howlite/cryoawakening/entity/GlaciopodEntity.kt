package com.howlite.cryoawakening.entity

import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

/**
 * GlaciopodEntity
 *
 * Cloporte géant ancien adapté au grand froid.
 * Il hiberne initialement sous forme d'un bloc rocheux 2x2x1 compact,
 * et se déplie de manière articulée et procédurale lorsqu'il subit des dégâts.
 */
class GlaciopodEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    /**
     * Progression d'ouverture (0.0f = enroulé en bloc 2x2x1, 1.0f = totalement déplié).
     */
    var unfurlProgress: Float = 0.0f
        private set
    var prevUnfurlProgress: Float = 0.0f
        private set

    // Historique des angles Yaw pour la courbure serpentine des 16 segments
    val currentSegmentYaws = FloatArray(16)
    val prevSegmentYaws = FloatArray(16)

    init {
        for (i in 0 until 16) {
            currentSegmentYaws[i] = yBodyRot
            prevSegmentYaws[i] = yBodyRot
        }
    }

    var isHibernating: Boolean
        get() = entityData.get(HIBERNATING)
        set(value) {
            entityData.set(HIBERNATING, value)
            refreshDimensions()
        }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(HIBERNATING, true)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, PanicGoal(this, 1.35))
        goalSelector.addGoal(2, object : WaterAvoidingRandomStrollGoal(this, 0.85) {
            override fun canUse(): Boolean = !this@GlaciopodEntity.isHibernating && this@GlaciopodEntity.unfurlProgress >= 0.7f && super.canUse()
            override fun canContinueToUse(): Boolean = !this@GlaciopodEntity.isHibernating && super.canContinueToUse()
        })
        goalSelector.addGoal(3, object : LookAtPlayerGoal(this, Player::class.java, 8.0f) {
            override fun canUse(): Boolean = !this@GlaciopodEntity.isHibernating && super.canUse()
        })
        goalSelector.addGoal(4, object : RandomLookAroundGoal(this) {
            override fun canUse(): Boolean = !this@GlaciopodEntity.isHibernating && super.canUse()
        })
    }

    override fun tick() {
        super.tick()

        prevUnfurlProgress = unfurlProgress

        // Interpolation de la progression d'ouverture sur 20 ticks (0.05f par tick)
        if (isHibernating) {
            if (unfurlProgress > 0.0f) {
                unfurlProgress = (unfurlProgress - 0.05f).coerceAtLeast(0.0f)
            }
        } else {
            if (unfurlProgress < 1.0f) {
                unfurlProgress = (unfurlProgress + 0.05f).coerceAtMost(1.0f)
            }
        }

        // Sauvegarde et mise à jour de la chaîne de rotation serpentine
        for (i in 0 until 16) {
            prevSegmentYaws[i] = currentSegmentYaws[i]
        }

        if (!isHibernating && unfurlProgress > 0.2f) {
            // Le premier segment suit l'orientation du corps
            val diff0 = Mth.wrapDegrees(yBodyRot - currentSegmentYaws[0])
            currentSegmentYaws[0] += diff0 * 0.45f

            // Chaque segment suivant suit le segment précédent avec un temps de retard (effet serpent)
            for (i in 1 until 16) {
                val diff = Mth.wrapDegrees(currentSegmentYaws[i - 1] - currentSegmentYaws[i])
                currentSegmentYaws[i] += diff * 0.22f
            }
        } else {
            for (i in 0 until 16) {
                currentSegmentYaws[i] = yBodyRot
            }
        }
    }

    override fun aiStep() {
        super.aiStep()

        // En hibernation ou en cours de dépliage initial, le mob reste immobile comme une roche
        if (isHibernating || unfurlProgress < 0.3f) {
            deltaMovement = Vec3(0.0, deltaMovement.y.coerceAtMost(0.0), 0.0)
            yBodyRot = yRot
            yHeadRot = yRot
        }
    }

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, amount: Float): Boolean {
        if (isHibernating) {
            // Réveil du cloporte
            isHibernating = false

            // Son de craquement de la carapace de glace/roche
            serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.STONE_BREAK,
                SoundSource.NEUTRAL,
                1.2f,
                0.75f
            )

            // Réduction significative du premier choc grâce à la carapace compacte (70% absorbés)
            val absorbedAmount = (amount * 0.30f).coerceAtLeast(1.0f)
            return super.hurtServer(serverLevel, damageSource, absorbedAmount)
        }

        return super.hurtServer(serverLevel, damageSource, amount)
    }

    override fun hurtClient(source: DamageSource): Boolean {
        if (isHibernating) {
            isHibernating = false
        }
        return super.hurtClient(source)
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        if (HIBERNATING == key) {
            refreshDimensions()
        }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (isHibernating) {
            HIBERNATING_DIMENSIONS
        } else {
            ACTIVE_DIMENSIONS
        }
    }

    /**
     * Calcule la progression d'ouverture interpolée pour le rendu client.
     */
    fun getInterpolatedUnfurlProgress(partialTick: Float): Float {
        return Mth.lerp(partialTick, prevUnfurlProgress, unfurlProgress)
    }

    /**
     * Calcule les angles de lacet serpentins interpolés pour chaque segment.
     */
    fun getInterpolatedSegmentYaws(partialTick: Float, result: FloatArray) {
        val baseBodyYaw = Mth.rotLerp(partialTick, yBodyRotO, yBodyRot)
        for (i in 0 until 16) {
            val yawI = Mth.rotLerp(partialTick, prevSegmentYaws[i], currentSegmentYaws[i])
            result[i] = Mth.wrapDegrees(yawI - baseBodyYaw)
        }
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putBoolean("Hibernating", isHibernating)
        output.putFloat("UnfurlProgress", unfurlProgress)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        isHibernating = input.getBooleanOr("Hibernating", true)
        unfurlProgress = input.getFloatOr("UnfurlProgress", 0.0f)
        prevUnfurlProgress = unfurlProgress
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Les animations sont 100% procédurales et gérées dans GlaciopodModel
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    companion object {
        val HIBERNATING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(GlaciopodEntity::class.java, EntityDataSerializers.BOOLEAN)

        /**
         * Hitbox d'hibernation : Bloc cubique compact 2.0 x 2.0 x 1.0 (2x2 blocs verticaux)
         */
        val HIBERNATING_DIMENSIONS: EntityDimensions =
            EntityDimensions.scalable(2.0f, 2.0f).withEyeHeight(1.0f)

        /**
         * Hitbox active : Corps allongé de cloporte géant (largeur adaptée au corps complet)
         */
        val ACTIVE_DIMENSIONS: EntityDimensions =
            EntityDimensions.scalable(2.2f, 0.9f).withEyeHeight(0.55f)

        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0)
        }
    }
}
