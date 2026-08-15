package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.CryoAwakening
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.item.equipment.EquipmentAssets

/**
 * Définition des matériaux d'armure pour les 3 tiers de Cryo Awakening :
 * - FOSSILIZED : Tier 1 (Statistiques de l'armure en Fer)
 * - PRIMORDIAL : Tier 2 (Statistiques de l'armure en Diamant)
 * - APEX_GLACIAL : Tier 3 (Statistiques de l'armure en Netherite avec haute résistance au recul)
 */
object ModArmorMaterials {

    private fun asset(name: String): ResourceKey<EquipmentAsset> =
        ResourceKey.create(EquipmentAssets.ROOT_ID, CryoAwakening.id(name))

    // 1. Matériau Fossilisé (Tier 1 - Équivalent Fer)
    val FOSSILIZED_ASSET_KEY: ResourceKey<EquipmentAsset> = asset("fossilized")
    val FOSSILIZED: ArmorMaterial = ArmorMaterial(
        15, // Durabilité de base (x multiplicateur par pièce)
        mapOf(
            ArmorType.BOOTS to 2,
            ArmorType.LEGGINGS to 5,
            ArmorType.CHESTPLATE to 6,
            ArmorType.HELMET to 2,
            ArmorType.BODY to 5
        ),
        9, // Enchantabilité
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0f, // Toughness (Robustesse)
        0.0f, // Knockback Resistance
        ItemTags.REPAIRS_IRON_ARMOR,
        FOSSILIZED_ASSET_KEY
    )

    // 2. Matériau Primordial (Tier 2 - Équivalent Diamant)
    val PRIMORDIAL_ASSET_KEY: ResourceKey<EquipmentAsset> = asset("primordial")
    val PRIMORDIAL: ArmorMaterial = ArmorMaterial(
        33, // Durabilité de base
        mapOf(
            ArmorType.BOOTS to 3,
            ArmorType.LEGGINGS to 6,
            ArmorType.CHESTPLATE to 8,
            ArmorType.HELMET to 3,
            ArmorType.BODY to 11
        ),
        10, // Enchantabilité
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.0f, // Toughness (Robustesse)
        0.0f, // Knockback Resistance
        ItemTags.REPAIRS_DIAMOND_ARMOR,
        PRIMORDIAL_ASSET_KEY
    )

    // 3. Matériau Apex Glacial (Tier 3 - Équivalent Netherite renforcé)
    val APEX_GLACIAL_ASSET_KEY: ResourceKey<EquipmentAsset> = asset("apex_glacial")
    val APEX_GLACIAL: ArmorMaterial = ArmorMaterial(
        37, // Durabilité de base
        mapOf(
            ArmorType.BOOTS to 3,
            ArmorType.LEGGINGS to 6,
            ArmorType.CHESTPLATE to 8,
            ArmorType.HELMET to 3,
            ArmorType.BODY to 11
        ),
        15, // Enchantabilité
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0f, // Toughness (Robustesse)
        0.2f, // Haute résistance au recul (Knockback Resistance 0.2f)
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        APEX_GLACIAL_ASSET_KEY
    )
}
