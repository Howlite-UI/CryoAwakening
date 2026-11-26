package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import kotlin.math.sin

/**
 * InfiltrationHudElement
 *
 * Overlay visuel minimaliste sans texte ni barre intrusive :
 * - Flash lumineux doux lors des transitions (entrée dans le plafond, sortie à la surface, retour).
 * - Coins lumineux discrets uniquement en mode visée (vert si plafond valide, rouge sinon).
 */
object InfiltrationHudElement : HudElement {

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        if (client.player == null || client.gui.hud.isHidden) return

        val partialTick = deltaTracker.getGameTimeDeltaPartialTick(false)
        val phase = InfiltrationClientHandler.phase
        val aimAnim = InfiltrationClientHandler.getAimAnim(partialTick)
        val flashAlpha = InfiltrationClientHandler.getFlashAlpha(partialTick)

        if (phase == InfiltrationPhase.IDLE && aimAnim <= 0.01f && flashAlpha <= 0.01f) return

        val screenW = graphics.guiWidth()
        val screenH = graphics.guiHeight()
        val time = (System.currentTimeMillis() % 100000L) / 1000.0f

        graphics.nextStratum()

        // 1. Flash lumineux fluide lors des transitions
        if (flashAlpha > 0.01f) {
            val rgb = InfiltrationClientHandler.flashColorRgb
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            val a = (flashAlpha * 165.0f).toInt().coerceIn(0, 255)
            graphics.fill(RenderPipelines.GUI, 0, 0, screenW, screenH, ARGB.color(a, r, g, b))
        }

        // 2. Coins discrets en mode AIMING (repère purement visuel, zéro texte)
        if (phase == InfiltrationPhase.AIMING || aimAnim > 0.05f) {
            val target = InfiltrationClientHandler.currentTarget
            val isValid = target?.valid == true
            val isErrorPulse = InfiltrationClientHandler.invalidFlashTicks > 0

            val accentR = if (isValid) 46 else 255
            val accentG = if (isValid) 235 else if (isErrorPulse) 45 else 78
            val accentB = if (isValid) 175 else if (isErrorPulse) 60 else 95

            val pulse = 0.78f + 0.22f * sin(time * 5.0f)
            val cornerAlpha = (aimAnim * 185.0f * pulse).toInt().coerceIn(0, 255)
            val cornerColor = ARGB.color(cornerAlpha, accentR, accentG, accentB)

            val margin = 16
            val len = 22
            val thick = 2
            // Haut-gauche
            graphics.fill(RenderPipelines.GUI, margin, margin, margin + len, margin + thick, cornerColor)
            graphics.fill(RenderPipelines.GUI, margin, margin, margin + thick, margin + len, cornerColor)
            // Haut-droite
            graphics.fill(RenderPipelines.GUI, screenW - margin - len, margin, screenW - margin, margin + thick, cornerColor)
            graphics.fill(RenderPipelines.GUI, screenW - margin - thick, margin, screenW - margin, margin + len, cornerColor)
            // Bas-gauche
            graphics.fill(RenderPipelines.GUI, margin, screenH - margin - thick, margin + len, screenH - margin, cornerColor)
            graphics.fill(RenderPipelines.GUI, margin, screenH - margin - len, margin + thick, screenH - margin, cornerColor)
            // Bas-droite
            graphics.fill(RenderPipelines.GUI, screenW - margin - len, screenH - margin - thick, screenW - margin, screenH - margin, cornerColor)
            graphics.fill(RenderPipelines.GUI, screenW - margin - thick, screenH - margin - len, screenW - margin, screenH - margin, cornerColor)
        }
    }
}
