package com.howlite.cryoawakening.client.render.entity

import com.howlite.cryoawakening.entity.GaleBoomerangEntity
import com.howlite.cryoawakening.item.ModItems
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.item.ItemModelResolver
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class GaleBoomerangRenderState : EntityRenderState() {
    val item: ItemStackRenderState = ItemStackRenderState()
    var spinAngle: Float = 0.0f
    private val dataMap: MutableMap<com.geckolib.constant.dataticket.DataTicket<*>, Any> = mutableMapOf()
    override fun getDataMap(): MutableMap<com.geckolib.constant.dataticket.DataTicket<*>, Any> = dataMap
}

/**
 * GaleBoomerangRenderer
 *
 * Renderer pour GaleBoomerangEntity utilisant directement le modèle d'item
 * standard (gale_boomerang.json) avec rotation aérodynamique continue en vol.
 */
class GaleBoomerangRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<GaleBoomerangEntity, GaleBoomerangRenderState>(context) {

    private val itemModelResolver: ItemModelResolver = context.itemModelResolver

    override fun createRenderState(): GaleBoomerangRenderState = GaleBoomerangRenderState()

    override fun extractRenderState(
        entity: GaleBoomerangEntity,
        state: GaleBoomerangRenderState,
        partialTick: Float
    ) {
        super.extractRenderState(entity, state, partialTick)

        val stack = if (entity.boomerangStack.isEmpty) ItemStack(ModItems.GALE_BOOMERANG) else entity.boomerangStack
        itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.GROUND, entity)

        val rotationSpeed = if (entity.boomerangState == GaleBoomerangEntity.BoomerangState.HOVERING) 55.0f else 40.0f
        state.spinAngle = (entity.tickCount + partialTick) * rotationSpeed
    }

    override fun submit(
        state: GaleBoomerangRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()

        // Taille du boomerang en vol (agrandi pour un rendu plus imposant)
        val scale = 1.65f
        poseStack.scale(scale, scale, scale)

        // Posé horizontalement (plan de vol)
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))

        // Rotation rapide autour du centre de gravité (le joyau bleu)
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.spinAngle))

        // Décalage pour aligner le centre de rotation exactement sur le joyau bleu (pixel 11, 4 sur 16x16)
        poseStack.translate(-0.1875f, -0.25f, 0.0f)

        // Rendu du modèle d'item standard (gale_boomerang.json)
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor)

        poseStack.popPose()
        super.submit(state, poseStack, collector, cameraRenderState)
    }
}
