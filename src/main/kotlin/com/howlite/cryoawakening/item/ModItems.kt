package com.howlite.cryoawakening.item

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.ModEntities
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType

/**
 * Enregistrement et gestion de tous les items du mod Cryo Awakening,
 * incluant les 3 sets d'armures complets :
 * - Fossilized (Casque, Plastron, Jambières, Bottes)
 * - Primordial (Casque, Plastron, Jambières, Bottes)
 * - Apex Glacial (Casque, Plastron, Jambières, Bottes)
 */
object ModItems {

    private fun itemKey(name: String): ResourceKey<Item> =
        ResourceKey.create(Registries.ITEM, CryoAwakening.id(name))

    // --- Minerais & Matériaux ---
    val RAW_BISMUTH_KEY: ResourceKey<Item> = itemKey("raw_bismuth")
    val RAW_BISMUTH: Item = Item(Item.Properties().setId(RAW_BISMUTH_KEY))

    val RAW_TELLURIUM_KEY: ResourceKey<Item> = itemKey("raw_tellurium")
    val RAW_TELLURIUM: Item = Item(Item.Properties().setId(RAW_TELLURIUM_KEY))

    val TELLUROBISMUTHITE_INGOT_KEY: ResourceKey<Item> = itemKey("tellurobismuthite_ingot")
    val TELLUROBISMUTHITE_INGOT: Item = Item(Item.Properties().setId(TELLUROBISMUTHITE_INGOT_KEY))

    val BISMUTH_DROSS_KEY: ResourceKey<Item> = itemKey("bismuth_dross")
    val BISMUTH_DROSS: Item = Item(Item.Properties().setId(BISMUTH_DROSS_KEY))

    val DROSS_GLASS_LENS_KEY: ResourceKey<Item> = itemKey("dross_glass_lens")
    val DROSS_GLASS_LENS: Item = Item(Item.Properties().setId(DROSS_GLASS_LENS_KEY))

    val WRENCH_KEY: ResourceKey<Item> = itemKey("wrench")
    val WRENCH: Item = Item(Item.Properties().setId(WRENCH_KEY).stacksTo(1).durability(256))

    val FOSSILIZED_LILAC_LEAF_KEY: ResourceKey<Item> = itemKey("fossilized_lilac_leaf")
    val FOSSILIZED_LILAC_LEAF: Item = Item(Item.Properties().setId(FOSSILIZED_LILAC_LEAF_KEY))

    // --- Items Gawker ---
    val GAWKER_FUR_KEY: ResourceKey<Item> = itemKey("gawker_fur")
    val GAWKER_FUR: Item = Item(Item.Properties().setId(GAWKER_FUR_KEY))

    val GAWK_BOMB_KEY: ResourceKey<Item> = itemKey("gawk_bomb")
    val GAWK_BOMB: Item = GawkBombItem(Item.Properties().setId(GAWK_BOMB_KEY).stacksTo(16))

    val GALE_BOOMERANG_ATTRIBUTES: ItemAttributeModifiers = ItemAttributeModifiers.builder()
        .add(
            Attributes.ATTACK_DAMAGE,
            AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        )
        .add(
            Attributes.ATTACK_SPEED,
            AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        )
        .build()

    val GALE_BOOMERANG_KEY: ResourceKey<Item> = itemKey("gale_boomerang")
    val GALE_BOOMERANG: Item = GaleBoomerangItem(
        Item.Properties()
            .setId(GALE_BOOMERANG_KEY)
            .stacksTo(1)
            .durability(384)
            .enchantable(15)
            .attributes(GALE_BOOMERANG_ATTRIBUTES)
    )

    val GAWKER_SPAWN_EGG_KEY: ResourceKey<Item> = itemKey("gawker_spawn_egg")
    val GAWKER_SPAWN_EGG: Item = SpawnEggItem(
        Item.Properties()
            .setId(GAWKER_SPAWN_EGG_KEY)
            .spawnEgg(ModEntities.GAWKER)
    )

    // --- Items Lumesh & Flore ---
    val LUMESH_SEED_KEY: ResourceKey<Item> = itemKey("lumesh_seed")
    val LUMESH_SEED: Item = net.minecraft.world.item.BlockItem(
        com.howlite.cryoawakening.ModBlocks.LUMESH_STEM,
        Item.Properties().setId(LUMESH_SEED_KEY).stacksTo(64)
    )

