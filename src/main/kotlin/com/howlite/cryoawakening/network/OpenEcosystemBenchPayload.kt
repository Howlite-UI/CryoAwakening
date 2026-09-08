package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload réseau envoyé par le serveur au client pour ouvrir l'interface de l'Ecosystem Bench (Diagramme de Grotte).
 */
data class OpenEcosystemBenchPayload(
    val pos: BlockPos
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<OpenEcosystemBenchPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("open_ecosystem_bench"))

        val CODEC: StreamCodec<ByteBuf, OpenEcosystemBenchPayload> = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpenEcosystemBenchPayload::pos,
            ::OpenEcosystemBenchPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
