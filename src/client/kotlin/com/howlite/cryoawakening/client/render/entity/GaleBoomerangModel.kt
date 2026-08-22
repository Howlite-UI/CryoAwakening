package com.howlite.cryoawakening.client.render.entity

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.GaleBoomerangEntity
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier

/**
 * GaleBoomerangModel
 *
 * Modèle GeckoLib 5 pour le Gale Boomerang 3D.
 * Gère la rotation continue à haute vitesse sur lui-même et l'inclinaison aérodynamique en vol.
 */
class GaleBoomerangModel : GeoModel<GaleBoomerangEntity>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("gale_boomerang")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/entity/gale_boomerang.png")

    override fun getAnimationResource(animatable: GaleBoomerangEntity): Identifier =
        CryoAwakening.id("gale_boomerang")

    fun setCustomAnimations(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        val state = renderPassInfo.renderState()
        val age = state.ageInTicks

        val boomerangStateId = renderPassInfo.getOrDefaultGeckolibData(GaleBoomerangRenderer.BOOMERANG_STATE, 0) ?: 0
        val isHovering = boomerangStateId == GaleBoomerangEntity.BoomerangState.HOVERING.id

        // Vitesse de rotation continue du Gale Boomerang (plus rapide en stase / vortex)
        val spinSpeed = if (isHovering) 1.25f else 0.85f
        val spinAngle = age * spinSpeed

        // Inclinaison aérodynamique selon l'état de vol
        val tiltPitch = if (isHovering) 0.05f else 0.22f
        val tiltRoll = if (isHovering) 0.0f else 0.12f

        boneSnapshots.get("root").ifPresent { rootBone ->
            rootBone.setRotation(tiltPitch, spinAngle, tiltRoll)
        }
    }
}
