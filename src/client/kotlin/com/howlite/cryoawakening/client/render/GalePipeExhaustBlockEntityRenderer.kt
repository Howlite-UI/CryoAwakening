package com.howlite.cryoawakening.client.render

import com.geckolib.constant.dataticket.DataTicket
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.block.GalePipeExhaustBlock
import com.howlite.cryoawakening.block.entity.GalePipeExhaustBlockEntity
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
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class GalePipeExhaustRenderState : BlockEntityRenderState() {
    var facing: Direction = Direction.NORTH
    var valveFacing: Direction = Direction.NORTH
    var visualAngle: Float = 0.0f

    private val geckolibDataMap: MutableMap<DataTicket<*>, Any> = HashMap()
    override fun getDataMap(): MutableMap<DataTicket<*>, Any> = geckolibDataMap
}

/**
 * GalePipeExhaustBlockEntityRenderer
 *
 * Rendu animé du volant de vanne (valve_wheel) qui s'oriente face au joueur
 * et tourne sur lui-même en temps réel lorsque la vitesse est modifiée.
 */
class GalePipeExhaustBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<GalePipeExhaustBlockEntity, GalePipeExhaustRenderState> {

    companion object {
        val VALVE_TEXTURE: Identifier = CryoAwakening.id("textures/block/gale_pipe/gale_pipe_valve.png")
    }

    override fun createRenderState(): GalePipeExhaustRenderState = GalePipeExhaustRenderState()

    override fun extractRenderState(
        blockEntity: GalePipeExhaustBlockEntity,
        state: GalePipeExhaustRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.facing = if (blockEntity.blockState.hasProperty(GalePipeExhaustBlock.FACING)) {
            blockEntity.blockState.getValue(GalePipeExhaustBlock.FACING)
        } else {
            Direction.NORTH
        }
        state.valveFacing = if (blockEntity.blockState.hasProperty(GalePipeExhaustBlock.VALVE_FACING)) {
            blockEntity.blockState.getValue(GalePipeExhaustBlock.VALVE_FACING)
        } else {
            Direction.NORTH
        }
        // Interpolation fluide de l'angle du volant
        state.visualAngle = Mth.lerp(partialTicks, blockEntity.prevVisualAngle, blockEntity.visualAngle)
    }

    override fun submit(
        state: GalePipeExhaustRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val renderType = RenderTypes.entityCutout(VALVE_TEXTURE, false)
        val light = state.lightCoords

        poseStack.pushPose()

        // 1. Positionnement et orientation du bloc selon FACING et VALVE_FACING
        poseStack.translate(0.5, 0.5, 0.5)
        when (state.facing) {
            Direction.NORTH -> {}
            Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))
            Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0f))
            Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0f))
            Direction.UP -> {
                val yRot = when (state.valveFacing) {
                    Direction.NORTH -> 0.0f
                    Direction.SOUTH -> 180.0f
                    Direction.WEST -> 90.0f
                    Direction.EAST -> 270.0f
                    else -> 0.0f
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(yRot))
                poseStack.mulPose(Axis.XP.rotationDegrees(270.0f))
            }
            Direction.DOWN -> {
                val yRot = when (state.valveFacing) {
                    Direction.NORTH -> 180.0f
                    Direction.SOUTH -> 0.0f
                    Direction.WEST -> 270.0f
                    Direction.EAST -> 90.0f
                    else -> 0.0f
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(yRot))
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
            }
        }

        // 2. Déplacement au centre du volant de vanne sur la surface du tuyau (Y=12.5) et abaissé vers le sol (Z=0.0)
        val wheelCenterX = 0.0f
        val wheelCenterY = (12.5f - 8.0f) / 16.0f // 0.28125 (en surface extérieure du tuyau)
        val wheelCenterZ = 0.0f                   // Abaissé de 1 pixel vers le sol (éloigné de la bride)

        poseStack.translate(wheelCenterX.toDouble(), wheelCenterY.toDouble(), wheelCenterZ.toDouble())

        // 3. Rotation du volant de vanne sur lui-même selon l'angle de réglage
        poseStack.mulPose(Axis.YP.rotationDegrees(state.visualAngle))

        // 4. Rendu du volant (taille 12x12 pixels de -6/16 à +6/16)
        val r = 6.0f / 16.0f
        val u0 = 2.0f / 16.0f
        val v0 = 2.0f / 16.0f
        val u1 = 14.0f / 16.0f
        val v1 = 14.0f / 16.0f

        submitNodeCollector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            // Face UP (+Y)
            consumer.addVertex(pose, -r, 0.0f, -r).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
            consumer.addVertex(pose, -r, 0.0f, r).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
            consumer.addVertex(pose, r, 0.0f, r).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
            consumer.addVertex(pose, r, 0.0f, -r).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)

            // Face DOWN (-Y)
            consumer.addVertex(pose, -r, -0.005f, -r).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
            consumer.addVertex(pose, r, -0.005f, -r).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
            consumer.addVertex(pose, r, -0.005f, r).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
            consumer.addVertex(pose, -r, -0.005f, r).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        }

        poseStack.popPose()
    }
}
