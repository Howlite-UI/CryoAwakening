package com.howlite.cryoawakening.client.compat.jei

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.client.render.gui.BreezeFoundryScreen
import com.howlite.cryoawakening.recipe.BreezeFoundryRecipes
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IGuiHandlerRegistration
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.resources.Identifier

/**
 * Plugin JEI pour Cryo Awakening.
 * Enregistre la catégorie, toutes les recettes dynamiques, le catalyseur et les zones de clic de la Breeze Foundry.
 */
@JeiPlugin
class CryoAwakeningJeiPlugin : IModPlugin {

    override fun getPluginUid(): Identifier = CryoAwakening.id("jei_plugin")

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(
            BreezeFoundryRecipeCategory(registration.jeiHelpers.guiHelper)
        )
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val recipes = BreezeFoundryRecipes.RECIPES.map {
            BreezeFoundryJeiRecipe(
                inputA = it.ingredientAList,
                inputB = it.ingredientBList,
                output = it.output,
                windCost = it.windCost,
                cookTimeTicks = it.cookTimeTicks
            )
        }
        registration.addRecipes(BreezeFoundryRecipeCategory.RECIPE_TYPE, recipes)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(
            ModBlocks.BREEZE_FOUNDRY,
            BreezeFoundryRecipeCategory.RECIPE_TYPE
        )
    }

    override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
        registration.addRecipeClickArea(
            BreezeFoundryScreen::class.java,
            84, 35, 20, 20,
            BreezeFoundryRecipeCategory.RECIPE_TYPE
        )
    }
}
