package com.howlite.cryoawakening.worldgen.biome

import com.howlite.cryoawakening.CryoAwakening
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.fabricmc.fabric.api.biome.v1.ModificationPhase
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature

/**
 * ModBiomes
 *
 * Registre centralisé des biomes custom du mod Cryo Awakening.
 */
object ModBiomes {

    val CRYO_CAVERNS: ResourceKey<Biome> = ResourceKey.create(
        Registries.BIOME,
        CryoAwakening.id("cryo_caverns")
    )

    private val PILLARED_ICE_CAVE_PLACED: ResourceKey<PlacedFeature> = ResourceKey.create(
        Registries.PLACED_FEATURE,
        CryoAwakening.id("pillared_ice_cave")
    )

    private val AMETHYST_GEODE_PLACED: ResourceKey<PlacedFeature> = ResourceKey.create(
        Registries.PLACED_FEATURE,
        Identifier.fromNamespaceAndPath("minecraft", "amethyst_geode")
    )

    fun register() {
        // 1. Suppression des Géodes d'Améthyste dans cryo_caverns
        BiomeModifications.create(CryoAwakening.id("remove_geodes"))
            .add(
                ModificationPhase.REMOVALS,
                BiomeSelectors.includeByKey(CRYO_CAVERNS)
            ) { context ->
                context.generationSettings.removeFeature(
                    GenerationStep.Decoration.UNDERGROUND_STRUCTURES,
                    AMETHYST_GEODE_PLACED
                )
                context.generationSettings.removeFeature(
                    GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                    AMETHYST_GEODE_PLACED
                )
                context.generationSettings.removeFeature(
                    GenerationStep.Decoration.UNDERGROUND_DECORATION,
                    AMETHYST_GEODE_PLACED
                )
            }

        // 2. Injection de notre cathédrale monumentale dans les étapes sous-terraines
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_DECORATION,
            PILLARED_ICE_CAVE_PLACED
        )
    }
}
