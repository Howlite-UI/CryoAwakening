package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload réseau envoyé par le client au serveur pour déclencher le lancer du Gale Boomerang
 * avec la force calculée et la liste des entités ciblées (lock-on).
 */
data class ThrowBoomerangPayload(
    val force: Float,
    val targetEntityIds: List<Int>
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<ThrowBoomerangPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("throw_boomerang"))

        val CODEC: StreamCodec<ByteBuf, ThrowBoomerangPayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeFloat(payload.force)
                buf.writeInt(payload.targetEntityIds.size)
                for (id in payload.targetEntityIds) {
                    buf.writeInt(id)
                }
            },
            { buf ->
                val force = buf.readFloat()
                val count = buf.readInt().coerceIn(0, 64)
                val targets = ArrayList<Int>(count)
                for (i in 0 until count) {
                    targets.add(buf.readInt())
                }
                ThrowBoomerangPayload(force, targets)
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
