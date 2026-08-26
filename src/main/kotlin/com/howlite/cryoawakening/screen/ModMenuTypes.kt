package com.howlite.cryoawakening.screen

import com.howlite.cryoawakening.CryoAwakening
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.inventory.MenuType

object ModMenuTypes {

    val BREEZE_FOUNDRY: MenuType<BreezeFoundryMenu> =
        MenuType(::BreezeFoundryMenu, FeatureFlagSet.of())

    fun register() {
        Registry.register(
            BuiltInRegistries.MENU,
            CryoAwakening.id("breeze_foundry"),
            BREEZE_FOUNDRY
        )
    }
}
