package com.howlite.cryoawakening.item

import net.minecraft.sounds.SoundEvents
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.Level

/**
 * KaleidoscopeItem
 *
 * Item ludique qui s'utilise comme une longue-vue (Spyglass),
 * mais qui déforme la vision en facettes géométriques kaléidoscopiques et vagues optiques
 * au lieu de zoomer au loin.
 */
class KaleidoscopeItem(properties: Properties) : Item(properties) {

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation = ItemUseAnimation.SPYGLASS

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        player.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.3f)
        player.awardStat(Stats.ITEM_USED.get(this))
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        stopUsing(entity)
        return stack
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int): Boolean {
        stopUsing(entity)
        return true
    }

    private fun stopUsing(entity: LivingEntity) {
        entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 1.3f)
    }
}
