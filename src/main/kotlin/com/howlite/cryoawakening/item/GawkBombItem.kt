package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.entity.GawkBombEntity
import com.howlite.cryoawakening.entity.ModEntities
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * GawkBombItem
 *
 * Bombe cryogénique artisanale fabriquée à base de fourrure de Gawker.
 * Stackable jusqu'à 16 avec modèle 3D Blockbench dédié (gawker_bomb.json).
 *
 * Utilisation :
 * - Clic Droit maintenu : Charge la puissance de projection et lance la bombe en arc balistique.
 * - Shift + Clic Droit sur un bloc : Pose la Gawk-Bomb au sol comme une mine terrestre de proximité.
 * - Table de craft : Peut être combinée avec de la Poudre à Canon (Gunpowder) pour augmenter son niveau de charge (0 à 3).
 */
class GawkBombItem(properties: Properties) : Item(properties) {

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation = ItemUseAnimation.BOW

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        val level = context.level
        val clickedPos = context.clickedPos
        val clickedFace = context.clickedFace

        // Shift + Clic Droit : Poser la Gawk-Bomb en mine de proximité
        if (player != null && player.isShiftKeyDown) {
            val placeContext = BlockPlaceContext(context)
            val placePos = if (level.getBlockState(clickedPos).canBeReplaced(placeContext)) clickedPos else clickedPos.relative(clickedFace)

            if (!level.isClientSide && level is ServerLevel) {
                val charge = getCharge(context.itemInHand)
                val bombEntity = GawkBombEntity(ModEntities.GAWK_BOMB, level)
                bombEntity.setupMine(
                    placePos.x + 0.5,
                    placePos.y.toDouble(),
                    placePos.z + 0.5,
                    charge,
                    player
                )
                level.addFreshEntity(bombEntity)

                if (!player.abilities.instabuild) {
                    context.itemInHand.shrink(1)
                }
            }
            return InteractionResult.SUCCESS
        }

        return super.useOn(context)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int): Boolean {
        if (entity is Player) {
            val useTicks = getUseDuration(stack, entity) - timeLeft
            val chargeProgress = (useTicks.toFloat() / 25.0f).coerceIn(0.0f, 1.0f)
            val force = 0.8f + (chargeProgress * 1.4f)
            val charge = getCharge(stack)

            if (!level.isClientSide && level is ServerLevel) {
                val bombEntity = GawkBombEntity(ModEntities.GAWK_BOMB, level)
                bombEntity.setPos(entity.x, entity.eyeY - 0.1, entity.z)
                bombEntity.setupThrow(entity, force, charge)
                level.addFreshEntity(bombEntity)

                if (!entity.abilities.instabuild) {
                    stack.shrink(1)
                }
            }
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val charge = getCharge(stack)
        val stars = when (charge) {
            1 -> "§e★☆☆ §7(1/3)"
            2 -> "§6★★☆ §7(2/3)"
            3 -> "§c★★★ §7(3/3 - Max)"
            else -> "§7☆☆☆ (0/3)"
        }

        builder.accept(Component.literal("§b❄ Charge : $stars"))
        builder.accept(Component.literal("§8▪ §7Maintenir Clic Droit : §fLancer"))
        builder.accept(Component.literal("§8▪ §7Shift + Clic Droit : §fPoser en Mine"))
        super.appendHoverText(stack, context, display, builder, tooltipFlag)
    }

    companion object {
        fun getCharge(stack: ItemStack): Int {
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return 0
            return customData.copyTag().getIntOr("PowderCharge", 0)
        }

        fun setCharge(stack: ItemStack, charge: Int) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                tag.putInt("PowderCharge", charge.coerceIn(0, 3))
            }
        }

        fun createWithCharge(charge: Int): ItemStack {
            val stack = ItemStack(ModItems.GAWK_BOMB)
            setCharge(stack, charge)
            return stack
        }
    }
}
