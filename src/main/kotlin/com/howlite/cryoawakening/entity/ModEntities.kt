package com.howlite.cryoawakening.entity

import com.howlite.cryoawakening.CryoAwakening
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

/**
 * Registre des entités du mod Cryo Awakening.
 */
object ModEntities {

    val GLACIOPOD_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, CryoAwakening.id("glaciopod"))

    val GLACIOPOD: EntityType<GlaciopodEntity> =
        EntityType.Builder.of(::GlaciopodEntity, MobCategory.CREATURE)
            .sized(2.0f, 2.0f)
            .eyeHeight(1.0f)
            .build(GLACIOPOD_KEY)

    val GAWKER_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, CryoAwakening.id("gawker"))

    val GAWKER: EntityType<GawkerEntity> =
        EntityType.Builder.of(::GawkerEntity, MobCategory.CREATURE)
            .sized(0.6f, 0.55f)
            .eyeHeight(0.45f)
            .build(GAWKER_KEY)

    fun register() {
        // Enregistrement dans le registre d'EntityType Fabric
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GLACIOPOD_KEY, GLACIOPOD)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GAWKER_KEY, GAWKER)

        // Enregistrement des attributs par défaut (HP, Armor, Speed, Knockback)
        FabricDefaultAttributeRegistry.register(GLACIOPOD, GlaciopodEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(GAWKER, GawkerEntity.createAttributes())
    }
}
