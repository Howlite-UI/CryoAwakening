package com.howlite.cryoawakening.client.compat.jei

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.types.IRecipeType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * Catégorie JEI pour les recettes de fusion d'alliage de la Breeze Foundry.
 */
class BreezeFoundryRecipeCategory(private val guiHelper: IGuiHelper) : IRecipeCategory<BreezeFoundryJeiRecipe> {

    companion object {
        val RECIPE_TYPE: IRecipeType<BreezeFoundryJeiRecipe> =
            IRecipeType.create(CryoAwakening.id("breeze_foundry"), BreezeFoundryJeiRecipe::class.java)

        val GUI_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_gui.png")
        val GAUGE_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_bar_gauge.png")
        val WIND_PROCESS_TEXTURE: Identifier = CryoAwakening.id("textures/gui/breeze_foundry_gui_wind_process.png")
    }

    private val background: IDrawable = guiHelper.drawableBuilder(GUI_TEXTURE, 10, 15, 138, 56)
        .setTextureSize(256, 256)
        .build()

    private val icon: IDrawable = guiHelper.createDrawableItemStack(ItemStack(ModBlocks.BREEZE_FOUNDRY))

    override fun getRecipeType(): IRecipeType<BreezeFoundryJeiRecipe> = RECIPE_TYPE

    override fun getTitle(): Component = Component.translatable("block.cryo-awakening.breeze_foundry")

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = 138

    override fun getHeight(): Int = 56

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: BreezeFoundryJeiRecipe, focuses: IFocusGroup) {
        // Slot Entrée Haute (x = 47, y = 9)
        builder.addSlot(RecipeIngredientRole.INPUT, 47, 9)
            .addItemStacks(recipe.inputA)

        // Slot Entrée Basse (x = 47, y = 31)
        builder.addSlot(RecipeIngredientRole.INPUT, 47, 31)
            .addItemStacks(recipe.inputB)

        // Slot Sortie (x = 106, y = 20)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 20)
            .add(recipe.output)
    }

    override fun draw(
        recipe: BreezeFoundryJeiRecipe,
        recipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double
    ) {
        // 0. Fond de l'interface
        background.draw(graphics, 0, 0)

        // 1. Rendu de la jauge de Vent remplie
        graphics.blit(RenderPipelines.GUI_TEXTURED, GAUGE_TEXTURE, 4, 2, 0.0f, 0.0f, 16, 52, 16, 52)

        // 2. Rendu de la tornade animée au centre
        val tick = (System.currentTimeMillis() / 50L) % 100L
        val ratio = (tick.toFloat() / 100.0f).coerceIn(0.0f, 1.0f)
        val procHeight = (ratio * 16.0f).toInt().coerceIn(0, 16)
        if (procHeight > 0) {
            val procY = 22 + (16 - procHeight)
            val vOffset = (16 - procHeight).toFloat()
            graphics.blit(RenderPipelines.GUI_TEXTURED, WIND_PROCESS_TEXTURE, 76, procY, 0.0f, vOffset, 16, procHeight, 16, 16)
        }
    }

    override fun getTooltip(
        tooltip: mezz.jei.api.gui.builder.ITooltipBuilder,
        recipe: BreezeFoundryJeiRecipe,
        recipeSlotsView: IRecipeSlotsView,
        mouseX: Double,
        mouseY: Double
    ) {
        // Survol de la jauge de Vent (x: 4..20, y: 2..54)
        if (mouseX in 3.0..21.0 && mouseY in 1.0..55.0) {
            tooltip.add(Component.translatable("gui.cryo-awakening.breeze_foundry.wind_cost", recipe.windCost))
        } else if (mouseX in 74.0..94.0 && mouseY in 20.0..40.0) {
            val seconds = recipe.cookTimeTicks / 20.0f
            tooltip.add(Component.translatable("gui.cryo-awakening.breeze_foundry.craft_time", "%.1f".format(seconds)))
        }
    }
}
