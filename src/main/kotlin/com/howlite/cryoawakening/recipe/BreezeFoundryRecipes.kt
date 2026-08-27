package com.howlite.cryoawakening.recipe

import com.howlite.cryoawakening.item.ModItems
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Modèle de recette interne pour la Breeze Foundry.
 */
data class BreezeFoundryRecipe(
    val ingredientA: (ItemStack) -> Boolean,
    val ingredientB: (ItemStack) -> Boolean,
    val ingredientAList: List<ItemStack>,
    val ingredientBList: List<ItemStack>,
    val output: ItemStack,
    val windCost: Int = 200,
    val cookTimeTicks: Int = 100
) {
    fun matches(in1: ItemStack, in2: ItemStack): Boolean {
        if (in1.isEmpty || in2.isEmpty) return false
        return (ingredientA(in1) && ingredientB(in2)) || (ingredientA(in2) && ingredientB(in1))
    }
}

/**
 * Registre de toutes les recettes de fusion d'alliage et crafts alternatifs de la Breeze Foundry.
 */
object BreezeFoundryRecipes {

    val RECIPES: MutableList<BreezeFoundryRecipe> = mutableListOf()

    init {
        // 1. Tellurobismuthite Ingot (Alliage principal Cryo)
        register(
            ingredientA = { it.`is`(ModItems.RAW_BISMUTH) },
            ingredientB = { it.`is`(ModItems.RAW_TELLURIUM) },
            ingredientAList = listOf(ItemStack(ModItems.RAW_BISMUTH)),
            ingredientBList = listOf(ItemStack(ModItems.RAW_TELLURIUM)),
            output = ItemStack(ModItems.TELLUROBISMUTHITE_INGOT),
            windCost = 200,
            cookTimeTicks = 100
        )

        // 2. Réduction de Scorie : Bismuth Dross + Coal / Charcoal = Raw Bismuth
        register(
            ingredientA = { it.`is`(ModItems.BISMUTH_DROSS) },
            ingredientB = { it.`is`(Items.COAL) || it.`is`(Items.CHARCOAL) },
            ingredientAList = listOf(ItemStack(ModItems.BISMUTH_DROSS)),
            ingredientBList = listOf(ItemStack(Items.COAL), ItemStack(Items.CHARCOAL)),
            output = ItemStack(ModItems.RAW_BISMUTH),
            windCost = 150,
            cookTimeTicks = 80
        )

        // 3. Lentille Optique : Bismuth Dross + Quartz = Dross Glass Lens
        register(
            ingredientA = { it.`is`(ModItems.BISMUTH_DROSS) },
            ingredientB = { it.`is`(Items.QUARTZ) },
            ingredientAList = listOf(ItemStack(ModItems.BISMUTH_DROSS)),
            ingredientBList = listOf(ItemStack(Items.QUARTZ)),
            output = ItemStack(ModItems.DROSS_GLASS_LENS),
            windCost = 200,
            cookTimeTicks = 100
        )

        // 4. Verre Teinté Haute Pression : Glass + Amethyst Shard = Tinted Glass (x2)
        register(
            ingredientA = { it.`is`(Items.GLASS) },
            ingredientB = { it.`is`(Items.AMETHYST_SHARD) },
            ingredientAList = listOf(ItemStack(Items.GLASS)),
            ingredientBList = listOf(ItemStack(Items.AMETHYST_SHARD)),
            output = ItemStack(Items.TINTED_GLASS, 2),
            windCost = 100,
            cookTimeTicks = 60
        )

        // 5. Compression Cryo-Vent : Ice + Snow Block = Packed Ice (x2)
        register(
            ingredientA = { it.`is`(Items.ICE) },
            ingredientB = { it.`is`(Items.SNOW_BLOCK) },
            ingredientAList = listOf(ItemStack(Items.ICE)),
            ingredientBList = listOf(ItemStack(Items.SNOW_BLOCK)),
            output = ItemStack(Items.PACKED_ICE, 2),
            windCost = 100,
            cookTimeTicks = 60
        )

        // 6. Cristallisation Glaciale : Packed Ice + Amethyst Shard = Blue Ice (x2)
        register(
            ingredientA = { it.`is`(Items.PACKED_ICE) },
            ingredientB = { it.`is`(Items.AMETHYST_SHARD) },
            ingredientAList = listOf(ItemStack(Items.PACKED_ICE)),
            ingredientBList = listOf(ItemStack(Items.AMETHYST_SHARD)),
            output = ItemStack(Items.BLUE_ICE, 2),
            windCost = 150,
            cookTimeTicks = 80
        )

        // 7. Synthèse Prismarine : Quartz + Glowstone Dust = Prismarine Crystals (x2)
        register(
            ingredientA = { it.`is`(Items.QUARTZ) },
            ingredientB = { it.`is`(Items.GLOWSTONE_DUST) },
            ingredientAList = listOf(ItemStack(Items.QUARTZ)),
            ingredientBList = listOf(ItemStack(Items.GLOWSTONE_DUST)),
            output = ItemStack(Items.PRISMARINE_CRYSTALS, 2),
            windCost = 120,
            cookTimeTicks = 70
        )

        // 8. Fusion Verrière Turbocompressée : Sand + Blaze Powder = Glass (x4)
        register(
            ingredientA = { it.`is`(Items.SAND) || it.`is`(Items.RED_SAND) },
            ingredientB = { it.`is`(Items.BLAZE_POWDER) },
            ingredientAList = listOf(ItemStack(Items.SAND), ItemStack(Items.RED_SAND)),
            ingredientBList = listOf(ItemStack(Items.BLAZE_POWDER)),
            output = ItemStack(Items.GLASS, 4),
            windCost = 80,
            cookTimeTicks = 50
        )
    }

    fun register(
        ingredientA: (ItemStack) -> Boolean,
        ingredientB: (ItemStack) -> Boolean,
        ingredientAList: List<ItemStack>,
        ingredientBList: List<ItemStack>,
        output: ItemStack,
        windCost: Int,
        cookTimeTicks: Int
    ) {
        RECIPES.add(
            BreezeFoundryRecipe(
                ingredientA,
                ingredientB,
                ingredientAList,
                ingredientBList,
                output,
                windCost,
                cookTimeTicks
            )
        )
    }

    fun findRecipe(in1: ItemStack, in2: ItemStack): BreezeFoundryRecipe? {
        if (in1.isEmpty || in2.isEmpty) return null
        return RECIPES.firstOrNull { it.matches(in1, in2) }
    }

    fun isValidIngredient(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return RECIPES.any { it.ingredientA(stack) || it.ingredientB(stack) }
    }
}