    val ORANGE_LUMESH_SLICE_KEY: ResourceKey<Item> = itemKey("orange_lumesh_slice")
    val ORANGE_LUMESH_SLICE: Item = Item(
        Item.Properties()
            .setId(ORANGE_LUMESH_SLICE_KEY)
            .food(net.minecraft.world.food.FoodProperties(4, 0.6f, false))
    )

    val ORANGE_LUMESH_HULL_KEY: ResourceKey<Item> = itemKey("orange_lumesh_hull")
    val ORANGE_LUMESH_HULL: Item = Item(Item.Properties().setId(ORANGE_LUMESH_HULL_KEY).stacksTo(64))

    val YELLOW_LUMESH_SLICE_KEY: ResourceKey<Item> = itemKey("yellow_lumesh_slice")
    val YELLOW_LUMESH_SLICE: Item = Item(
        Item.Properties()
            .setId(YELLOW_LUMESH_SLICE_KEY)
            .food(net.minecraft.world.food.FoodProperties(4, 0.6f, false))
    )

    val YELLOW_LUMESH_HULL_KEY: ResourceKey<Item> = itemKey("yellow_lumesh_hull")
    val YELLOW_LUMESH_HULL: Item = Item(Item.Properties().setId(YELLOW_LUMESH_HULL_KEY).stacksTo(64))

    private fun createArmor(name: String, material: ArmorMaterial, armorType: ArmorType): Pair<ResourceKey<Item>, Item> {
        val key = itemKey(name)
        val item = Item(
            Item.Properties()
                .setId(key)
                .humanoidArmor(material, armorType)
        )
        return Pair(key, item)
    }

    // --- Set 1 : Fossilized (Tier 1) ---
    val FOSSILIZED_HELMET_KEY: ResourceKey<Item> = itemKey("fossilized_helmet")
    val FOSSILIZED_HELMET: Item = GeoArmorItem(
        Item.Properties().setId(FOSSILIZED_HELMET_KEY).humanoidArmor(ModArmorMaterials.FOSSILIZED, ArmorType.HELMET)
    )

    val FOSSILIZED_CHESTPLATE_KEY: ResourceKey<Item> = itemKey("fossilized_chestplate")
    val FOSSILIZED_CHESTPLATE: Item = Item(Item.Properties().setId(FOSSILIZED_CHESTPLATE_KEY).humanoidArmor(ModArmorMaterials.FOSSILIZED, ArmorType.CHESTPLATE))

    val FOSSILIZED_LEGGINGS_KEY: ResourceKey<Item> = itemKey("fossilized_leggings")
    val FOSSILIZED_LEGGINGS: Item = Item(Item.Properties().setId(FOSSILIZED_LEGGINGS_KEY).humanoidArmor(ModArmorMaterials.FOSSILIZED, ArmorType.LEGGINGS))

    val FOSSILIZED_BOOTS_KEY: ResourceKey<Item> = itemKey("fossilized_boots")
    val FOSSILIZED_BOOTS: Item = Item(Item.Properties().setId(FOSSILIZED_BOOTS_KEY).humanoidArmor(ModArmorMaterials.FOSSILIZED, ArmorType.BOOTS))

    // --- Set 2 : Primordial (Tier 2) ---
    val PRIMORDIAL_HELMET_KEY: ResourceKey<Item> = itemKey("primordial_helmet")
    val PRIMORDIAL_HELMET: Item = Item(Item.Properties().setId(PRIMORDIAL_HELMET_KEY).humanoidArmor(ModArmorMaterials.PRIMORDIAL, ArmorType.HELMET))

    val PRIMORDIAL_CHESTPLATE_KEY: ResourceKey<Item> = itemKey("primordial_chestplate")
    val PRIMORDIAL_CHESTPLATE: Item = Item(Item.Properties().setId(PRIMORDIAL_CHESTPLATE_KEY).humanoidArmor(ModArmorMaterials.PRIMORDIAL, ArmorType.CHESTPLATE))

    val PRIMORDIAL_LEGGINGS_KEY: ResourceKey<Item> = itemKey("primordial_leggings")
    val PRIMORDIAL_LEGGINGS: Item = Item(Item.Properties().setId(PRIMORDIAL_LEGGINGS_KEY).humanoidArmor(ModArmorMaterials.PRIMORDIAL, ArmorType.LEGGINGS))

