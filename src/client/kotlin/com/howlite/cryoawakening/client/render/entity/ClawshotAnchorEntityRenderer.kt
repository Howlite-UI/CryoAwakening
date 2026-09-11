package com.howlite.cryoawakening.client.render.entity

import com.geckolib.constant.dataticket.DataTicket
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ClawshotAnchorRenderState
 *
 * Mémorise l'état dynamique de l'ancre pour le pipeline de rendu moderne 1.21+.
 */
class ClawshotAnchorRenderState : EntityRenderState() {
    var anchorState: Int = 0
    var clawOpenAmount: Float = 1.0f
    var hasOwner: Boolean = false
    var ownerHandPos: Vec3 = Vec3.ZERO
    var anchorPos: Vec3 = Vec3.ZERO
    var yaw: Float = 0.0f
    var pitch: Float = 0.0f
    var hookDirection: Int = -1

    private val dataMap: MutableMap<DataTicket<*>, Any> = mutableMapOf()
    override fun getDataMap(): MutableMap<DataTicket<*>, Any> = dataMap
}

/**
 * ClawshotAnchorEntityRenderer
 *
 * Rendu haute fidélité de la tête du grappin (anchor_core) et de la chaîne maillée 3D (chain) :
 * 1. Tête de grappin : Centre cubique (core) + 3 griffes articulées (claw) s'ouvrant en vol et se refermant à l'impact.
 * 2. Chaîne 3D dynamique : Maillons croisés alternés à 90° reliant le gantelet (handle_gauntlet) à l'ancre.
 * 3. Texturation conforme au modèle Blockbench clawshot.json et textures/item/clawshot/clawshot.png.
 */
class ClawshotAnchorEntityRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<ClawshotAnchorEntity, ClawshotAnchorRenderState>(context) {

    companion object {
        val TEXTURE: Identifier = CryoAwakening.id("textures/item/clawshot/clawshot.png")
        val CHAIN_TEXTURE: Identifier = Identifier.withDefaultNamespace("textures/block/iron_chain.png")
    }

    override fun createRenderState(): ClawshotAnchorRenderState = ClawshotAnchorRenderState()

    override fun extractRenderState(
        entity: ClawshotAnchorEntity,
        state: ClawshotAnchorRenderState,
        partialTick: Float
    ) {
        super.extractRenderState(entity, state, partialTick)

        state.anchorState = entity.anchorState.id
        state.clawOpenAmount = Mth.lerp(partialTick, entity.prevClawOpen, entity.clawOpenAmount)
        state.hookDirection = entity.hookDirection

        val anchorPos = entity.getPosition(partialTick)
        state.anchorPos = anchorPos

        val owner = entity.getOwnerEntity()
        if (owner != null && owner.isAlive) {
            state.hasOwner = true
            state.ownerHandPos = computeOwnerHandPosition(owner, entity.usedHand, partialTick)

            // Calcul de l'orientation de la tête dans la direction de vol / d'impact
            if (entity.anchorState == ClawshotAnchorEntity.AnchorState.FLYING) {
                val motion = entity.deltaMovement
                if (motion.lengthSqr() > 1.0e-4) {
                    val horizDist = sqrt(motion.x * motion.x + motion.z * motion.z)
                    state.yaw = (atan2(-motion.x, -motion.z) * (180.0 / Math.PI)).toFloat()
                    state.pitch = (atan2(motion.y, horizDist) * (180.0 / Math.PI)).toFloat()
                }
            } else if (entity.anchorState == ClawshotAnchorEntity.AnchorState.RETRACTING) {
                val toOwner = state.ownerHandPos.subtract(anchorPos)
                if (toOwner.lengthSqr() > 1.0e-4) {
                    val horizDist = sqrt(toOwner.x * toOwner.x + toOwner.z * toOwner.z)
                    state.yaw = (atan2(-toOwner.x, -toOwner.z) * (180.0 / Math.PI)).toFloat()
                    state.pitch = (atan2(toOwner.y, horizDist) * (180.0 / Math.PI)).toFloat()
                }
            } else if (entity.anchorState == ClawshotAnchorEntity.AnchorState.HOOKED_BLOCK) {
                // Accroché dans un bloc : l'ancre s'aligne proprement avec la face touchée
                if (state.hookDirection >= 0) {
                    when (net.minecraft.core.Direction.from3DDataValue(state.hookDirection)) {
                        net.minecraft.core.Direction.SOUTH -> { state.yaw = 0.0f; state.pitch = 0.0f }
                        net.minecraft.core.Direction.NORTH -> { state.yaw = 180.0f; state.pitch = 0.0f }
                        net.minecraft.core.Direction.WEST -> { state.yaw = 90.0f; state.pitch = 0.0f }
                        net.minecraft.core.Direction.EAST -> { state.yaw = -90.0f; state.pitch = 0.0f }
                        net.minecraft.core.Direction.UP -> { state.pitch = -90.0f }
                        net.minecraft.core.Direction.DOWN -> { state.pitch = 90.0f }
                    }
                } else {
                    val toOwner = state.ownerHandPos.subtract(anchorPos)
                    if (toOwner.lengthSqr() > 1.0e-4) {
                        val horizDist = sqrt(toOwner.x * toOwner.x + toOwner.z * toOwner.z)
                        state.yaw = (atan2(toOwner.x, toOwner.z) * (180.0 / Math.PI)).toFloat()
                        state.pitch = (atan2(-toOwner.y, horizDist) * (180.0 / Math.PI)).toFloat()
                    }
                }
            } else {
                // Accroché à une entité : pointe vers l'entité
                val toOwner = state.ownerHandPos.subtract(anchorPos)
                if (toOwner.lengthSqr() > 1.0e-4) {
                    val horizDist = sqrt(toOwner.x * toOwner.x + toOwner.z * toOwner.z)
                    state.yaw = (atan2(toOwner.x, toOwner.z) * (180.0 / Math.PI)).toFloat()
                    state.pitch = (atan2(-toOwner.y, horizDist) * (180.0 / Math.PI)).toFloat()
                }
            }
        } else {
            state.hasOwner = false
        }
    }

