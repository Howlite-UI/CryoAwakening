package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.entity.GaleBoomerangEntity
import com.howlite.cryoawakening.entity.ModEntities
import com.howlite.cryoawakening.item.GaleBoomerangItem
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.network.ThrowBoomerangPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand

/**
 * BoomerangServerHandler
 *
 * Gère l'enregistrement du payload réseau C2S et le traitement côté serveur
 * du lancer du Gale Boomerang (création de l'entité avec ses cibles verrouillées).
 */
object BoomerangServerHandler {

    fun register() {
        // Enregistrement du type de payload C2S
        PayloadTypeRegistry.serverboundPlay().register(
            ThrowBoomerangPayload.ID,
            ThrowBoomerangPayload.CODEC
        )

        // Réception du paquet de lancer de Boomerang
        ServerPlayNetworking.registerGlobalReceiver(ThrowBoomerangPayload.ID) { payload, context ->
            val player = context.player()

            context.server().execute {
                val level = player.level() as? ServerLevel ?: return@execute
                // Trouver la main qui tient le boomerang
                val hand = when {
                    player.mainHandItem.`is`(ModItems.GALE_BOOMERANG) -> InteractionHand.MAIN_HAND
                    player.offhandItem.`is`(ModItems.GALE_BOOMERANG) -> InteractionHand.OFF_HAND
                    else -> null
                } ?: return@execute

                val itemStack = player.getItemInHand(hand)

                val boomerang = GaleBoomerangEntity(ModEntities.GALE_BOOMERANG, level)
                boomerang.setPos(player.x, player.eyeY - 0.1, player.z)
                boomerang.setupThrow(player, payload.force, payload.targetEntityIds, hand, itemStack)
                level.addFreshEntity(boomerang)

                // Usure de durabilité
                if (!player.abilities.instabuild) {
                    itemStack.hurtAndBreak(1, level, player) {
                        player.onEquippedItemBroken(itemStack.item, if (hand == InteractionHand.MAIN_HAND) net.minecraft.world.entity.EquipmentSlot.MAINHAND else net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                    }
                }

                // Cooldown court pour éviter le spam pendant le vol
                player.cooldowns.addCooldown(itemStack, 15)
            }
        }
    }
}
