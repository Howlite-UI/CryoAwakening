package com.howlite.cryoawakening.item

import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * GaleBoomerangItem
 *
 * Boomerang Tornade inspiré de Zelda: Twilight Princess.
 *
 * Fonctionnalités :
 * - Maintenir Clic Droit : Charge le lancer et active le réticule de ciblage (Lock-on).
 * - Verrouillage multi-cibles : Viser jusqu'à 5 créatures ou items pour les cibler en séquence.
 * - Relâcher : Lance le Boomerang avec une trajectoire courbe vers les cibles marquées.
 * - Stase & Tornade (Hovering) : Tourbillonne en l'air et aspire les items et créatures proches.
 * - Ramassage automatique : Attache et rapporte tous les items au sol dans l'inventaire du joueur.
 */
class GaleBoomerangItem(properties: Properties) : Item(properties) {

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation {
        // Si Zephyr est présent, annule l'animation Bow qui ralentit le joueur pour le garder 100% mobile
        val hasZephyr = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS)?.let { enchantments ->
            enchantments.entrySet().any { it.key.isBound && it.key.unwrapKey().get() == com.howlite.cryoawakening.enchantment.ModEnchantments.ZEPHYR }
        } ?: false

        return if (hasZephyr) ItemUseAnimation.NONE else ItemUseAnimation.BOW
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int): Boolean {
        // Le déclenchement principal avec la liste des cibles marquées est géré via ThrowBoomerangPayload côté client
        return true
    }

    @Suppress("DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        builder.accept(Component.literal("§b🌀 Boomerang (Zelda: TP)"))
        builder.accept(Component.literal("§8▪ §7Maintenir Clic Droit : §fCharger & Verrouiller jusqu'à 5 cibles"))
        builder.accept(Component.literal("§8▪ §7Relâcher : §fAttaquer les cibles en séquence"))
        builder.accept(Component.literal("§8▪ §7Ciblage d'item : §aRapporte les items verrouillés au joueur"))
        super.appendHoverText(stack, context, display, builder, tooltipFlag)
    }
}
