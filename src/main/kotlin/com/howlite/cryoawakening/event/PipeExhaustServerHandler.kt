package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.block.entity.GalePipeExhaustBlockEntity
import com.howlite.cryoawakening.network.SetPipeExhaustSpeedPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/**
 * Gère l'enregistrement et la réception serveur du paquet SetPipeExhaustSpeedPayload
 * pour modifier la vitesse d'échappement du Gale Pipe Exhaust.
 */
object PipeExhaustServerHandler {

    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(
            SetPipeExhaustSpeedPayload.ID,
            SetPipeExhaustSpeedPayload.CODEC
        )

        ServerPlayNetworking.registerGlobalReceiver(SetPipeExhaustSpeedPayload.ID) { payload, context ->
            context.server().execute {
                val player = context.player()
                val level = player.level()
                val pos = payload.pos

                // Vérification de distance de portée d'interaction (max 8 blocs)
                if (player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0) {
                    val be = level.getBlockEntity(pos)
                    if (be is GalePipeExhaustBlockEntity) {
                        be.setSpeed(payload.outputRate)
                    }
                }
            }
        }
    }
}
