package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.client.event.ValueSettingsClientHandler
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * ValueSettingsHudElement
 *
 * Affichage tête haute (HUD) reproduisant fidèlement l'interface "Value Settings" du mod Create :
 * - S'affiche UNIQUEMENT lors du maintien du Clic-Droit sur le bloc.
 * - Utilise la texture value_settings.png :
 *   • Rail de guidage (79x8 à UV: 0, 0)
 *   • Boîtier en laiton élargi (23x14 à UV: 0, 16)
 *   • Icône V/t (3x7 à UV: 0, 32)
 *   • Valeur numérique gravée sombre à côté de l'icône V/t
 */
object ValueSettingsHudElement : HudElement {

    private val VALUE_SETTINGS_TEXTURE: Identifier = CryoAwakening.id("textures/gui/value_settings.png")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.gui.hud.isHidden) return
        if (client.gui.screen() != null) return

        // Affichage uniquement lorsque le joueur maintient activement le Clic-Droit sur le bloc
        if (!ValueSettingsClientHandler.isHolding) return

        val value = ValueSettingsClientHandler.currentValue

        val screenWidth = graphics.guiWidth()
        val screenHeight = graphics.guiHeight()

        graphics.nextStratum()

        // 1. Positionnement du rail de guidage Create (Track: 79x8) au centre de l'écran
        val trackW = 79
        val trackH = 8
        val trackX = screenWidth / 2 - trackW / 2
        val trackY = screenHeight / 2 - trackH / 2

        // Rendu du rail
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            VALUE_SETTINGS_TEXTURE,
            trackX,
            trackY,
            0.0f,
            0.0f,
            trackW,
            trackH,
            256,
            256
        )

        // 2. Positionnement du boîtier curseur en laiton (Box: 23x14)
        // Course horizontale : de trackX (value=0) à trackX + 79 - 23 = trackX + 56 (value=50)
        val boxW = 23
        val boxH = 14
        val minX = trackX
        val maxX = trackX + trackW - boxW
        val boxX = minX + ((value.toFloat() / 50.0f) * (maxX - minX)).toInt()
        val boxY = trackY - 3 // Aligne l'axe du boîtier sur l'axe du rail

        // Rendu du boîtier en laiton
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            VALUE_SETTINGS_TEXTURE,
            boxX,
            boxY,
            0.0f,
            16.0f,
            boxW,
            boxH,
            256,
            256
        )

        // 3. Rendu de l'icône V/t (3x7 à UV: 0, 32) sur la droite à l'intérieur du boîtier
        val vtX = boxX + 17
        val vtY = boxY + 3
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            VALUE_SETTINGS_TEXTURE,
            vtX,
            vtY,
            0.0f,
            32.0f,
            3,
            7,
            256,
            256
        )

        // 4. Rendu du chiffre à gauche de l'icône V/t (gravure Create sombre)
        val text = "$value"
        val font = client.font
        val textW = font.width(text)
        val numAreaLeft = boxX + 2
        val numAreaWidth = vtX - numAreaLeft
        val textX = numAreaLeft + (numAreaWidth - textW) / 2
        val textY = boxY + 3
        val textColor = 0xFF241812.toInt() // Marron foncé/noir Create

        graphics.text(font, Component.literal(text), textX, textY, textColor, false)
    }
}
