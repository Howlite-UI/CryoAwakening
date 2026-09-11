package com.howlite.cryoawakening.client.model.property

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.mojang.serialization.MapCodec
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.HitResult

/**
 * ClawshotAimingProperty
 *
 * Détecte si le joueur tenant le Clawshot vise une surface solide ou une entité
 * située dans la portée maximale du grappin (28 blocs).
 * Si vrai, les 3 griffes du modèle s'écartent vers l'extérieur (clawshot_aiming.json).
 */
class ClawshotAimingProperty : ConditionalItemModelProperty {

    companion object {
        val MAP_CODEC: MapCodec<ClawshotAimingProperty> = MapCodec.unit(ClawshotAimingProperty())
    }

    override fun get(
        stack: ItemStack,
        level: ClientLevel?,
        entity: LivingEntity?,
        seed: Int,
        context: ItemDisplayContext
    ): Boolean {
        if (entity !is Player || level == null) return false

        val isHand = context.firstPerson() ||
                context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND

        if (!isHand) return false

        val hit = ProjectileUtil.getHitResultOnViewVector(
            entity,
            { target -> target.isAlive && target != entity && !target.isSpectator },
            ClawshotAnchorEntity.MAX_RANGE
        )

        return hit.type != HitResult.Type.MISS
    }

    override fun type(): MapCodec<out ConditionalItemModelProperty> = MAP_CODEC
}
