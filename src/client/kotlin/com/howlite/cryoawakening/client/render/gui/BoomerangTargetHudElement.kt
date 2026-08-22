package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.client.event.BoomerangClientTargetHandler
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.tan

/**
 * BoomerangTargetHudElement
 *
 * Rendu HUD des indicateurs de verrouillage style Zelda: Twilight Princess.
 * Projette les coordonnées 3D de chaque mob marqué sur l'écran en 2D
 * pour afficher son réticule doré numéroté (1 à 5) directement sur lui.
 */
object BoomerangTargetHudElement : HudElement {

    private val TARGET_RETICLE_TEXTURE: Identifier =
        CryoAwakening.id("textures/gui/target_reticle.png")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return
        if (client.gui.hud.isHidden) return

        if (!BoomerangClientTargetHandler.isCharging()) return
        val targetIds = BoomerangClientTargetHandler.markedTargetIds
        if (targetIds.isEmpty()) return

        val screenWidth = graphics.guiWidth()
        val screenHeight = graphics.guiHeight()
        val camera = client.gameRenderer.mainCamera()
        val camPos = camera.position()
        val camRot = camera.rotation()
        val forward = Vector3f(0.0f, 0.0f, -1.0f).rotate(camRot)

        val partialTick = deltaTracker.getGameTimeDeltaPartialTick(false)

        graphics.nextStratum()

        for (i in targetIds.indices) {
            val id = targetIds[i]
            val entity = level.getEntity(id) ?: continue

            val ex = entity.xOld + (entity.x - entity.xOld) * partialTick
            val ey = entity.yOld + (entity.y - entity.yOld) * partialTick + (entity.bbHeight * 0.55)
            val ez = entity.zOld + (entity.z - entity.zOld) * partialTick
            val targetPos = net.minecraft.world.phys.Vec3(ex, ey, ez)

            val rel = targetPos.subtract(camPos)
            val dotForward = (forward.x * rel.x + forward.y * rel.y + forward.z * rel.z).toFloat()

            // Devant la caméra uniquement
            if (dotForward > 0.3f) {
                // Projection perspective exacte officielle de Minecraft
                val proj = client.gameRenderer.projectPointToScreen(targetPos)

                val screenCenterX = ((proj.x + 1.0) * 0.5 * screenWidth).toInt()
                val screenCenterY = ((1.0 - proj.y) * 0.5 * screenHeight).toInt()

                if (screenCenterX in -30..(screenWidth + 30) && screenCenterY in -30..(screenHeight + 30)) {
                    // Grille 4x4 de 16x16 pixels pour les 16 cibles possibles (1 à 16)
                    val targetIndex = i.coerceIn(0, 15)
                    val col = targetIndex % 4
                    val row = targetIndex / 4
                    val u = (col * 16).toFloat()
                    val v = (row * 16).toFloat()

                    val angleRad = (System.currentTimeMillis() % 2000L) * (Math.PI.toFloat() * 2.0f / 2000.0f)

                    val pose = graphics.pose()
                    val prevPose = org.joml.Matrix3x2f(pose)

                    pose.translate(screenCenterX.toFloat(), screenCenterY.toFloat())
                    pose.rotate(angleRad)
                    pose.translate(-8.0f, -8.0f)

                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TARGET_RETICLE_TEXTURE,
                        0,
                        0,
                        u,
                        v,
                        16,
                        16,
                        64,
                        64
                    )
                    pose.set(prevPose)
                }
            }
        }
    }
}
