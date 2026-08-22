package com.howlite.cryoawakening.client.render.entity

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.entity.GaleBoomerangEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState

/**
 * GaleBoomerangRenderer
 *
 * Renderer GeckoLib 5 pour GaleBoomerangEntity.
 */
class GaleBoomerangRenderer(
    context: EntityRendererProvider.Context
) : GeoEntityRenderer<GaleBoomerangEntity, LivingEntityRenderState>(context, GaleBoomerangModel()) {

    companion object {
        val BOOMERANG_STATE: DataTicket<Int> =
            DataTicket.create("gale_boomerang_state", Int::class.javaObjectType)

        val FLIGHT_TICKS: DataTicket<Int> =
            DataTicket.create("gale_boomerang_flight_ticks", Int::class.javaObjectType)
    }

    override fun addRenderData(
        animatable: GaleBoomerangEntity,
        extraData: Void?,
        state: LivingEntityRenderState,
        partialTick: Float
    ) {
        super.addRenderData(animatable, extraData, state, partialTick)
        state.addGeckolibData(BOOMERANG_STATE, animatable.boomerangState.id)
        state.addGeckolibData(FLIGHT_TICKS, animatable.flightTicks)
    }

    override fun adjustModelBonesForRender(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots)
        (model as GaleBoomerangModel).setCustomAnimations(renderPassInfo, boneSnapshots)
    }
}
