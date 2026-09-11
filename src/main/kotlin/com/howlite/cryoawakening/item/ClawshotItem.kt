package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.howlite.cryoawakening.entity.ModEntities
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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
import net.minecraft.world.phys.AABB
import java.util.function.Consumer

/**
 * ClawshotItem
 *
 * Grappin mécanique avec gantelet, chaîne rétractable et tête à 3 griffes,
 * directement inspiré du Clawshot de Zelda: Twilight Princess.
 *
 * Fonctionnalités :
 * - Clic droit : Lance la tête (anchor_core) à grande vitesse pendant que le gantelet reste en main.
 * - Réutilisation / Rappel : Clic droit à nouveau pour détacher ou rétracter l'ancre.
 * - Accrochage : Se plante sur les blocs solides / grilles et propulse le joueur vers la cible.
 * - Mobs : Attire les créatures légères vers le joueur / tracte le joueur vers les créatures lourdes.
 * - Sécurité anti-chute : Annule strictement tous les dégâts de chute pendant la traction.
 */
class ClawshotItem(properties: Properties) : Item(properties) {

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation = ItemUseAnimation.NONE

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        // Recherche si le joueur a déjà une ancre active dans le monde
        val existingAnchor = findActiveAnchor(level, player)

        if (existingAnchor != null && existingAnchor.isAlive) {
            // Un second clic droit rappelle l'ancre côté serveur
            if (!level.isClientSide) {
                existingAnchor.retract()
            }
            return InteractionResult.SUCCESS
        }

        // Lancer d'une nouvelle ancre
        if (!level.isClientSide) {
            val anchor = ClawshotAnchorEntity(ModEntities.CLAWSHOT_ANCHOR, level)
            anchor.setupLaunch(player, hand)
            level.addFreshEntity(anchor)

            // Sons mécaniques de tir et de déroulement de chaîne
            level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                0.8f,
                1.2f
            )
            level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS,
                1.0f,
                1.4f
            )
        }

        player.swing(hand)
        return InteractionResult.SUCCESS
    }

    companion object {
        fun findActiveAnchor(level: Level, player: Player): ClawshotAnchorEntity? {
            val searchBox = AABB(
                player.x - ClawshotAnchorEntity.MAX_RANGE - 4.0,
                player.y - ClawshotAnchorEntity.MAX_RANGE - 4.0,
                player.z - ClawshotAnchorEntity.MAX_RANGE - 4.0,
                player.x + ClawshotAnchorEntity.MAX_RANGE + 4.0,
                player.y + ClawshotAnchorEntity.MAX_RANGE + 4.0,
                player.z + ClawshotAnchorEntity.MAX_RANGE + 4.0
            )
            return level.getEntitiesOfClass(ClawshotAnchorEntity::class.java, searchBox) { anchor ->
                (anchor.ownerUuid == player.uuid || anchor.ownerEntityId == player.id) && anchor.isAlive
            }.firstOrNull()
        }
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
            Component.translatable("item.cryo-awakening.clawshot.desc")
                .withStyle(ChatFormatting.GRAY)
        )
        builder.accept(
            Component.translatable("item.cryo-awakening.clawshot.hook")
                .withStyle(ChatFormatting.AQUA)
        )
        builder.accept(
            Component.translatable("item.cryo-awakening.clawshot.safety")
                .withStyle(ChatFormatting.DARK_GREEN)
        )
        super.appendHoverText(stack, context, display, builder, tooltipFlag)
    }
}
