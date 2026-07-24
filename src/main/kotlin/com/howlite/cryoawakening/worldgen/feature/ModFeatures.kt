package com.howlite.cryoawakening.worldgen.feature

import com.howlite.cryoawakening.CryoAwakening
import com.mojang.serialization.Codec
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration

/**
 * ModFeatures
 *
 * Registre centralisé de tous les Features custom du mod Cryo Awakening.
 * Appelé depuis CryoAwakening.onInitialize() avant tout autre code WorldGen.
 */
object ModFeatures {

    /**
     * Le Feature principal de la caverne de glace à piliers.
     * Enregistré sous l'identifiant "cryo-awakening:pillared_ice_cave".
     */
    val PILLARED_ICE_CAVE: Feature<NoneFeatureConfiguration> =
        PillaredIceCaveFeature(NoneFeatureConfiguration.CODEC)

    /**
     * Enregistre tous les Features du mod dans le registre vanilla Fabric.
     * Doit être appelé depuis onInitialize() sur le thread principal.
     */
    fun register() {
        Registry.register(
            BuiltInRegistries.FEATURE,
            CryoAwakening.id("pillared_ice_cave"),
            PILLARED_ICE_CAVE
        )
    }
}