    val PRIMORDIAL_BOOTS_KEY: ResourceKey<Item> = itemKey("primordial_boots")
    val PRIMORDIAL_BOOTS: Item = Item(Item.Properties().setId(PRIMORDIAL_BOOTS_KEY).humanoidArmor(ModArmorMaterials.PRIMORDIAL, ArmorType.BOOTS))

    // --- Set 3 : Apex Glacial (Tier 3) ---
    val APEX_GLACIAL_HELMET_KEY: ResourceKey<Item> = itemKey("apex_glacial_helmet")
    val APEX_GLACIAL_HELMET: Item = Item(Item.Properties().setId(APEX_GLACIAL_HELMET_KEY).humanoidArmor(ModArmorMaterials.APEX_GLACIAL, ArmorType.HELMET))

    val APEX_GLACIAL_CHESTPLATE_KEY: ResourceKey<Item> = itemKey("apex_glacial_chestplate")
    val APEX_GLACIAL_CHESTPLATE: Item = Item(Item.Properties().setId(APEX_GLACIAL_CHESTPLATE_KEY).humanoidArmor(ModArmorMaterials.APEX_GLACIAL, ArmorType.CHESTPLATE))

    val APEX_GLACIAL_LEGGINGS_KEY: ResourceKey<Item> = itemKey("apex_glacial_leggings")
    val APEX_GLACIAL_LEGGINGS: Item = Item(Item.Properties().setId(APEX_GLACIAL_LEGGINGS_KEY).humanoidArmor(ModArmorMaterials.APEX_GLACIAL, ArmorType.LEGGINGS))

    val APEX_GLACIAL_BOOTS_KEY: ResourceKey<Item> = itemKey("apex_glacial_boots")
    val APEX_GLACIAL_BOOTS: Item = Item(Item.Properties().setId(APEX_GLACIAL_BOOTS_KEY).humanoidArmor(ModArmorMaterials.APEX_GLACIAL, ArmorType.BOOTS))

    // Liste de toutes les pièces d'armure enregistrées
    val ALL_ARMOR_ITEMS: List<Item> = listOf(
        FOSSILIZED_HELMET, FOSSILIZED_CHESTPLATE, FOSSILIZED_LEGGINGS, FOSSILIZED_BOOTS,
        PRIMORDIAL_HELMET, PRIMORDIAL_CHESTPLATE, PRIMORDIAL_LEGGINGS, PRIMORDIAL_BOOTS,
        APEX_GLACIAL_HELMET, APEX_GLACIAL_CHESTPLATE, APEX_GLACIAL_LEGGINGS, APEX_GLACIAL_BOOTS
    )

    /**
     * Tiers d'armure pour la mécanique de bonus de set complet
     */
    enum class ArmorTier {
        FOSSILIZED,
        PRIMORDIAL,
        APEX_GLACIAL
    }

    /**
     * Détermine si le joueur porte un set complet d'armure du mod et renvoie le tier correspondant.
     * Renvoie null si le set est incomplet ou constitué d'armures mixtes.
     */
    fun getEquippedFullSetTier(player: Player): ArmorTier? {
        val head = player.getItemBySlot(EquipmentSlot.HEAD).item
        val chest = player.getItemBySlot(EquipmentSlot.CHEST).item
        val legs = player.getItemBySlot(EquipmentSlot.LEGS).item
        val feet = player.getItemBySlot(EquipmentSlot.FEET).item

        return when {
            head == FOSSILIZED_HELMET && chest == FOSSILIZED_CHESTPLATE &&
            legs == FOSSILIZED_LEGGINGS && feet == FOSSILIZED_BOOTS -> ArmorTier.FOSSILIZED

            head == PRIMORDIAL_HELMET && chest == PRIMORDIAL_CHESTPLATE &&
            legs == PRIMORDIAL_LEGGINGS && feet == PRIMORDIAL_BOOTS -> ArmorTier.PRIMORDIAL

            head == APEX_GLACIAL_HELMET && chest == APEX_GLACIAL_CHESTPLATE &&
            legs == APEX_GLACIAL_LEGGINGS && feet == APEX_GLACIAL_BOOTS -> ArmorTier.APEX_GLACIAL

            else -> null
        }
    }

