package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.entity.GawkerEntity
import com.howlite.cryoawakening.network.ThrowGawkerPayload
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player

/**
 * GawkerCarryHandler
 *
 * Gère les interactions et le blocage de la barre d'item / inventaire
 * lorsque le joueur porte un Gawker au-dessus de sa tête, ainsi que la réception
 * du paquet de lancer.
 */
object GawkerCarryHandler {

    @JvmStatic
    fun getCarriedGawker(player: Player): GawkerEntity? {
        return player.level().getEntitiesOfClass(
            GawkerEntity::class.java,
            player.boundingBox.inflate(3.0)
        ) { it.isCarried && it.carrierId == player.id }.firstOrNull()
    }

    fun register() {
        // Enregistrement du type de payload C2S
        PayloadTypeRegistry.serverboundPlay().register(
            ThrowGawkerPayload.ID,
            ThrowGawkerPayload.CODEC
        )

        // Réception du paquet de lancer de Gawker
        ServerPlayNetworking.registerGlobalReceiver(ThrowGawkerPayload.ID) { payload, context ->
            val player = context.player()
            val gawker = getCarriedGawker(player)

            if (gawker != null) {
                context.server().execute {
                    gawker.launch(player, payload.force)
                }
            }
        }

        // Blocage de l'utilisation des items en main pendant le portage du Gawker
        UseItemCallback.EVENT.register { player, _, _ ->
            if (getCarriedGawker(player) != null) {
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }

        // Blocage de l'utilisation des blocs pendant le portage du Gawker
        UseBlockCallback.EVENT.register { player, _, _, _ ->
            if (getCarriedGawker(player) != null) {
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }

        // Blocage du minage / attaque de bloc pendant le portage
        AttackBlockCallback.EVENT.register { player, _, _, _, _ ->
            if (getCarriedGawker(player) != null) {
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }

        // Blocage de l'attaque d'entité avec des armes pendant le portage
        AttackEntityCallback.EVENT.register { player, _, _, _, _ ->
            if (getCarriedGawker(player) != null) {
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }
    }
}
