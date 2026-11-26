package com.howlite.cryoawakening.client.render

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * InfiltrationWorldRenderer
 *
 * Rendu 3D dans le monde (inspiré de Zelda: Tears of the Kingdom Ascend) :
 * 1. En phase AIMING :
 *    - Projection lumineuse plate collée exactement sous le plafond ciblé (disque radial,
 *      anneaux concentriques pulsants, grille géométrique rotative).
 *    - Vert émeraude / cyan cryogénique si le plafond est traversable, rouge vif (avec croix X) si invalide.
 *    - Faisceau vertical translucide + anneaux montants reliant le joueur au point d'impact au plafond.
 * 2. En phase RISING_TO_CEILING :
 *    - Portail d'entrée au plafond qui s'élargit + anneau de décollage sous les pieds du joueur.
 * 3. En phase PEEKING / EMERGING :
 *    - Portail lumineux circulaire posé au ras du sol autour de la tête et des épaules du joueur.
 */
object InfiltrationWorldRenderer {

    fun register() {
        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            render(context)
        }
    }

    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val phase = InfiltrationClientHandler.phase
        if (phase == InfiltrationPhase.IDLE || phase.isInsideBlocks()) return

        val camPos = context.levelState().cameraRenderState.pos
        val poseStack = context.poseStack()
        val collector = context.submitNodeCollector()
        val timeSec = (System.currentTimeMillis() % 1000000L) / 1000.0f

        when (phase) {
            InfiltrationPhase.AIMING -> {
                val target = InfiltrationClientHandler.currentTarget ?: return
                val aimScale = InfiltrationClientHandler.aimAnim.coerceIn(0.05f, 1.0f)
                val isValid = target.valid
                val isErrorPulse = InfiltrationClientHandler.invalidFlashTicks > 0

                val rx = (target.columnX - camPos.x).toFloat()
                val ry = (target.ceilingHitY - 0.018 - camPos.y).toFloat()
                val rz = (target.columnZ - camPos.z).toFloat()
                val playerFootRelY = (player.y + 0.05 - camPos.y).toFloat()

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    if (isValid) {
                        // Palette TOTK Émeraude-Cyan
                        renderValidCeilingTarget(pose, consumer, rx, ry, rz, playerFootRelY, aimScale, timeSec)
                    } else {
                        // Palette Rouge Invalide
                        renderInvalidCeilingTarget(pose, consumer, rx, ry, rz, aimScale, timeSec, isErrorPulse)
                    }
                }
                poseStack.popPose()
            }

            InfiltrationPhase.RISING_TO_CEILING -> {
                val progress = InfiltrationClientHandler.getPhaseProgress(0.0f)
                val cx = (InfiltrationClientHandler.targetX - camPos.x).toFloat()
                val ceilY = (InfiltrationClientHandler.ceilingHitY - 0.018 - camPos.y).toFloat()
                val origX = (InfiltrationClientHandler.originX - camPos.x).toFloat()
                val origY = (InfiltrationClientHandler.originY + 0.03 - camPos.y).toFloat()
                val origZ = (InfiltrationClientHandler.originZ - camPos.z).toFloat()
                val cz = (InfiltrationClientHandler.targetZ - camPos.z).toFloat()

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    // Portail d'entrée au plafond qui s'ouvre
                    val portalScale = 1.0f + progress * 0.45f
                    drawHorizontalDisc(pose, consumer, cx, ceilY, cz, 0.92f * portalScale, 28, 80, 255, 220, 145, 20, 220, 175, 0)
                    drawRing(pose, consumer, cx, ceilY - 0.008f, cz, 0.72f * portalScale, 0.86f * portalScale, 32, 130, 255, 235, 220)
                    drawRing(pose, consumer, cx, ceilY - 0.012f, cz, 1.02f * portalScale, 1.12f * portalScale, 32, 46, 235, 175, 175)

                    // Colonne lumineuse vibrante reliant le départ au plafond
                    drawEnergyColumn(pose, consumer, cx, origY, ceilY, cz, 0.58f, timeSec, 0.85f)

                    // Onde de décollage au sol
                    val launchRadius = 0.5f + progress * 1.4f
                    val launchAlpha = ((1.0f - progress) * 190.0f).toInt().coerceIn(0, 255)
                    drawRing(pose, consumer, origX, origY, origZ, launchRadius - 0.12f, launchRadius, 28, 70, 255, 210, launchAlpha)
                }
                poseStack.popPose()
            }

            InfiltrationPhase.PEEKING, InfiltrationPhase.EMERGING -> {
                val cx = (InfiltrationClientHandler.targetX - camPos.x).toFloat()
                val floorY = (InfiltrationClientHandler.exitFloorY + 0.018 - camPos.y).toFloat()
                val cz = (InfiltrationClientHandler.targetZ - camPos.z).toFloat()

                val fade = if (phase == InfiltrationPhase.EMERGING) {
                    (1.0f - InfiltrationClientHandler.getPhaseProgress(0.0f)).coerceIn(0.0f, 1.0f)
                } else {
                    1.0f
                }

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    // Disque sombre-émeraude au ras du sol donnant l'illusion d'un portail liquide
                    drawHorizontalDisc(
                        pose, consumer, cx, floorY, cz,
                        0.76f, 28,
                        25, 245, 195, (140 * fade).toInt(),
                        12, 110, 100, (15 * fade).toInt()
                    )
                    // Anneau principal net autour des épaules du joueur
                    drawRing(
                        pose, consumer, cx, floorY + 0.006f, cz,
                        0.60f, 0.72f, 32,
                        115, 255, 225, (225 * fade).toInt()
                    )
                    // 2 ondes concentriques qui s'étendent doucement sur le sol
                    for (i in 0 until 2) {
                        val waveT = ((timeSec * 0.75f + i * 0.5f) % 1.0f)
                        val rInner = 0.65f + waveT * 0.65f
                        val alpha = ((1.0f - waveT) * 155.0f * fade).toInt().coerceIn(0, 255)
                        drawRing(pose, consumer, cx, floorY + 0.004f, cz, rInner, rInner + 0.07f, 28, 52, 235, 185, alpha)
                    }
                    // Cadran rotatif subtil
                    drawRotatingArcs(pose, consumer, cx, floorY + 0.01f, cz, 0.80f, 0.86f, timeSec * 1.1f, 4, 150, 255, 235, (185 * fade).toInt())
                }
                poseStack.popPose()
            }

            else -> {}
        }
    }

    private fun renderValidCeilingTarget(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        ceilY: Float,
        cz: Float,
        footY: Float,
        scale: Float,
        timeSec: Float
    ) {
        val pulse = 0.88f + 0.12f * sin(timeSec * 5.0f)

        // 1. Halo radial doux plaqué sur la face inférieure du plafond
        drawHorizontalDisc(
            pose, consumer, cx, ceilY, cz,
            0.95f * scale, 32,
            110, 255, 225, (145 * scale * pulse).toInt(),
            30, 220, 165, 0
        )

        // 2. Anneau central net + 4 segments rotatifs façon Zonai / TOTK
        drawRing(pose, consumer, cx, ceilY - 0.006f, cz, 0.52f * scale, 0.62f * scale, 32, 95, 255, 220, (220 * scale).toInt())
        drawRotatingArcs(pose, consumer, cx, ceilY - 0.010f, cz, 0.74f * scale, 0.82f * scale, timeSec * 1.4f, 4, 140, 255, 240, (210 * scale).toInt())
        drawRotatingArcs(pose, consumer, cx, ceilY - 0.012f, cz, 0.30f * scale, 0.36f * scale, -timeSec * 2.0f, 3, 200, 255, 248, (200 * scale).toInt())

        // 3. Ondes concentriques qui rayonnent sur le plafond
        for (i in 0 until 2) {
            val wave = ((timeSec * 0.9f + i * 0.5f) % 1.0f)
            val rad = (0.35f + wave * 0.95f) * scale
            val alpha = ((1.0f - wave) * 165.0f * scale).toInt().coerceIn(0, 255)
            drawRing(pose, consumer, cx, ceilY - 0.004f, cz, rad, rad + 0.06f * scale, 28, 52, 240, 185, alpha)
        }

        // 4. Grille en croix fine au centre du cercle (repère de centrage exact)
        val crossLen = 0.45f * scale
        val crossHalfW = 0.025f * scale
        drawFlatQuad(pose, consumer, cx - crossLen, ceilY - 0.014f, cz - crossHalfW, cx + crossLen, cz + crossHalfW, 180, 255, 245, (180 * scale).toInt())
        drawFlatQuad(pose, consumer, cx - crossHalfW, ceilY - 0.014f, cz - crossLen, cx + crossHalfW, cz + crossLen, 180, 255, 245, (180 * scale).toInt())

        // 5. Faisceau vertical translucide + anneaux ascendants vers le plafond
        if (ceilY - footY > 1.2f) {
            drawEnergyColumn(pose, consumer, cx, footY + 0.3f, ceilY - 0.02f, cz, 0.42f * scale, timeSec, 0.45f * scale)
        }
    }

    private fun renderInvalidCeilingTarget(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        ceilY: Float,
        cz: Float,
        scale: Float,
        timeSec: Float,
        errorPulse: Boolean
    ) {
        val pulse = if (errorPulse) 1.0f else (0.72f + 0.28f * sin(timeSec * 6.5f))
        val s = scale * (if (errorPulse) 1.12f else 1.0f)

        // Disque rouge atténué + anneau extérieur
        drawHorizontalDisc(
            pose, consumer, cx, ceilY, cz,
            0.80f * s, 28,
            255, 55, 75, (105 * scale * pulse).toInt(),
            200, 20, 40, 0
        )
        drawRing(pose, consumer, cx, ceilY - 0.008f, cz, 0.56f * s, 0.66f * s, 28, 255, 65, 85, (215 * scale * pulse).toInt())

        // Grande croix diagonale "X" rouge au plafond
        drawRotatedBar(pose, consumer, cx, ceilY - 0.014f, cz, 0.46f * s, 0.06f * s, (PI / 4.0).toFloat(), 255, 90, 105, (220 * scale * pulse).toInt())
        drawRotatedBar(pose, consumer, cx, ceilY - 0.014f, cz, 0.46f * s, 0.06f * s, (-PI / 4.0).toFloat(), 255, 90, 105, (220 * scale * pulse).toInt())
    }

    private fun drawEnergyColumn(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        bottomY: Float,
        topY: Float,
        cz: Float,
        radius: Float,
        timeSec: Float,
        intensity: Float
    ) {
        val segments = 12
        val baseAlpha = (52.0f * intensity).toInt().coerceIn(0, 255)
        val topAlpha = (125.0f * intensity).toInt().coerceIn(0, 255)

        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (PI * 2.0).toFloat() + timeSec * 0.5f
            val a1 = ((i + 1).toFloat() / segments) * (PI * 2.0).toFloat() + timeSec * 0.5f

            val x0 = cx + cos(a0) * radius
            val z0 = cz + sin(a0) * radius
            val x1 = cx + cos(a1) * radius
            val z1 = cz + sin(a1) * radius

            val topR = radius * 1.12f
            val tx0 = cx + cos(a0) * topR
            val tz0 = cz + sin(a0) * topR
            val tx1 = cx + cos(a1) * topR
            val tz1 = cz + sin(a1) * topR

            consumer.addVertex(pose, x0, bottomY, z0).setColor(42, 235, 185, baseAlpha)
            consumer.addVertex(pose, x1, bottomY, z1).setColor(42, 235, 185, baseAlpha)
            consumer.addVertex(pose, tx1, topY, tz1).setColor(115, 255, 235, topAlpha)
            consumer.addVertex(pose, tx0, topY, tz0).setColor(115, 255, 235, topAlpha)
        }

        // 3 anneaux d'énergie qui montent le long de la colonne
        val height = (topY - bottomY).coerceAtLeast(0.5f)
        for (k in 0 until 3) {
            val t = ((timeSec * 0.85f + k / 3.0f) % 1.0f)
            val ringY = bottomY + t * height
            val edgeFade = sin(t * PI).toFloat()
            val alpha = (edgeFade * 155.0f * intensity).toInt().coerceIn(0, 255)
            val r = radius * (1.0f + t * 0.14f)
            drawRing(pose, consumer, cx, ringY, cz, r, r + 0.05f, 20, 120, 255, 235, alpha)
        }
    }

    private fun drawHorizontalDisc(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        y: Float,
        cz: Float,
        radius: Float,
        segments: Int,
        cr: Int, cg: Int, cb: Int, ca: Int,
        er: Int, eg: Int, eb: Int, ea: Int
    ) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (PI * 2.0).toFloat()
            val a1 = ((i + 1).toFloat() / segments) * (PI * 2.0).toFloat()

            val x0 = cx + cos(a0) * radius
            val z0 = cz + sin(a0) * radius
            val x1 = cx + cos(a1) * radius
            val z1 = cz + sin(a1) * radius

            // Quad dégénéré en triangle (centre dupliqué) pour compatibilité QUADS
            consumer.addVertex(pose, cx, y, cz).setColor(cr, cg, cb, ca)
            consumer.addVertex(pose, cx, y, cz).setColor(cr, cg, cb, ca)
            consumer.addVertex(pose, x1, y, z1).setColor(er, eg, eb, ea)
            consumer.addVertex(pose, x0, y, z0).setColor(er, eg, eb, ea)
        }
    }

    private fun drawRing(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        y: Float,
        cz: Float,
        innerR: Float,
        outerR: Float,
        segments: Int,
        r: Int, g: Int, b: Int, a: Int
    ) {
        if (a <= 2) return
        val inR = innerR.coerceAtLeast(0.01f)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (PI * 2.0).toFloat()
            val a1 = ((i + 1).toFloat() / segments) * (PI * 2.0).toFloat()

            val c0 = cos(a0)
            val s0 = sin(a0)
            val c1 = cos(a1)
            val s1 = sin(a1)

            consumer.addVertex(pose, cx + c0 * inR, y, cz + s0 * inR).setColor(r, g, b, a)
            consumer.addVertex(pose, cx + c0 * outerR, y, cz + s0 * outerR).setColor(r, g, b, a)
            consumer.addVertex(pose, cx + c1 * outerR, y, cz + s1 * outerR).setColor(r, g, b, a)
            consumer.addVertex(pose, cx + c1 * inR, y, cz + s1 * inR).setColor(r, g, b, a)
        }
    }

    private fun drawRotatingArcs(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        y: Float,
        cz: Float,
        innerR: Float,
        outerR: Float,
        startAngle: Float,
        arcCount: Int,
        r: Int, g: Int, b: Int, a: Int
    ) {
        if (a <= 2) return
        val sector = (PI * 2.0).toFloat() / arcCount
        val arcSpan = sector * 0.52f
        val subSteps = 6
        for (k in 0 until arcCount) {
            val baseA = startAngle + k * sector
            for (s in 0 until subSteps) {
                val a0 = baseA + (s.toFloat() / subSteps) * arcSpan
                val a1 = baseA + ((s + 1).toFloat() / subSteps) * arcSpan

                val c0 = cos(a0)
                val s0 = sin(a0)
                val c1 = cos(a1)
                val s1 = sin(a1)

                consumer.addVertex(pose, cx + c0 * innerR, y, cz + s0 * innerR).setColor(r, g, b, a)
                consumer.addVertex(pose, cx + c0 * outerR, y, cz + s0 * outerR).setColor(r, g, b, a)
                consumer.addVertex(pose, cx + c1 * outerR, y, cz + s1 * outerR).setColor(r, g, b, a)
                consumer.addVertex(pose, cx + c1 * innerR, y, cz + s1 * innerR).setColor(r, g, b, a)
            }
        }
    }

    private fun drawFlatQuad(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        minX: Float, y: Float, minZ: Float,
        maxX: Float, maxZ: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        consumer.addVertex(pose, minX, y, minZ).setColor(r, g, b, a)
        consumer.addVertex(pose, minX, y, maxZ).setColor(r, g, b, a)
        consumer.addVertex(pose, maxX, y, maxZ).setColor(r, g, b, a)
        consumer.addVertex(pose, maxX, y, minZ).setColor(r, g, b, a)
    }

    private fun drawRotatedBar(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float, y: Float, cz: Float,
        halfLen: Float, halfWidth: Float,
        angle: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val ca = cos(angle)
        val sa = sin(angle)
        val dxL = ca * halfLen
        val dzL = sa * halfLen
        val dxW = -sa * halfWidth
        val dzW = ca * halfWidth

        consumer.addVertex(pose, cx - dxL - dxW, y, cz - dzL - dzW).setColor(r, g, b, a)
        consumer.addVertex(pose, cx - dxL + dxW, y, cz - dzL + dzW).setColor(r, g, b, a)
        consumer.addVertex(pose, cx + dxL + dxW, y, cz + dzL + dzW).setColor(r, g, b, a)
        consumer.addVertex(pose, cx + dxL - dxW, y, cz + dzL - dzW).setColor(r, g, b, a)
    }
}
