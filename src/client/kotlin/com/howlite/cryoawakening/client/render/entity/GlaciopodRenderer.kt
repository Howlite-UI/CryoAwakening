package com.howlite.cryoawakening.client.render.entity

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.entity.GlaciopodEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Mth

/**
 * GlaciopodRenderer
 *
 * Renderer GeckoLib pour GlaciopodEntity.
 * Transmet les DataTickets d'animation procédurale (dépliage, regard de tête, virage serpentin).
 */
class GlaciopodRenderer(
    context: EntityRendererProvider.Context
) : GeoEntityRenderer<GlaciopodEntity, LivingEntityRenderState>(context, GlaciopodModel()) {

    companion object {
        val UNFURL_PROGRESS: DataTicket<Float> =
            DataTicket.create("unfurl_progress", Float::class.javaObjectType)

        val IS_HIBERNATING: DataTicket<Boolean> =
            DataTicket.create("is_hibernating", Boolean::class.javaObjectType)

        val NET_HEAD_YAW: DataTicket<Float> =
            DataTicket.create("net_head_yaw", Float::class.javaObjectType)

        val HEAD_PITCH: DataTicket<Float> =
            DataTicket.create("head_pitch", Float::class.javaObjectType)

        val SEGMENT_YAWS: DataTicket<FloatArray> =
            DataTicket.create("segment_yaws", FloatArray::class.java)
    }

    private val tempSegmentYaws = FloatArray(16)

    override fun addRenderData(
        animatable: GlaciopodEntity,
        extraData: Void?,
        state: LivingEntityRenderState,
        partialTick: Float
    ) {
        super.addRenderData(animatable, extraData, state, partialTick)

        val unfurl = animatable.getInterpolatedUnfurlProgress(partialTick)
        state.addGeckolibData(UNFURL_PROGRESS, unfurl)
        state.addGeckolibData(IS_HIBERNATING, animatable.isHibernating)

        // Calcul de la rotation indépendante de la tête vers le joueur (Yaw et Pitch)
        val bodyYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot)
        val headYaw = Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot)
        val netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw)
        val headPitch = Mth.rotLerp(partialTick, animatable.xRotO, animatable.xRot)

        state.addGeckolibData(NET_HEAD_YAW, netHeadYaw)
        state.addGeckolibData(HEAD_PITCH, headPitch)

        // Courbure serpentine des segments lors des virages
        animatable.getInterpolatedSegmentYaws(partialTick, tempSegmentYaws)
        state.addGeckolibData(SEGMENT_YAWS, tempSegmentYaws.clone())
    }

    override fun adjustModelBonesForRender(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots)

        // Délégation des animations procédurales (dépliage + regard de tête + virage serpentin)
        (model as GlaciopodModel).setCustomAnimations(renderPassInfo, boneSnapshots)
    }
}
