package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.item.ModItems
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.min

/**
 * KaleidoscopeHudElement
 *
 * Affiche le viseur circulaire officiel (Spyglass Scope) de Minecraft lors de l'utilisation du Kaléidoscope.
 */
object KaleidoscopeHudElement : HudElement {

    private val SPYGLASS_SCOPE_TEXTURE: Identifier =
        Identifier.fromNamespaceAndPath("minecraft", "textures/misc/spyglass_scope.png")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.gui.hud.isHidden) return

        // Ne s'affiche que si le joueur utilise activement le Kaléidoscope en vue subjective
        val isUsingKaleidoscope = player.isUsingItem && player.useItem.`is`(ModItems.KALEIDOSCOPE)
        if (!isUsingKaleidoscope) return
        if (!client.options.cameraType.isFirstPerson) return

        val screenWidth = graphics.guiWidth()
        val screenHeight = graphics.guiHeight()

        val minDimension = min(screenWidth, screenHeight).toFloat()
        val scopeWidth = minDimension.toInt()
        val scopeHeight = minDimension.toInt()

        val x0 = (screenWidth - scopeWidth) / 2
        val y0 = (screenHeight - scopeHeight) / 2
        val x1 = x0 + scopeWidth
        val y1 = y0 + scopeHeight

        graphics.nextStratum()

        // 1. Dessin de la texture de viseur Spyglass de base
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SPYGLASS_SCOPE_TEXTURE,
            x0,
            y0,
            0.0f,
            0.0f,
            scopeWidth,
            scopeHeight,
            scopeWidth,
            scopeHeight
        )

        // 2. Remplissage noir des bandes extérieures (haut, bas, gauche, droite)
        val black = 0xFF000000.toInt()
        if (y0 > 0) {
            graphics.fill(RenderPipelines.GUI, 0, 0, screenWidth, y0, black)
        }
        if (y1 < screenHeight) {
            graphics.fill(RenderPipelines.GUI, 0, y1, screenWidth, screenHeight, black)
        }
        if (x0 > 0) {
            graphics.fill(RenderPipelines.GUI, 0, y0, x0, y1, black)
        }
        if (x1 < screenWidth) {
            graphics.fill(RenderPipelines.GUI, x1, y0, screenWidth, y1, black)
        }
    }
}
