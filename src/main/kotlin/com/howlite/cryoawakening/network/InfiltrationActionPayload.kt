package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload Client -> Serveur pour piloter les actions d'Infiltration Pearl :
 * - ACTION_START_ASCEND (1) : 2ème clic droit -> lance l'ascension vers le plafond
 * - ACTION_CONFIRM_EMERGE (2) : Clic droit en phase PEEKING -> sort du sol et consomme la perle
 * - ACTION_CANCEL_RETURN (3) : Clic gauche en phase PEEKING -> annule et redescend à l'origine
 */
data class InfiltrationActionPayload(
    val action: Int
) : CustomPacketPayload {

    companion object {
        const val ACTION_START_ASCEND: Int = 1
        const val ACTION_CONFIRM_EMERGE: Int = 2
        const val ACTION_CANCEL_RETURN: Int = 3

        val ID: CustomPacketPayload.Type<InfiltrationActionPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("infiltration_action"))

        val CODEC: StreamCodec<ByteBuf, InfiltrationActionPayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeByte(payload.action)
            },
            { buf ->
                InfiltrationActionPayload(buf.readByte().toInt())
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
