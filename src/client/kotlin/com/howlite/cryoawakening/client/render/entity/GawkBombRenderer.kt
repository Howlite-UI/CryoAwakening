package com.howlite.cryoawakening.client.render.entity

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.entity.GawkBombEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState

/**
 * GawkBombRenderer
 *
 * Renderer GeckoLib pour l'entité 3D GawkBombEntity (modèle gawker avec dead_gawker.png).
 */
class GawkBombRenderer(
    context: EntityRendererProvider.Context
) : GeoEntityRenderer<GawkBombEntity, LivingEntityRenderState>(context, GawkBombModel()) {

    companion object {
        val IS_MINE: DataTicket<Boolean> =
            DataTicket.create("gawk_bomb_is_mine", Boolean::class.javaObjectType)

        val POWDER_CHARGE: DataTicket<Int> =
            DataTicket.create("gawk_bomb_powder_charge", Int::class.javaObjectType)

        val IS_ARMED: DataTicket<Boolean> =
            DataTicket.create("gawk_bomb_is_armed", Boolean::class.javaObjectType)

        val FUSE_TICKS: DataTicket<Int> =
            DataTicket.create("gawk_bomb_fuse_ticks", Int::class.javaObjectType)

        val FLIGHT_TICKS: DataTicket<Int> =
            DataTicket.create("gawk_bomb_flight_ticks", Int::class.javaObjectType)
    }

    override fun addRenderData(
        animatable: GawkBombEntity,
        extraData: Void?,
        state: LivingEntityRenderState,
        partialTick: Float
    ) {
        super.addRenderData(animatable, extraData, state, partialTick)

        state.addGeckolibData(IS_MINE, animatable.isMine)
        state.addGeckolibData(POWDER_CHARGE, animatable.powderCharge)
        state.addGeckolibData(IS_ARMED, animatable.isArmed)
        state.addGeckolibData(FUSE_TICKS, animatable.fuseTicks)
        state.addGeckolibData(FLIGHT_TICKS, animatable.flightTicks)
    }

    override fun adjustModelBonesForRender(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots)
        (model as GawkBombModel).setCustomAnimations(renderPassInfo, boneSnapshots)
    }
}
