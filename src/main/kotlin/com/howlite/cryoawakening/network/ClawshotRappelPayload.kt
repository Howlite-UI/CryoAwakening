package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload réseau envoyé par le client au serveur pour faire descendre (lâcher du mou)
 * ou faire remonter le joueur le long de la chaîne du Clawshot lorsqu'il est suspendu à un mur ou plafond.
 *
 * @param anchorId Id de l'entité ancre accrochée
 * @param direction -1 = descendre (lâcher du mou), +1 = remonter, 0 = repos
 */
data class ClawshotRappelPayload(
    val anchorId: Int,
    val direction: Int
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<ClawshotRappelPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("clawshot_rappel"))

        val CODEC: StreamCodec<ByteBuf, ClawshotRappelPayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeInt(payload.anchorId)
                buf.writeInt(payload.direction)
            },
            { buf ->
                val anchorId = buf.readInt()
                val direction = buf.readInt()
                ClawshotRappelPayload(anchorId, direction)
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
