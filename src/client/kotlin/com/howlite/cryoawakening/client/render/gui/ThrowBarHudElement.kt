package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.client.event.GawkerClientCarryHandler
import com.howlite.cryoawakening.item.ModItems
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

/**
 * ThrowBarHudElement
 *
 * Affiche la jauge graphique 3x11 de charge de lancer (throw_bar.png)
 * directement au-dessus du réticule (crosshair) du joueur lors de la préparation
 * d'un lancer de Gawker (porté) ou d'une Gawk-Bomb (en main).
 */
object ThrowBarHudElement : HudElement {

    private val THROW_BAR_TEXTURE: Identifier =
        CryoAwakening.id("textures/gui/throw_bar.png")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.gui.hud.isHidden) return

        // Progression de charge : portage Gawker, item Gawk-Bomb en main ou Gale Boomerang
        val progress: Float = if (GawkerClientCarryHandler.isCharging()) {
            GawkerClientCarryHandler.getChargeProgress()
        } else if (com.howlite.cryoawakening.client.event.BoomerangClientTargetHandler.isCharging()) {
            com.howlite.cryoawakening.client.event.BoomerangClientTargetHandler.getChargeProgress()
        } else if (player.isUsingItem && player.useItem.`is`(ModItems.GAWK_BOMB)) {
            val useTicks = player.ticksUsingItem
            (useTicks.toFloat() / 25.0f).coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }

        if (progress <= 0.0f) return

        val screenWidth = graphics.guiWidth()
        val screenHeight = graphics.guiHeight()

        // Centrage horizontal et positionnement juste au-dessus du crosshair
        val barWidth = 3
        val barHeight = 11
        val x = (screenWidth - barWidth) / 2
        val y = (screenHeight / 2) - 18

        graphics.nextStratum()

        // 1. Dessin du fond (non chargé) : x=0..2, y=0..10
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            THROW_BAR_TEXTURE,
            x,
            y,
            0.0f,
            0.0f,
            barWidth,
            barHeight,
            16,
            16
        )

        // 2. Dessin du remplissage chargé (remplit de bas en haut) : x=5..7, y=0..10
        val fillHeight = (barHeight * progress).toInt().coerceIn(0, barHeight)
        if (fillHeight > 0) {
            val filledY = y + (barHeight - fillHeight)
            val vOffset = (barHeight - fillHeight).toFloat()
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                THROW_BAR_TEXTURE,
                x,
                filledY,
                5.0f,
                vOffset,
                barWidth,
                fillHeight,
                16,
                16
            )
        }
    }
}