    /**
     * Enregistre tous les items d'armure dans le registre BuiltInRegistries.ITEM
     */
    fun register() {
        // Minerais & Matériaux
        Registry.register(BuiltInRegistries.ITEM, RAW_BISMUTH_KEY, RAW_BISMUTH)
        Registry.register(BuiltInRegistries.ITEM, RAW_TELLURIUM_KEY, RAW_TELLURIUM)
        Registry.register(BuiltInRegistries.ITEM, TELLUROBISMUTHITE_INGOT_KEY, TELLUROBISMUTHITE_INGOT)
        Registry.register(BuiltInRegistries.ITEM, BISMUTH_DROSS_KEY, BISMUTH_DROSS)
        Registry.register(BuiltInRegistries.ITEM, DROSS_GLASS_LENS_KEY, DROSS_GLASS_LENS)
        Registry.register(BuiltInRegistries.ITEM, WRENCH_KEY, WRENCH)
        Registry.register(BuiltInRegistries.ITEM, FOSSILIZED_LILAC_LEAF_KEY, FOSSILIZED_LILAC_LEAF)
        Registry.register(BuiltInRegistries.ITEM, GAWKER_FUR_KEY, GAWKER_FUR)
        Registry.register(BuiltInRegistries.ITEM, GAWK_BOMB_KEY, GAWK_BOMB)
        Registry.register(BuiltInRegistries.ITEM, GALE_BOOMERANG_KEY, GALE_BOOMERANG)
        Registry.register(BuiltInRegistries.ITEM, GAWKER_SPAWN_EGG_KEY, GAWKER_SPAWN_EGG)

        // Items Lumesh & Flore
        Registry.register(BuiltInRegistries.ITEM, LUMESH_SEED_KEY, LUMESH_SEED)
        Registry.register(BuiltInRegistries.ITEM, ORANGE_LUMESH_SLICE_KEY, ORANGE_LUMESH_SLICE)
        Registry.register(BuiltInRegistries.ITEM, ORANGE_LUMESH_HULL_KEY, ORANGE_LUMESH_HULL)
        Registry.register(BuiltInRegistries.ITEM, YELLOW_LUMESH_SLICE_KEY, YELLOW_LUMESH_SLICE)
        Registry.register(BuiltInRegistries.ITEM, YELLOW_LUMESH_HULL_KEY, YELLOW_LUMESH_HULL)

        // Set Fossilized
        Registry.register(BuiltInRegistries.ITEM, FOSSILIZED_HELMET_KEY, FOSSILIZED_HELMET)
        Registry.register(BuiltInRegistries.ITEM, FOSSILIZED_CHESTPLATE_KEY, FOSSILIZED_CHESTPLATE)
        Registry.register(BuiltInRegistries.ITEM, FOSSILIZED_LEGGINGS_KEY, FOSSILIZED_LEGGINGS)
        Registry.register(BuiltInRegistries.ITEM, FOSSILIZED_BOOTS_KEY, FOSSILIZED_BOOTS)

        // Set Primordial
        Registry.register(BuiltInRegistries.ITEM, PRIMORDIAL_HELMET_KEY, PRIMORDIAL_HELMET)
        Registry.register(BuiltInRegistries.ITEM, PRIMORDIAL_CHESTPLATE_KEY, PRIMORDIAL_CHESTPLATE)
        Registry.register(BuiltInRegistries.ITEM, PRIMORDIAL_LEGGINGS_KEY, PRIMORDIAL_LEGGINGS)
        Registry.register(BuiltInRegistries.ITEM, PRIMORDIAL_BOOTS_KEY, PRIMORDIAL_BOOTS)

        // Set Apex Glacial
        Registry.register(BuiltInRegistries.ITEM, APEX_GLACIAL_HELMET_KEY, APEX_GLACIAL_HELMET)
        Registry.register(BuiltInRegistries.ITEM, APEX_GLACIAL_CHESTPLATE_KEY, APEX_GLACIAL_CHESTPLATE)
        Registry.register(BuiltInRegistries.ITEM, APEX_GLACIAL_LEGGINGS_KEY, APEX_GLACIAL_LEGGINGS)
        Registry.register(BuiltInRegistries.ITEM, APEX_GLACIAL_BOOTS_KEY, APEX_GLACIAL_BOOTS)
    }
}
