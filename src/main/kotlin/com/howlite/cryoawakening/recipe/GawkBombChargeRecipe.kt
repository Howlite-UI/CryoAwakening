package com.howlite.cryoawakening.recipe

import com.howlite.cryoawakening.item.GawkBombItem
import com.howlite.cryoawakening.item.ModItems
import com.mojang.serialization.MapCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

/**
 * GawkBombChargeRecipe
 *
 * Recette spéciale personnalisée permettant de combiner une Gawk-Bomb avec 1 à 3 poudres à canon
 * (Gunpowder) dans n'importe quel arrangement de la table de craft pour augmenter son niveau de charge.
 */
class GawkBombChargeRecipe : CustomRecipe() {

    override fun matches(input: CraftingInput, level: Level): Boolean {
        var bombCount = 0
        var gunpowderCount = 0
        var currentCharge = 0

        for (stack in input.items()) {
            if (stack.isEmpty) continue
            if (stack.`is`(ModItems.GAWK_BOMB)) {
                bombCount++
                currentCharge = GawkBombItem.getCharge(stack)
            } else if (stack.`is`(Items.GUNPOWDER)) {
                gunpowderCount++
            } else {
                return false
            }
        }

        // Il faut exactement 1 Gawk-Bomb et au moins 1 Gunpowder, sans dépasser la charge max (3)
        return bombCount == 1 && gunpowderCount in 1..3 && (currentCharge + gunpowderCount) <= 3
    }

    override fun assemble(input: CraftingInput): ItemStack {
        var currentCharge = 0
        var gunpowderCount = 0

        for (stack in input.items()) {
            if (stack.isEmpty) continue
            if (stack.`is`(ModItems.GAWK_BOMB)) {
                currentCharge = GawkBombItem.getCharge(stack)
            } else if (stack.`is`(Items.GUNPOWDER)) {
                gunpowderCount++
            }
        }

        val newCharge = (currentCharge + gunpowderCount).coerceIn(0, 3)
        return GawkBombItem.createWithCharge(newCharge)
    }

    override fun category(): CraftingBookCategory = CraftingBookCategory.EQUIPMENT

    override fun placementInfo(): PlacementInfo = PlacementInfo.NOT_PLACEABLE

    override fun getSerializer(): RecipeSerializer<GawkBombChargeRecipe> = SERIALIZER

    companion object {
        val INSTANCE = GawkBombChargeRecipe()
        val MAP_CODEC: MapCodec<GawkBombChargeRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GawkBombChargeRecipe> = StreamCodec.unit(INSTANCE)
        val SERIALIZER: RecipeSerializer<GawkBombChargeRecipe> = RecipeSerializer(MAP_CODEC, STREAM_CODEC)
    }
}
