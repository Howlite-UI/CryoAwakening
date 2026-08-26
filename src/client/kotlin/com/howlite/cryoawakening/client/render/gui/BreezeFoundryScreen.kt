package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.screen.BreezeFoundryMenu
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/**
 * Interface graphique (Screen) pour la Breeze Foundry.
 */
class BreezeFoundryScreen(
    menu: BreezeFoundryMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<BreezeFoundryMenu>(menu, playerInventory, title) {

    companion object {
        val GUI_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_gui.png")
        val GAUGE_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_bar_gauge.png")
    }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
        inventoryLabelY = 72
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = leftPos
        val y = topPos

        // 1. Fond principal de l'interface
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256)

        // 2. Jauge de Vent (16x50 px à x+14, y+17)
        val fillRatio = menu.getWindRatio()
        val fillHeight = (fillRatio * 50.0f).toInt().coerceIn(0, 50)
        if (fillHeight > 0) {
            val gaugeY = y + 17 + (50 - fillHeight)
            val vOffset = (50 - fillHeight).toFloat()
            graphics.blit(RenderPipelines.GUI_TEXTURED, GAUGE_TEXTURE, x + 14, gaugeY, 0.0f, vOffset, 16, fillHeight, 16, 50)
        }

        // 3. Barre de progression de fusion (Flèche à x+79, y+35, 24x16 px)
        val cookProgress = menu.getCookProgress(24)
        if (cookProgress > 0) {
            // Effet d'énergie vent cyan lumineux progressif
            graphics.fill(RenderPipelines.GUI, x + 79, y + 35, x + 79 + cookProgress, y + 49, 0x8800E5FF.toInt())
            graphics.fill(RenderPipelines.GUI, x + 79 + cookProgress - 1, y + 35, x + 79 + cookProgress, y + 49, 0xFFFFFFFF.toInt())
        }

        // 4. Indicateur d'activité du vent (x+57, y+37)
        if (menu.isLit) {
            val pulse = ((System.currentTimeMillis() % 1000L) / 1000.0f)
            val alpha = (0.35f + 0.35f * kotlin.math.sin(pulse * Math.PI * 2.0).toFloat()).coerceIn(0.0f, 1.0f)
            val alphaHex = (alpha * 255).toInt() shl 24
            graphics.fill(RenderPipelines.GUI, x + 57, y + 38, x + 69, y + 50, alphaHex or 0x00E5FF)
        }

        // 5. Rendu des slots, textes d'inventaire et curseur
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    override fun extractTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        super.extractTooltip(graphics, mouseX, mouseY)

        // Tooltip personnalisé sur la jauge de Vent
        if (isHovering(13, 16, 18, 54, mouseX.toDouble(), mouseY.toDouble())) {
            val text = Component.translatable(
                "gui.cryo-awakening.breeze_foundry.wind",
                "%,d".format(menu.wind),
                "%,d".format(menu.windCapacity)
            )
            graphics.setTooltipForNextFrame(font, text, mouseX, mouseY)
        }
    }
}
