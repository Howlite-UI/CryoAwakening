package com.howlite.cryoawakening.enchantment

import com.howlite.cryoawakening.CryoAwakening
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level

/**
 * ModEnchantments
 *
 * Registre des clés et utilitaires pour les 8 enchantements du Gale Boomerang (1.21.4).
 */
object ModEnchantments {

    val HEAVYWEIGHT: ResourceKey<Enchantment> = key("heavyweight")
    val ZEPHYR: ResourceKey<Enchantment> = key("zephyr")
    val RICOCHET: ResourceKey<Enchantment> = key("ricochet")
    val SOAR: ResourceKey<Enchantment> = key("soar")
    val FROSTWIND: ResourceKey<Enchantment> = key("frostwind")
    val ORBIT: ResourceKey<Enchantment> = key("orbit")
    val RETRIEVAL: ResourceKey<Enchantment> = key("retrieval")
    val GALE_VORTEX: ResourceKey<Enchantment> = key("gale_vortex")
    val HAWKEYE: ResourceKey<Enchantment> = key("hawkeye")

    private fun key(name: String): ResourceKey<Enchantment> =
        ResourceKey.create(Registries.ENCHANTMENT, CryoAwakening.id(name))

    fun getLevel(stack: ItemStack, key: ResourceKey<Enchantment>, level: Level): Int {
        if (stack.isEmpty) return 0
        val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        val holder = registry.get(key).orElse(null) ?: return 0
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack)
    }
}
