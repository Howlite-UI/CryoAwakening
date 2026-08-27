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
        val WIND_PROCESS_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_gui_wind_process.png")
    }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
        inventoryLabelY = 73
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = leftPos
        val y = topPos

        // 1. Fond principal de l'interface
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256)

        // 2. Jauge de Vent (16x52 px à x+14, y+17)
        val fillRatio = menu.getWindRatio()
        val fillHeight = (fillRatio * 52.0f).toInt().coerceIn(0, 52)
        if (fillHeight > 0) {
            val gaugeY = y + 17 + (52 - fillHeight)
            val vOffset = (52 - fillHeight).toFloat()
            graphics.blit(RenderPipelines.GUI_TEXTURED, GAUGE_TEXTURE, x + 14, gaugeY, 0.0f, vOffset, 16, fillHeight, 16, 52)
        }

        // 3. Barre de chargement unique du processus de fusion (Tornade à x+86, y+37, 16x16 px)
        if (menu.isLit) {
            val cookRatio = (menu.cookTime.toFloat() / maxOf(1, menu.cookTimeTotal).toFloat()).coerceIn(0.0f, 1.0f)
            val procHeight = (cookRatio * 16.0f).toInt().coerceIn(0, 16)
            if (procHeight > 0) {
                val procY = y + 37 + (16 - procHeight)
                val vOffset = (16 - procHeight).toFloat()
                graphics.blit(RenderPipelines.GUI_TEXTURED, WIND_PROCESS_TEXTURE, x + 86, procY, 0.0f, vOffset, 16, procHeight, 16, 16)
            }
        }

        // 4. Rendu des slots, textes d'inventaire et curseur
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    override fun extractTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        super.extractTooltip(graphics, mouseX, mouseY)

        // Tooltip personnalisé sur la jauge de Vent (x: 13..30, y: 16..69)
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