    override fun submit(
        state: ClawshotAnchorRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        val renderType = RenderTypes.entityCutout(TEXTURE, false)
        val chainRenderType = RenderTypes.entityCutout(CHAIN_TEXTURE, false)
        val light = state.lightCoords

        // ==========================================
        // 1. RENDU DE LA TÊTE DU GRAPPIN (anchor_core)
        // ==========================================
        poseStack.pushPose()

        // Orientation globale de la tête dans l'axe de visée / vol
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw))
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch))

        // Rotation de -90° sur l'axe X pour que l'axe -Y du modèle (direction des griffes)
        // devienne l'axe de vol vers la cible
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))

        // Échelle fidèle pour une présence visuelle percutante
        val scale = 1.35f
        poseStack.scale(scale, scale, scale)

        // Rendu du cube central (core) avec texture clawshot
        collector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            renderCore(pose, consumer, light)
        }

        // Rendu du collier de chaîne à l'arrière avec texture vanilla chain
        collector.submitCustomGeometry(poseStack, chainRenderType) { pose, consumer ->
            renderCollar(pose, consumer, light)
        }

        // Animation d'ouverture des griffes :
        // Vol (clawOpenAmount ~ 1.0) : griffes écartées (-24°) prêtes à crocheter
        // Accroché ou rétracté (clawOpenAmount ~ 0.0) : griffes refermées (0°) fermement
        val openAngle = -24.0f * state.clawOpenAmount

        // 1. Claw 1 (azimut 135°)
        poseStack.pushPose()
        poseStack.translate(0.0f, -0.5f / 16.0f, 0.0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(135.0f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(openAngle))
        collector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            renderClaw1(pose, consumer, light)
        }
        poseStack.popPose()

        // 2. Claw 2 (azimut -135°)
        poseStack.pushPose()
        poseStack.translate(0.0f, -0.5f / 16.0f, 0.0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(openAngle))
        collector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            renderClaw2(pose, consumer, light)
        }
        poseStack.popPose()

        // 3. Claw 3 (azimut 0°)
        poseStack.pushPose()
        poseStack.translate(0.0f, -0.5f / 16.0f, 0.0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(openAngle))
        collector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
            renderClaw3(pose, consumer, light)
        }
        poseStack.popPose()

        poseStack.popPose()

        // ==========================================
        // 2. RENDU DYNAMIQUE DE LA CHAÎNE 3D (chain)
        // ==========================================
        if (state.hasOwner) {
            val diff = state.ownerHandPos.subtract(state.anchorPos)
            val totalDist = diff.length()

            if (totalDist > 0.3) {
                collector.submitCustomGeometry(poseStack, chainRenderType) { pose, consumer ->
                    renderChainLinks(pose, consumer, diff, totalDist, state.anchorState, light)
                }
            }
        }

        super.submit(state, poseStack, collector, cameraRenderState)
    }

    /**
     * Rendu d'un pavé texturé (boîte 3D) à faces indépendantes.
     */
    private fun renderBox(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        light: Int,
        minX: Float, maxX: Float,
        minY: Float, maxY: Float,
        minZ: Float, maxZ: Float,
        northUv: FloatArray,
        eastUv: FloatArray,
        southUv: FloatArray,
        westUv: FloatArray,
        upUv: FloatArray,
        downUv: FloatArray
    ) {
        // UP (+Y)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(upUv[0] / 32f, upUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(upUv[0] / 32f, upUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(upUv[2] / 32f, upUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(upUv[2] / 32f, upUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)

        // DOWN (-Y)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(downUv[0] / 32f, downUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(downUv[2] / 32f, downUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(downUv[2] / 32f, downUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(downUv[0] / 32f, downUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, -1f, 0f)

        // NORTH (-Z)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(northUv[0] / 32f, northUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(northUv[0] / 32f, northUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(northUv[2] / 32f, northUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(northUv[2] / 32f, northUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)

        // SOUTH (+Z)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(southUv[0] / 32f, southUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(southUv[0] / 32f, southUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(southUv[2] / 32f, southUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(southUv[2] / 32f, southUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)

        // WEST (-X)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(255, 255, 255, 255).setUv(westUv[0] / 32f, westUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, minY, minZ).setColor(255, 255, 255, 255).setUv(westUv[0] / 32f, westUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(255, 255, 255, 255).setUv(westUv[2] / 32f, westUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(westUv[2] / 32f, westUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)

        // EAST (+X)
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(255, 255, 255, 255).setUv(eastUv[0] / 32f, eastUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(255, 255, 255, 255).setUv(eastUv[0] / 32f, eastUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(255, 255, 255, 255).setUv(eastUv[2] / 32f, eastUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(255, 255, 255, 255).setUv(eastUv[2] / 32f, eastUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
    }

    /**
     * Rendu d'un quad plat à double face.
     */
    private fun renderDoubleSidedQuad(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        light: Int,
        minX: Float, maxX: Float,
        minY: Float, maxY: Float,
        z: Float,
        frontUv: FloatArray,
        backUv: FloatArray
    ) {
        // Face recto (+Z normal)
        consumer.addVertex(pose, minX, maxY, z).setColor(255, 255, 255, 255).setUv(frontUv[0] / 32f, frontUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, minX, minY, z).setColor(255, 255, 255, 255).setUv(frontUv[0] / 32f, frontUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, minY, z).setColor(255, 255, 255, 255).setUv(frontUv[2] / 32f, frontUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, maxY, z).setColor(255, 255, 255, 255).setUv(frontUv[2] / 32f, frontUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)

        // Face verso (-Z normal)
        consumer.addVertex(pose, maxX, maxY, z).setColor(255, 255, 255, 255).setUv(backUv[0] / 32f, backUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, maxX, minY, z).setColor(255, 255, 255, 255).setUv(backUv[0] / 32f, backUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, minY, z).setColor(255, 255, 255, 255).setUv(backUv[2] / 32f, backUv[3] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, maxY, z).setColor(255, 255, 255, 255).setUv(backUv[2] / 32f, backUv[1] / 32f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
    }

    /**
     * Rendu du cube central (core) selon clawshot.json.
     * Dimensions : 5x2x5 pixels.
     */
    private fun renderCore(pose: PoseStack.Pose, consumer: VertexConsumer, light: Int) {
        renderBox(
            pose, consumer, light,
            -2.5f / 16f, 2.5f / 16f,
            0.0f, 2.0f / 16f,
            -2.5f / 16f, 2.5f / 16f,
            floatArrayOf(6.5f, 0.0f, 9.0f, 1.0f),
            floatArrayOf(6.5f, 1.0f, 9.0f, 2.0f),
            floatArrayOf(6.5f, 2.0f, 9.0f, 3.0f),
            floatArrayOf(6.5f, 3.0f, 9.0f, 4.0f),
            floatArrayOf(0.0f, 0.0f, 2.5f, 2.5f),
            floatArrayOf(0.0f, 2.5f, 2.5f, 5.0f)
        )
    }

    /**
     * Rendu du collier de raccord de chaîne à l'arrière du core selon clawshot.json avec texture de chaîne vanilla.
     */
    private fun renderCollar(pose: PoseStack.Pose, consumer: VertexConsumer, light: Int) {
        val minX = -1.5f / 16f
        val maxX = 1.5f / 16f
        val minY = 0.0f
        val maxY = 4.0f / 16f

        val u0 = 0.0f
        val u1 = 3.0f / 16.0f
        val v0 = 0.0f
        val v1 = 1.0f

        // Plan 1 (orienté Z)
        consumer.addVertex(pose, minX, maxY, 0f).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, minX, minY, 0f).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, minY, 0f).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)
        consumer.addVertex(pose, maxX, maxY, 0f).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, 1f)

        consumer.addVertex(pose, maxX, maxY, 0f).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, maxX, minY, 0f).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, minY, 0f).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)
        consumer.addVertex(pose, minX, maxY, 0f).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f)

        // Plan 2 croisé à 90° (orienté X)
        consumer.addVertex(pose, 0f, maxY, minX).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, 0f, minY, minX).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, 0f, minY, maxX).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)
        consumer.addVertex(pose, 0f, maxY, maxX).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1f, 0f, 0f)

        consumer.addVertex(pose, 0f, maxY, maxX).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, 0f, minY, maxX).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, 0f, minY, minX).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
        consumer.addVertex(pose, 0f, maxY, minX).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1f, 0f, 0f)
    }

    /**
     * Rendu de la griffe 1 (azimut 135°) selon clawshot.json.
     */
    private fun renderClaw1(pose: PoseStack.Pose, consumer: VertexConsumer, light: Int) {
        // Base articulée (2x3x2 pixels)
        renderBox(
            pose, consumer, light,
            -4.0f / 16f, -2.0f / 16f,
            -1.0f / 16f, 2.0f / 16f,
            -1.0f / 16f, 1.0f / 16f,
            floatArrayOf(0.0f, 8.0f, 1.0f, 9.5f),
            floatArrayOf(1.0f, 8.0f, 2.0f, 9.5f),
            floatArrayOf(2.0f, 8.0f, 3.0f, 9.5f),
            floatArrayOf(3.0f, 8.0f, 4.0f, 9.5f),
            floatArrayOf(5.0f, 9.0f, 6.0f, 10.0f),
            floatArrayOf(9.0f, 8.0f, 10.0f, 9.0f)
        )
        // Lame acérée (3x6 pixels plate à double face)
        renderDoubleSidedQuad(
            pose, consumer, light,
            -4.0f / 16f, -1.0f / 16f,
            -7.0f / 16f, -1.0f / 16f,
            0.0f,
            floatArrayOf(0.0f, 5.0f, 1.5f, 8.0f),
            floatArrayOf(5.0f, 0.0f, 6.5f, 3.0f)
        )
    }

    /**
     * Rendu de la griffe 2 (azimut -135°) selon clawshot.json.
     */
    private fun renderClaw2(pose: PoseStack.Pose, consumer: VertexConsumer, light: Int) {
        // Base articulée (2x3x2 pixels)
        renderBox(
            pose, consumer, light,
            -4.0f / 16f, -2.0f / 16f,
            -1.0f / 16f, 2.0f / 16f,
            -1.0f / 16f, 1.0f / 16f,
            floatArrayOf(6.0f, 8.0f, 7.0f, 9.5f),
            floatArrayOf(7.0f, 8.0f, 8.0f, 9.5f),
            floatArrayOf(8.0f, 8.0f, 9.0f, 9.5f),
            floatArrayOf(9.0f, 0.0f, 10.0f, 1.5f),
            floatArrayOf(9.0f, 9.0f, 10.0f, 10.0f),
            floatArrayOf(0.0f, 9.5f, 1.0f, 10.5f)
        )
        // Lame acérée (3x6 pixels plate à double face)
        renderDoubleSidedQuad(
            pose, consumer, light,
            -4.0f / 16f, -1.0f / 16f,
            -7.0f / 16f, -1.0f / 16f,
            0.0f,
            floatArrayOf(1.5f, 5.0f, 3.0f, 8.0f),
            floatArrayOf(3.0f, 5.0f, 4.5f, 8.0f)
        )
    }

    /**
     * Rendu de la griffe 3 (azimut 0°) selon clawshot.json.
     */
    private fun renderClaw3(pose: PoseStack.Pose, consumer: VertexConsumer, light: Int) {
        // Base articulée (2x3x2 pixels)
        renderBox(
            pose, consumer, light,
            -4.0f / 16f, -2.0f / 16f,
            -1.0f / 16f, 2.0f / 16f,
            -1.0f / 16f, 1.0f / 16f,
            floatArrayOf(9.0f, 1.5f, 10.0f, 3.0f),
            floatArrayOf(9.0f, 3.0f, 10.0f, 4.5f),
            floatArrayOf(4.0f, 9.0f, 5.0f, 10.5f),
            floatArrayOf(9.0f, 4.5f, 10.0f, 6.0f),
            floatArrayOf(1.0f, 9.5f, 2.0f, 10.5f),
            floatArrayOf(2.0f, 9.5f, 3.0f, 10.5f)
        )
        // Lame acérée (3x6 pixels plate à double face)
        renderDoubleSidedQuad(
            pose, consumer, light,
            -4.0f / 16f, -1.0f / 16f,
            -7.0f / 16f, -1.0f / 16f,
            0.0f,
            floatArrayOf(5.0f, 3.0f, 6.5f, 6.0f),
            floatArrayOf(4.5f, 6.0f, 6.0f, 9.0f)
        )
    }

    /**
     * Rendu haute fidélité de la chaîne 3D :
     * - Double ruban croisé à 90° ('X'-cross) continu sur toute la longueur,
     *   reproduisant fidèlement le rendu du bloc de chaîne en fer vanilla (textures/block/iron_chain.png).
     * - Coordonnée V directement proportionnelle à la distance métrique réelle (1.0 par bloc,
     *   correspondant aux 16 pixels du maillon vanilla), garantissant un déroulement 100% continu
     *   sans aucun sautillement, inversion ou snapping.
     * - Repère orthonormé stable le long du vecteur sans rupture de gimbal lock.
     */
    private fun renderChainLinks(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        diff: Vec3,
        totalDist: Double,
        anchorState: Int,
        light: Int
    ) {
        // Largeur exacte correspondant aux 3 pixels du maillon vanilla (3/16 / 2 = 1.5/16)
        val halfWidth = (1.5f / 16.0f) * 0.95f

        // Coordonnées horizontales UV (3 colonnes de pixels de iron_chain.png)
        val u0 = 0.0f
        val u1 = 3.0f / 16.0f

        // Découpage en segments lisses
        val isHooked = (anchorState == 1 || anchorState == 2)
        val numSegments = (totalDist * 3.0).toInt().coerceIn(6, 36)

        for (i in 0 until numSegments) {
            val t0 = i.toDouble() / numSegments.toDouble()
            val t1 = (i + 1).toDouble() / numSegments.toDouble()

            // Fléchissement naturel subtil uniquement en vol libre
            val sag0 = if (isHooked) 0.0 else (sin(t0 * Math.PI) * 0.10 * (totalDist / 16.0).coerceAtMost(1.0))
            val sag1 = if (isHooked) 0.0 else (sin(t1 * Math.PI) * 0.10 * (totalDist / 16.0).coerceAtMost(1.0))

            val x0 = (diff.x * t0).toFloat()
            val y0 = (diff.y * t0 - sag0).toFloat()
            val z0 = (diff.z * t0).toFloat()

            val x1 = (diff.x * t1).toFloat()
            val y1 = (diff.y * t1 - sag1).toFloat()
            val z1 = (diff.z * t1).toFloat()

            val dx = x1 - x0
            val dy = y1 - y0
            val dz = z1 - z0
            val segLen = sqrt(dx * dx + dy * dy + dz * dz)
            if (segLen < 1.0e-5f) continue

            val ndx = (dx / segLen).toDouble()
            val ndy = (dy / segLen).toDouble()
            val ndz = (dz / segLen).toDouble()

            // Repère orthonormé sans gimbal lock
            val refUp = if (kotlin.math.abs(ndy) < 0.95) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val tangent = Vec3(ndx, ndy, ndz)
            val normal = tangent.cross(refUp).normalize()
            val binormal = tangent.cross(normal)

            // Diagonales à 45° pour former la croix 'X' du maillon 3D
            val diag = 0.7071f * halfWidth
            val uX = ((normal.x + binormal.x) * diag).toFloat()
            val uY = ((normal.y + binormal.y) * diag).toFloat()
            val uZ = ((normal.z + binormal.z) * diag).toFloat()

            val wX = ((normal.x - binormal.x) * diag).toFloat()
            val wY = ((normal.y - binormal.y) * diag).toFloat()
            val wZ = ((normal.z - binormal.z) * diag).toFloat()

            // V continu : 1.0 par bloc parcouru
            val v0 = (t0 * totalDist).toFloat()
            val v1 = (t1 * totalDist).toFloat()

            // Plan 1 (Ruban diagonal U) - double face
            consumer.addVertex(pose, x0 - uX, y0 - uY, z0 - uZ).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, wX, wY, wZ)
            consumer.addVertex(pose, x1 - uX, y1 - uY, z1 - uZ).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, wX, wY, wZ)
            consumer.addVertex(pose, x1 + uX, y1 + uY, z1 + uZ).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, wX, wY, wZ)
            consumer.addVertex(pose, x0 + uX, y0 + uY, z0 + uZ).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, wX, wY, wZ)

            consumer.addVertex(pose, x0 + uX, y0 + uY, z0 + uZ).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -wX, -wY, -wZ)
            consumer.addVertex(pose, x1 + uX, y1 + uY, z1 + uZ).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -wX, -wY, -wZ)
            consumer.addVertex(pose, x1 - uX, y1 - uY, z1 - uZ).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -wX, -wY, -wZ)
            consumer.addVertex(pose, x0 - uX, y0 - uY, z0 - uZ).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -wX, -wY, -wZ)

            // Plan 2 (Ruban diagonal W à 90° du Plan 1) - double face
            consumer.addVertex(pose, x0 - wX, y0 - wY, z0 - wZ).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, uX, uY, uZ)
            consumer.addVertex(pose, x1 - wX, y1 - wY, z1 - wZ).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, uX, uY, uZ)
            consumer.addVertex(pose, x1 + wX, y1 + wY, z1 + wZ).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, uX, uY, uZ)
            consumer.addVertex(pose, x0 + wX, y0 + wY, z0 + wZ).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, uX, uY, uZ)

            consumer.addVertex(pose, x0 + wX, y0 + wY, z0 + wZ).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -uX, -uY, -uZ)
            consumer.addVertex(pose, x1 + wX, y1 + wY, z1 + wZ).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -uX, -uY, -uZ)
            consumer.addVertex(pose, x1 - wX, y1 - wY, z1 - wZ).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -uX, -uY, -uZ)
            consumer.addVertex(pose, x0 - wX, y0 - wY, z0 - wZ).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -uX, -uY, -uZ)
        }
    }

    /**
     * Détermine la position exacte dans le monde de la main tenant le grappin.
     */
    private fun computeOwnerHandPosition(
        owner: net.minecraft.world.entity.player.Player,
        hand: InteractionHand,
        partialTick: Float
    ): Vec3 {
        val mc = Minecraft.getInstance()
        val isFirstPerson = mc.options.cameraType.isFirstPerson && owner == mc.player
        val isRightArm = (owner.mainArm == HumanoidArm.RIGHT)
        val side = if (hand == InteractionHand.MAIN_HAND) {
            if (isRightArm) 1.0 else -1.0
        } else {
            if (isRightArm) -1.0 else 1.0
        }

        return if (isFirstPerson) {
            val eyePos = owner.getEyePosition(partialTick)
            val look = owner.getViewVector(partialTick)
            val right = look.cross(Vec3(0.0, 1.0, 0.0)).normalize()
            val up = right.cross(look).normalize()
            eyePos.add(right.scale(side * 0.35)).add(look.scale(0.42)).add(up.scale(-0.25))
        } else {
            val bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * (Math.PI.toFloat() / 180f)
            val xOff = -cos(bodyYaw) * 0.36 * side
            val zOff = -sin(bodyYaw) * 0.36 * side
            val yOff = if (owner.isShiftKeyDown) 0.65 else 0.95
            owner.getPosition(partialTick).add(xOff, yOff, zOff)
        }
    }
}
