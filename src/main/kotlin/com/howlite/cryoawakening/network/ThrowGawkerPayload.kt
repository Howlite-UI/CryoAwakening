package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload réseau envoyé par le client au serveur pour déclencher le lancer du Gawker porté.
 * @param force Force du lancer calculée à partir du temps de charge du clic droit.
 */
data class ThrowGawkerPayload(val force: Float) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<ThrowGawkerPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("throw_gawker"))

        val CODEC: StreamCodec<ByteBuf, ThrowGawkerPayload> =
            ByteBufCodecs.FLOAT.map(::ThrowGawkerPayload, ThrowGawkerPayload::force)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
