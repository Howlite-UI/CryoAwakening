package com.howlite.cryoawakening.client.render

import com.geckolib.constant.dataticket.DataTicket
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.block.GaleBellowsBlock
import com.howlite.cryoawakening.block.entity.GaleBellowsBlockEntity
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
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3

class GaleBellowsRenderState : BlockEntityRenderState() {
    var gameTime: Long = 0L
    var partialTicks: Float = 0.0f
    var facing: Direction = Direction.NORTH

    private val geckolibDataMap: MutableMap<DataTicket<*>, Any> = HashMap()
    override fun getDataMap(): MutableMap<DataTicket<*>, Any> = geckolibDataMap
}

/**
 * GaleBellowsBlockEntityRenderer
 *
 * Rendu animé haute fidélité du Poumon Mécanique (Gale Bellows) basé sur le modèle Blockbench gale_bellows.json :
 * - boards_bot : Socle inférieur fixe [1..15, 0..2, 1..15]
 * - nozzle     : Cheminée/conduit central fixe [4..12, 1..17, 4..12]
 * - accordion  : Soufflet repliable coulissant [2..14, 2..topY, 2..14]
 * - boards_top : Plateau supérieur oscillant [1..15, topY..topY+2, 1..15]
 */
class GaleBellowsBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<GaleBellowsBlockEntity, GaleBellowsRenderState> {

    companion object {
        val TEXTURE: Identifier = CryoAwakening.id("textures/block/gale_bellows.png")
    }

    override fun createRenderState(): GaleBellowsRenderState = GaleBellowsRenderState()

    override fun extractRenderState(
        blockEntity: GaleBellowsBlockEntity,
        state: GaleBellowsRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.gameTime = blockEntity.level?.gameTime ?: 0L
        state.partialTicks = partialTicks
        state.facing = if (blockEntity.blockState.hasProperty(GaleBellowsBlock.FACING)) {
            blockEntity.blockState.getValue(GaleBellowsBlock.FACING)
        } else {
            Direction.NORTH
        }
    }

    override fun submit(
        state: GaleBellowsRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val renderType = RenderTypes.entityCutout(TEXTURE, false)
        val light = state.lightCoords

        poseStack.pushPose()

        // Rotation selon le FACING autour du centre [0.5, 0.5, 0.5]
        poseStack.translate(0.5, 0.0, 0.5)
        val rotY = when (state.facing) {
            Direction.NORTH -> 0.0f
            Direction.SOUTH -> 180.0f
            Direction.WEST -> 90.0f
            Direction.EAST -> 270.0f
            else -> 0.0f
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY))
        poseStack.translate(-0.5, 0.0, -0.5)

        // Respiration douce d'un poumon mécanique (cycle de 70 ticks ~ 3.5s)
        val cycle = 70.0
        val t = ((state.gameTime.toDouble() + state.partialTicks.toDouble()) % cycle) / cycle
        val comp = (0.5 - 0.5 * kotlin.math.cos(t * 2.0 * Math.PI)).toFloat()
        val topY = (12.0f - comp * 9.5f) / 16.0f
        val accordionHeight = topY - (2.0f / 16.0f)

        submitNodeCollector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            // 1. Socle fixe inférieur (boards_bot) : [1..15, 0..2, 1..15]
            renderBox(
                pose, consumer, light,
                1.0f / 16.0f, 0.0f, 1.0f / 16.0f,
                15.0f / 16.0f, 2.0f / 16.0f, 15.0f / 16.0f,
                uRim0 = 5.5f / 16.0f, vRim0 = 2.0f / 16.0f, uRim1 = 9.0f / 16.0f, vRim1 = 2.5f / 16.0f,
                uPlate0 = 0.0f, vPlate0 = 0.0f, uPlate1 = 3.5f / 16.0f, vPlate1 = 3.5f / 16.0f
            )

            // 2. Cheminée/Buse centrale fixe (nozzle) : [4..12, 1..17, 4..12]
            renderBox(
                pose, consumer, light,
                4.0f / 16.0f, 1.0f / 16.0f, 4.0f / 16.0f,
                12.0f / 16.0f, 17.0f / 16.0f, 12.0f / 16.0f,
                uRim0 = 3.5f / 16.0f, vRim0 = 0.0f, uRim1 = 5.5f / 16.0f, vRim1 = 4.0f / 16.0f,
                uPlate0 = 5.5f / 16.0f, vPlate0 = 0.0f, uPlate1 = 7.5f / 16.0f, vPlate1 = 2.0f / 16.0f
            )

            // 3. Accordéon repliable : [2..14, 2..topY, 2..14]
            if (accordionHeight > 0.005f) {
                renderBox(
                    pose, consumer, light,
                    2.0f / 16.0f, 2.0f / 16.0f, 2.0f / 16.0f,
                    14.0f / 16.0f, topY, 14.0f / 16.0f,
                    uRim0 = 3.0f / 16.0f, vRim0 = 4.0f / 16.0f, uRim1 = 6.0f / 16.0f, vRim1 = 6.5f / 16.0f,
                    uPlate0 = 0.0f, vPlate0 = 3.5f / 16.0f, uPlate1 = 3.0f / 16.0f, vPlate1 = 6.5f / 16.0f
                )
            }

            // 4. Plateau supérieur mobile (boards_top) : [1..15, topY..topY+2, 1..15]
            renderBox(
                pose, consumer, light,
                1.0f / 16.0f, topY, 1.0f / 16.0f,
                15.0f / 16.0f, topY + (2.0f / 16.0f), 15.0f / 16.0f,
                uRim0 = 5.5f / 16.0f, vRim0 = 2.0f / 16.0f, uRim1 = 9.0f / 16.0f, vRim1 = 2.5f / 16.0f,
                uPlate0 = 0.0f, vPlate0 = 0.0f, uPlate1 = 3.5f / 16.0f, vPlate1 = 3.5f / 16.0f
            )
        }

        poseStack.popPose()
    }

    private fun renderBox(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        light: Int,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        uRim0: Float, vRim0: Float, uRim1: Float, vRim1: Float,
        uPlate0: Float, vPlate0: Float, uPlate1: Float, vPlate1: Float
    ) {
        // Face UP (+Y)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uPlate0, vPlate0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uPlate0, vPlate1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uPlate1, vPlate1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uPlate1, vPlate0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)

        // Face DOWN (-Y)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(uPlate0, vPlate0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(uPlate1, vPlate0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uPlate1, vPlate1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uPlate0, vPlate1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)

        // Face NORTH (-Z)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)

        // Face SOUTH (+Z)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)

        // Face WEST (-X)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)

        // Face EAST (+X)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(uRim0, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(uRim1, vRim0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
    }
}
