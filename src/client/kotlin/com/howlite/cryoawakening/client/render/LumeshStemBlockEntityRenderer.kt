package com.howlite.cryoawakening.client.render

import com.geckolib.constant.dataticket.DataTicket
import com.howlite.cryoawakening.block.LumeshStemBlock
import com.howlite.cryoawakening.block.entity.LumeshStemBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

class LumeshStemRenderState : BlockEntityRenderState() {
    var age: Int = 0

    private val geckolibDataMap: MutableMap<DataTicket<*>, Any> = HashMap()
    override fun getDataMap(): MutableMap<DataTicket<*>, Any> = geckolibDataMap
}

/**
 * LumeshStemBlockEntityRenderer
 *
 * Rendu animé haute fidélité des 4 grandes feuilles de la plante Lumesh au stade 4.
 * Les feuilles s'élèvent vers le ciel, attachées par leur base sur la tige,
 * avec une face supérieure parfaitement éclairée par la lumière du jour et une
 * douce ondulation harmonique.
 */
class LumeshStemBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<LumeshStemBlockEntity, LumeshStemRenderState> {

    companion object {
        val TEXTURE: Identifier = Identifier.fromNamespaceAndPath(
            "cryo-awakening",
            "textures/block/lumesh_stem_stage_4.png"
        )
    }

    override fun createRenderState(): LumeshStemRenderState = LumeshStemRenderState()

    override fun extractRenderState(
        blockEntity: LumeshStemBlockEntity,
        state: LumeshStemRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        val blockState = blockEntity.blockState
        state.age = if (blockState.hasProperty(LumeshStemBlock.AGE)) {
            blockState.getValue(LumeshStemBlock.AGE)
        } else {
            0
        }
    }

    override fun submit(
        state: LumeshStemRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        // Seul le stade 4 (mature) affiche les 4 grandes feuilles animées
        if (state.age < 4) return

        val time = (System.currentTimeMillis() % 10000000L).toDouble() / 1000.0
        val renderType = RenderTypes.entityCutout(TEXTURE, false)
        val light = state.lightCoords

        // Rendu des 4 feuilles à 90° d'intervalle autour du centre de la tige
        for (i in 0 until 4) {
            poseStack.pushPose()

            // 1. Point d'attache sur la tige : centre [8, 2, 8] en pixels
            poseStack.translate(0.5, 2.0 / 16.0, 0.5)

            // 2. Rotation radiale en lacet (0°, 90°, 180°, 270°)
            val baseYaw = i * 90.0f
            poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw))

            // 3. Oscillation harmonique douce (Pitch et Roll)
            val phase = i * (Math.PI / 2.0)
            val swayPitch = (sin(time * 2.2 + phase) * 3.0 + sin(time * 1.1 + phase * 1.5) * 1.5).toFloat()
            val swayRoll = (cos(time * 1.8 + phase) * 2.0).toFloat()

            // 4. Inclinaison vers le haut (-35°) pour s'épanouir vers le ciel + ondulation
            poseStack.mulPose(Axis.XP.rotationDegrees(-35.0f + swayPitch))
            poseStack.mulPose(Axis.ZP.rotationDegrees(swayRoll))

            // 5. Rendu géométrique de la feuille
            submitNodeCollector.submitCustomGeometry(poseStack, renderType) { leafPose, consumer ->
                renderLeafQuad(leafPose, consumer, light)
            }

            poseStack.popPose()
        }
    }

    private fun renderLeafQuad(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        light: Int
    ) {
        // Dimensions de la feuille :
        // - Largeur (X) : centrée de -11.5 à +11.5 pixels (-0.72 à +0.72 blocs)
        // - Longueur (Z) : part de 0 (attache tige) et s'étend vers l'extérieur jusqu'à +24 pixels (+1.5 bloc)
        val x0 = -11.5f / 16.0f
        val x1 = 11.5f / 16.0f
        val z0 = 0.0f
        val z1 = 24.0f / 16.0f

        // Coordonnées UV :
        // - v0 = 0.0 (base de la nervure attachée à la tige)
        // - v1 = 24/64 = 0.375 (bord arrondi extérieur)
        val u0 = 0.0f
        val u1 = 23.0f / 64.0f
        val v0 = 0.0f
        val v1 = 24.0f / 64.0f

        // 1. Face supérieure (Top - CCW winding vers le haut +Y) -> Éclairée par le ciel
        consumer.addVertex(pose, x0, 0.0f, z0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, x0, 0.0f, z1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, x1, 0.0f, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, x1, 0.0f, z0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)

        // 2. Face inférieure (Bottom - CCW winding vers le bas -Y)
        consumer.addVertex(pose, x0, 0.0f, z0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, x1, 0.0f, z0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, x1, 0.0f, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, x0, 0.0f, z1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
    }
}
