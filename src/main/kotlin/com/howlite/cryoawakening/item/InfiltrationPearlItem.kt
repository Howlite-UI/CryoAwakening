package com.howlite.cryoawakening.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * InfiltrationPearlItem
 *
 * Perle consommable à usage unique inspirée du pouvoir "Infiltration" (Ascend) de Zelda: Tears of the Kingdom.
 * - 1er Clic Droit : active la visée du plafond avec effet de ciblage 3D et animation caméra.
 * - 2ème Clic Droit : propulse le joueur à travers le plafond (vortex immersif) jusqu'au ras de la surface.
 * - À la surface : Clic Droit pour émerger (consomme la perle) ou Clic Gauche pour annuler et redescendre.
 */
class InfiltrationPearlItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        // L'interaction est gérée précisément via le système client/serveur d'infiltration
        // afin d'éviter les doubles activations et de supporter le cycle visée -> lancement -> émergence.
        return InteractionResult.PASS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        builder.accept(CommonComponents.EMPTY)
        builder.accept(
            Component.translatable("item.cryo-awakening.infiltration_pearl.desc")
                .withStyle(ChatFormatting.GRAY)
        )
        builder.accept(
            Component.translatable("item.cryo-awakening.infiltration_pearl.usage")
                .withStyle(ChatFormatting.AQUA)
        )
        builder.accept(
            Component.translatable("item.cryo-awakening.infiltration_pearl.single_use")
                .withStyle(ChatFormatting.DARK_GREEN)
        )
        super.appendHoverText(stack, context, display, builder, tooltipFlag)
    }
}
