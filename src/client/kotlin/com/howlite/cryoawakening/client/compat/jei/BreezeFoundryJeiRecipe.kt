package com.howlite.cryoawakening.client.compat.jei

import net.minecraft.world.item.ItemStack

/**
 * Modèle de recette JEI pour la Breeze Foundry.
 */
data class BreezeFoundryJeiRecipe(
    val inputA: List<ItemStack>,
    val inputB: List<ItemStack>,
    val output: ItemStack,
    val windCost: Int = 200,
    val cookTimeTicks: Int = 100
)
