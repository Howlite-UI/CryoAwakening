package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.howlite.cryoawakening.entity.ModEntities
import com.geckolib.animatable.GeoItem
import com.geckolib.animatable.client.GeoRenderProvider
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
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
class ClawshotItem(properties: Properties) : Item(properties), GeoItem {

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Géré procéduralement dans ClawshotItemRenderer
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        RENDER_PROVIDER?.let { consumer.accept(it) }
    }

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation = ItemUseAnimation.NONE

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        // Recherche si CETTE main a déjà une ancre active dans le monde
        val thisAnchor = findActiveAnchor(level, player, hand)

        if (thisAnchor != null && thisAnchor.isAlive) {
            val otherHand = if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
            val otherStack = player.getItemInHand(otherHand)

            // Si c'est la main principale et que la main secondaire possède AUSSI un Clawshot disponible (non déployé),
            // on passe (PASS) pour que Minecraft permette à la main secondaire de tirer son grappin !
            if (hand == InteractionHand.MAIN_HAND && otherStack.item is ClawshotItem) {
                val otherAnchor = findActiveAnchor(level, player, otherHand)
                if (otherAnchor == null || !otherAnchor.isAlive) {
                    return InteractionResult.PASS
                }
            }

            // Un second clic droit avec la même main rappelle l'ancre et détache le joueur (avec élan Slingshot si applicable)
            if (!level.isClientSide) {
                thisAnchor.handleManualRelease(player)
            }
            return InteractionResult.SUCCESS
        }

        // Lancer d'une nouvelle ancre depuis cette main
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
        var RENDER_PROVIDER: GeoRenderProvider? = null

        fun findActiveAnchor(level: Level, player: Player, hand: InteractionHand? = null): ClawshotAnchorEntity? {
            val searchBox = AABB(
                player.x - 64.0,
                player.y - 64.0,
                player.z - 64.0,
                player.x + 64.0,
                player.y + 64.0,
                player.z + 64.0
            )
            return level.getEntitiesOfClass(ClawshotAnchorEntity::class.java, searchBox) { anchor ->
                (anchor.ownerUuid == player.uuid || anchor.ownerEntityId == player.id) &&
                anchor.isAlive &&
                (hand == null || anchor.usedHand == hand)
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
