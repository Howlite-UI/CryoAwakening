package com.howlite.cryoawakening.client.model.property

import com.howlite.cryoawakening.item.ClawshotItem
import com.mojang.serialization.MapCodec
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/**
 * ClawshotFiredProperty
 *
 * Détermine si le Clawshot tenu en main a lancé sa tête (anchor_core) dans le monde.
 * Si vrai, le modèle affiche uniquement le gantelet (clawshot_fired.json).
 */
class ClawshotFiredProperty : ConditionalItemModelProperty {

    companion object {
        val MAP_CODEC: MapCodec<ClawshotFiredProperty> = MapCodec.unit(ClawshotFiredProperty())
    }

    override fun get(
        stack: ItemStack,
        level: ClientLevel?,
        entity: LivingEntity?,
        seed: Int,
        context: ItemDisplayContext
    ): Boolean {
        if (entity !is Player || level == null) return false

        val isRightHand = (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
        val isLeftHand = (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)

        if (!isRightHand && !isLeftHand) {
            return false
        }

        val anchor = ClawshotItem.findActiveAnchor(level, entity) ?: return false
        if (!anchor.isAlive) return false

        val anchorHand = anchor.usedHand
        val holdingHand = if (isRightHand) {
            if (entity.mainArm == HumanoidArm.RIGHT) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
        } else {
            if (entity.mainArm == HumanoidArm.RIGHT) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        }

        return anchorHand == holdingHand
    }

    override fun type(): MapCodec<out ConditionalItemModelProperty> = MAP_CODEC
}
