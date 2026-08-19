package com.howlite.cryoawakening.client.render.entity

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.entity.GawkerEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Mth

/**
 * GawkerRenderer
 *
 * Renderer GeckoLib pour GawkerEntity.
 * Transmet les DataTickets d'orientation pour les yeux d'escargot,
 * l'état de dégât (hurtTime) et l'animation d'attaque (morsure).
 */
class GawkerRenderer(
    context: EntityRendererProvider.Context
) : GeoEntityRenderer<GawkerEntity, LivingEntityRenderState>(context, GawkerModel()) {

    companion object {
        val NET_HEAD_YAW: DataTicket<Float> =
            DataTicket.create("gawker_net_head_yaw", Float::class.javaObjectType)

        val HEAD_PITCH: DataTicket<Float> =
            DataTicket.create("gawker_head_pitch", Float::class.javaObjectType)

        val HURT_PROGRESS: DataTicket<Float> =
            DataTicket.create("gawker_hurt_progress", Float::class.javaObjectType)

        val ATTACK_PROGRESS: DataTicket<Float> =
            DataTicket.create("gawker_attack_progress", Float::class.javaObjectType)
    }

    override fun addRenderData(
        animatable: GawkerEntity,
        extraData: Void?,
        state: LivingEntityRenderState,
        partialTick: Float
    ) {
        super.addRenderData(animatable, extraData, state, partialTick)

        // Calcul de l'orientation de la tête/yeux vers la cible
        val bodyYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot)
        val headYaw = Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot)
        val netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw)
        val headPitch = Mth.rotLerp(partialTick, animatable.xRotO, animatable.xRot)

        // Progression de dégâts et d'attaque pour l'ouverture de la bouche
        val hurtProgress = if (animatable.hurtTime > 0) animatable.hurtTime.toFloat() / 10.0f else 0.0f
        val attackProgress = animatable.getAttackAnim(partialTick)

        state.addGeckolibData(NET_HEAD_YAW, netHeadYaw)
        state.addGeckolibData(HEAD_PITCH, headPitch)
        state.addGeckolibData(HURT_PROGRESS, hurtProgress)
        state.addGeckolibData(ATTACK_PROGRESS, attackProgress)
    }

    override fun adjustModelBonesForRender(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots)

        // Délégation des animations procédurales (yeux, pattes, bouche, fourrure)
        (model as GawkerModel).setCustomAnimations(renderPassInfo, boneSnapshots)
    }
}
