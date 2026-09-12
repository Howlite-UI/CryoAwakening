package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.howlite.cryoawakening.network.ClawshotRappelPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/**
 * ClawshotServerHandler
 *
 * Enregistre le payload et le récepteur serveur pour le rappel/mou de chaîne du Clawshot.
 */
object ClawshotServerHandler {

    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(
            ClawshotRappelPayload.ID,
            ClawshotRappelPayload.CODEC
        )

        ServerPlayNetworking.registerGlobalReceiver(ClawshotRappelPayload.ID) { payload, context ->
            val player = context.player()
            val level = player.level()
            val entity = level.getEntity(payload.anchorId)
            if (entity is ClawshotAnchorEntity && entity.getOwnerEntity() == player) {
                context.server().execute {
                    entity.rappelDirection = payload.direction.coerceIn(-1, 1)
                }
            }
        }
    }
}
