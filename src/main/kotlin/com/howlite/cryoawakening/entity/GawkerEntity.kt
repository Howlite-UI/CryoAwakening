package com.howlite.cryoawakening.entity

import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import com.howlite.cryoawakening.item.ModItems
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
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
import net.minecraft.world.level.Level

/**
 * GawkerEntity
 *
 * Petite créature cubique curieuse composée d'une grande bouche articulée,
 * de 4 petites pattes trottinantes et de 2 yeux sur tige type escargot.
 * - Ses yeux s'orientent vers les cibles/joueurs indépendamment de son corps.
 * - Ses 4 pattes s'animent en trottinant.
 * - Sa bouche s'ouvre grand lorsqu'il attaque ou subit un coup.
 */
class GawkerEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, MeleeAttackGoal(this, 1.25, false))
        goalSelector.addGoal(2, TemptGoal(this, 1.15, { isTemptingItem(it) }, false))
        goalSelector.addGoal(3, WaterAvoidingRandomStrollGoal(this, 0.9))
        goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 12.0f))
        goalSelector.addGoal(5, RandomLookAroundGoal(this))

        // Riposte s'il est attaqué
        targetSelector.addGoal(1, HurtByTargetGoal(this))
    }

    private fun isTemptingItem(stack: ItemStack): Boolean {
        return stack.`is`(ModItems.RAW_BISMUTH)
    }

    override fun getAmbientSound(): SoundEvent? {
        return SoundEvents.RABBIT_AMBIENT
    }

    override fun getHurtSound(damageSource: DamageSource): SoundEvent {
        return SoundEvents.RABBIT_HURT
    }

    override fun getDeathSound(): SoundEvent {
        return SoundEvents.RABBIT_DEATH
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Animations gérées de manière procédurale dans GawkerModel
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.STEP_HEIGHT, 0.6)
                .add(Attributes.TEMPT_RANGE, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
        }
    }
}
