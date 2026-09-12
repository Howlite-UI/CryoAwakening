package com.howlite.cryoawakening.client.render.item

import com.geckolib.renderer.GeoItemRenderer
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.client.model.item.ClawshotModel
import com.howlite.cryoawakening.client.model.property.ClawshotAimingProgressProperty
import com.howlite.cryoawakening.item.ClawshotItem
import net.minecraft.client.Minecraft
import com.geckolib.constant.DataTickets
import com.geckolib.renderer.base.BoneSnapshots
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemDisplayContext

/**
 * Renderer GeckoLib pour le Clawshot en main :
 * - Masque dynamiquement l'élément anchor_core dès que le grappin est tiré / en vol dans le monde.
 * - Anime l'ouverture continue des 3 griffes vers l'EXTÉRIEUR lorsque le joueur vise une surface à portée.
 * - Désactive strictement l'animation dans l'inventaire / GUI pour éviter que tous les exemplaires s'animent en même temps.
 */
class ClawshotItemRenderer : GeoItemRenderer<ClawshotItem>(ClawshotModel()) {

    override fun adjustModelBonesForRender(
        renderPassInfo: RenderPassInfo<GeoRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots)

        val perspective = renderPassInfo.renderState().getGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE)
        val isInHand = perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
                       perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                       perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                       perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND

        // Dans l'inventaire, les coffres, au sol, dans les cadres ou en icône GUI :
        // L'ancre est toujours complète et les griffes sont au repos (fermées)
        if (!isInHand) {
            boneSnapshots.get("anchor_core").ifPresent {
                it.skipRender(false)
                it.skipChildrenRender(false)
            }
            boneSnapshots.get("claw_1").ifPresent { it.setRotZ(0.0f) }
            boneSnapshots.get("claw_2").ifPresent { it.setRotZ(0.0f) }
            boneSnapshots.get("claw_3").ifPresent { it.setRotZ(0.0f) }
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player
        val level = player?.level()

        // Détection de la main exacte qui porte cet item
        val isMainHandPerspective = if (player?.mainArm == HumanoidArm.RIGHT) {
            perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        } else {
            perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
        }
        val currentHand = if (isMainHandPerspective) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
        val activeAnchorThisHand = if (player != null && level != null) ClawshotItem.findActiveAnchor(level, player, currentHand) else null
        val isFiredFromThisHand = activeAnchorThisHand != null

        if (isFiredFromThisHand) {
            // Dès que le grappin est dans le monde depuis cette main, la tête (anchor_core) se détache
            boneSnapshots.get("anchor_core").ifPresent {
                it.skipRender(true)
                it.skipChildrenRender(true)
            }
        } else {
            boneSnapshots.get("anchor_core").ifPresent {
                it.skipRender(false)
                it.skipChildrenRender(false)
            }

            // Animation fluide de l'ouverture des 3 pinces radialement vers l'EXTÉRIEUR à la visée (+24°)
            val progress = ClawshotAimingProgressProperty.updateAndGetProgress(currentHand)
            val openAngleRad = 24.0f * progress * (Math.PI.toFloat() / 180.0f)
            boneSnapshots.get("claw_1").ifPresent { it.setRotZ(openAngleRad) }
            boneSnapshots.get("claw_2").ifPresent { it.setRotZ(openAngleRad) }
            boneSnapshots.get("claw_3").ifPresent { it.setRotZ(openAngleRad) }
        }
    }
}
