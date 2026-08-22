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

    val GAWK_BOMB_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, CryoAwakening.id("gawk_bomb"))

    val GAWK_BOMB: EntityType<GawkBombEntity> =
        EntityType.Builder.of(::GawkBombEntity, MobCategory.MISC)
            .sized(0.6f, 0.55f)
            .eyeHeight(0.45f)
            .build(GAWK_BOMB_KEY)

    val GALE_BOOMERANG_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, CryoAwakening.id("gale_boomerang"))

    val GALE_BOOMERANG: EntityType<GaleBoomerangEntity> =
        EntityType.Builder.of(::GaleBoomerangEntity, MobCategory.MISC)
            .sized(0.5f, 0.25f)
            .eyeHeight(0.12f)
            .build(GALE_BOOMERANG_KEY)

    fun register() {
        // Enregistrement dans le registre d'EntityType Fabric
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GLACIOPOD_KEY, GLACIOPOD)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GAWKER_KEY, GAWKER)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GAWK_BOMB_KEY, GAWK_BOMB)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GALE_BOOMERANG_KEY, GALE_BOOMERANG)

        // Enregistrement des attributs par défaut (HP, Armor, Speed, Knockback)
        FabricDefaultAttributeRegistry.register(GLACIOPOD, GlaciopodEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(GAWKER, GawkerEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(GAWK_BOMB, GawkBombEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(GALE_BOOMERANG, GaleBoomerangEntity.createAttributes())
    }
}
