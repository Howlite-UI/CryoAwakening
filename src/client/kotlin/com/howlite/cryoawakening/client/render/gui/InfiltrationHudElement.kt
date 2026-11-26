package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler
import com.howlite.cryoawakening.infiltration.InfiltrationInvalidReason
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * InfiltrationHudElement
 *
 * Interface HUD épurée inspirée de Zelda: Tears of the Kingdom pour la Perle d'Infiltration :
 * - En mode AIMING : cadre discret aux coins (émeraude si valide, rouge si invalide),
 *   indication de la hauteur du plafond / épaisseur à traverser et rappel des clics.
 * - En phase SWIMMING_THROUGH / RETURNING : jauge verticale de progression + flash lumineux de transition.
 * - En phase PEEKING : rappel clair en bas d'écran ([Clic Droit] Sortir • [Clic Gauche] Annuler).
 */
object InfiltrationHudElement : HudElement {

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.gui.hud.isHidden) return

        val partialTick = deltaTracker.getGameTimeDeltaPartialTick(false)
        val phase = InfiltrationClientHandler.phase
        val aimAnim = InfiltrationClientHandler.getAimAnim(partialTick)
        val flashAlpha = InfiltrationClientHandler.getFlashAlpha(partialTick)

        if (phase == InfiltrationPhase.IDLE && aimAnim <= 0.01f && flashAlpha <= 0.01f) return

        val screenW = graphics.guiWidth()
        val screenH = graphics.guiHeight()
        val font = client.font
        val time = (System.currentTimeMillis() % 100000L) / 1000.0f

        graphics.nextStratum()

        // 1. Flash lumineux fluide lors du percement du plafond ou de l'émergence
        if (flashAlpha > 0.01f) {
            val rgb = InfiltrationClientHandler.flashColorRgb
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            val a = (flashAlpha * 165.0f).toInt().coerceIn(0, 255)
            graphics.fill(RenderPipelines.GUI, 0, 0, screenW, screenH, ARGB.color(a, r, g, b))
        }

        // 2. Mode AIMING : coins lumineux + statut du plafond + contrôles
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

            // Cadre subtil aux 4 coins (look pouvoir Zonai / Cryo)
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

            if (phase == InfiltrationPhase.AIMING) {
                val centerX = screenW / 2
                val baseY = screenH - 64

                val statusText = if (target != null && target.valid) {
                    val ceilingDist = (target.ceilingHitY - player.eyeY).coerceAtLeast(0.0).roundToInt()
                    val thickness = target.thickness.roundToInt().coerceAtLeast(1)
                    Component.translatable("hud.cryo-awakening.infiltration.ready", ceilingDist, thickness)
                } else {
                    val key = when (target?.reason) {
                        InfiltrationInvalidReason.NO_CEILING -> "hud.cryo-awakening.infiltration.no_ceiling"
                        InfiltrationInvalidReason.UNBREAKABLE_BLOCK -> "hud.cryo-awakening.infiltration.blocked"
                        InfiltrationInvalidReason.NO_SURFACE_ABOVE,
                        InfiltrationInvalidReason.OUT_OF_WORLD -> "hud.cryo-awakening.infiltration.no_exit"
                        else -> "hud.cryo-awakening.infiltration.no_ceiling"
                    }
                    Component.translatable(key)
                }

                val controlsText = if (isValid) {
                    Component.translatable("hud.cryo-awakening.infiltration.aim_controls")
                } else {
                    Component.translatable("hud.cryo-awakening.infiltration.aim_cancel_only")
                }

                drawPromptPill(
                    graphics = graphics,
                    font = font,
                    centerX = centerX,
                    y = baseY,
                    line1 = statusText,
                    line2 = controlsText,
                    borderColor = ARGB.color((aimAnim * 210.0f).toInt().coerceIn(0, 255), accentR, accentG, accentB),
                    line1Color = ARGB.color(255, accentR, accentG, accentB)
                )
            }
        }

        // 3. Phase SWIMMING_THROUGH / RETURNING : barre de profondeur verticale discrète
        if (phase == InfiltrationPhase.SWIMMING_THROUGH || phase == InfiltrationPhase.RETURNING) {
            val progress = InfiltrationClientHandler.getPhaseProgress(partialTick)
            val fillRatio = if (phase == InfiltrationPhase.SWIMMING_THROUGH) progress else (1.0f - progress)

            val barH = 92
            val barW = 4
            val barX = screenW - 24
            val barY = (screenH - barH) / 2

            graphics.fill(RenderPipelines.GUI, barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0x88071A1C.toInt())
            val filledH = (barH * fillRatio).toInt().coerceIn(0, barH)
            val barColor = if (phase == InfiltrationPhase.SWIMMING_THROUGH) 0xFF3EF7BE.toInt() else 0xFF66C2FF.toInt()
            graphics.fill(RenderPipelines.GUI, barX, barY + (barH - filledH), barX + barW, barY + barH, barColor)
            // Point lumineux représentant le joueur dans la roche
            val dotY = barY + (barH - filledH)
            graphics.fill(RenderPipelines.GUI, barX - 2, dotY - 2, barX + barW + 2, dotY + 2, 0xFFFFFFFF.toInt())
        }

        // 4. Phase PEEKING : Invite TOTK "Sortir / Annuler" en bas d'écran
        if (phase == InfiltrationPhase.PEEKING) {
            val pulse = 0.8f + 0.2f * sin(time * 4.5f)
            val border = ARGB.color((215.0f * pulse).toInt().coerceIn(0, 255), 55, 240, 185)
            drawPromptPill(
                graphics = graphics,
                font = font,
                centerX = screenW / 2,
                y = screenH - 64,
                line1 = Component.translatable("hud.cryo-awakening.infiltration.peek_title"),
                line2 = Component.translatable("hud.cryo-awakening.infiltration.peek_controls"),
                borderColor = border,
                line1Color = 0xFF5CFFD0.toInt()
            )
        }
    }

    private fun drawPromptPill(
        graphics: GuiGraphicsExtractor,
        font: net.minecraft.client.gui.Font,
        centerX: Int,
        y: Int,
        line1: Component,
        line2: Component,
        borderColor: Int,
        line1Color: Int
    ) {
        val w1 = font.width(line1)
        val w2 = font.width(line2)
        val boxW = maxOf(w1, w2) + 22
        val boxH = 29
        val x0 = centerX - boxW / 2
        val y0 = y

        graphics.fill(RenderPipelines.GUI, x0, y0, x0 + boxW, y0 + boxH, 0xB808141A.toInt())
        graphics.outline(x0, y0, boxW, boxH, borderColor)

        graphics.centeredText(font, line1, centerX, y0 + 5, line1Color)
        graphics.centeredText(font, line2, centerX, y0 + 16, 0xFFE6F7FF.toInt())
    }
}
