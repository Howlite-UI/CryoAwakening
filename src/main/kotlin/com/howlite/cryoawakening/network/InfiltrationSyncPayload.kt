package com.howlite.cryoawakening.network

import com.howlite.cryoawakening.CryoAwakening
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Payload Serveur -> Client synchronisant l'état autoritaire d'une séquence d'infiltration.
 * Garantit une synchronisation parfaite entre la physique serveur, la caméra, le shader vortex et le rendu 3D.
 */
data class InfiltrationSyncPayload(
    val phaseId: Int,
    val originX: Double,
    val originY: Double,
    val originZ: Double,
    val targetX: Double,
    val targetZ: Double,
    val ceilingHitY: Double,
    val exitFloorY: Double,
    val phaseTick: Int,
    val phaseDuration: Int
) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<InfiltrationSyncPayload> =
            CustomPacketPayload.Type(CryoAwakening.id("infiltration_sync"))

        val CODEC: StreamCodec<ByteBuf, InfiltrationSyncPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeByte(p.phaseId)
                buf.writeDouble(p.originX)
                buf.writeDouble(p.originY)
                buf.writeDouble(p.originZ)
                buf.writeDouble(p.targetX)
                buf.writeDouble(p.targetZ)
                buf.writeDouble(p.ceilingHitY)
                buf.writeDouble(p.exitFloorY)
                buf.writeInt(p.phaseTick)
                buf.writeInt(p.phaseDuration)
            },
            { buf ->
                InfiltrationSyncPayload(
                    phaseId = buf.readByte().toInt(),
                    originX = buf.readDouble(),
                    originY = buf.readDouble(),
                    originZ = buf.readDouble(),
                    targetX = buf.readDouble(),
                    targetZ = buf.readDouble(),
                    ceilingHitY = buf.readDouble(),
                    exitFloorY = buf.readDouble(),
                    phaseTick = buf.readInt(),
                    phaseDuration = buf.readInt()
                )
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}
