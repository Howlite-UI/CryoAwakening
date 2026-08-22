package com.howlite.cryoawakening.recipe

import com.howlite.cryoawakening.CryoAwakening
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.crafting.RecipeSerializer

object ModRecipes {

    val GAWK_BOMB_CHARGE_SERIALIZER: RecipeSerializer<GawkBombChargeRecipe> =
        GawkBombChargeRecipe.SERIALIZER

    fun register() {
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            CryoAwakening.id("crafting_special_gawk_bomb_charge"),
            GAWK_BOMB_CHARGE_SERIALIZER
        )
    }
}
