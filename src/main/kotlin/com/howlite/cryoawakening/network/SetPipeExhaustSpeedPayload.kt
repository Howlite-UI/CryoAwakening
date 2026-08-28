package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload réseau envoyé par le client au serveur pour ajuster la vitesse d'échappement du vent d'un Gale Pipe Exhaust.
 */
data class SetPipeExhaustSpeedPayload(
    val pos: BlockPos,
    val outputRate: Int
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<SetPipeExhaustSpeedPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("set_pipe_exhaust_speed"))

        val CODEC: StreamCodec<ByteBuf, SetPipeExhaustSpeedPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetPipeExhaustSpeedPayload::pos,
            ByteBufCodecs.VAR_INT,
            SetPipeExhaustSpeedPayload::outputRate,
            ::SetPipeExhaustSpeedPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
