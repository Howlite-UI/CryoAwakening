package com.howlite.cryoawakening

import com.howlite.cryoawakening.block.CryoVentBlock
import com.howlite.cryoawakening.block.entity.CryoVentBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.GlowLichenBlock
import net.minecraft.world.level.block.MultifaceBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object ModBlocks {

    private fun blockKey(name: String): ResourceKey<Block> =
        ResourceKey.create(Registries.BLOCK, CryoAwakening.id(name))

    private fun itemKey(name: String): ResourceKey<Item> =
        ResourceKey.create(Registries.ITEM, CryoAwakening.id(name))

    // 1. Cryo Vent (Bloc spécial avec particules)
    val CRYO_VENT_KEY: ResourceKey<Block> = blockKey("cryo_vent")
    val CRYO_VENT_ITEM_KEY: ResourceKey<Item> = itemKey("cryo_vent")
    val CRYO_VENT: Block = CryoVentBlock(
        BlockBehaviour.Properties.of()
            .setId(CRYO_VENT_KEY)
            .strength(1.5f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    )
    val CRYO_VENT_ITEM: Item = BlockItem(CRYO_VENT, Item.Properties().setId(CRYO_VENT_ITEM_KEY))

    // BlockEntityType pour CryoVentBlock
    val CRYO_VENT_BLOCK_ENTITY_TYPE: BlockEntityType<CryoVentBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::CryoVentBlockEntity, CRYO_VENT).build()

    // 2. Bismuth Ore Shivering Shale
    val BISMUTH_ORE_SHIVERING_SHALE_KEY: ResourceKey<Block> = blockKey("bismuth_ore_shivering_shale")
    val BISMUTH_ORE_SHIVERING_SHALE_ITEM_KEY: ResourceKey<Item> = itemKey("bismuth_ore_shivering_shale")
    val BISMUTH_ORE_SHIVERING_SHALE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(BISMUTH_ORE_SHIVERING_SHALE_KEY)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val BISMUTH_ORE_SHIVERING_SHALE_ITEM: Item = BlockItem(BISMUTH_ORE_SHIVERING_SHALE, Item.Properties().setId(BISMUTH_ORE_SHIVERING_SHALE_ITEM_KEY))

    // 3. Frozen Flysch
    val FROZEN_FLYSCH_KEY: ResourceKey<Block> = blockKey("frozen_flysch")
    val FROZEN_FLYSCH_ITEM_KEY: ResourceKey<Item> = itemKey("frozen_flysch")
    val FROZEN_FLYSCH: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(FROZEN_FLYSCH_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val FROZEN_FLYSCH_ITEM: Item = BlockItem(FROZEN_FLYSCH, Item.Properties().setId(FROZEN_FLYSCH_ITEM_KEY))

    // 4. Blue Frozen Flysch
    val BLUE_FROZEN_FLYSCH_KEY: ResourceKey<Block> = blockKey("blue_frozen_flysch")
    val BLUE_FROZEN_FLYSCH_ITEM_KEY: ResourceKey<Item> = itemKey("blue_frozen_flysch")
    val BLUE_FROZEN_FLYSCH: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(BLUE_FROZEN_FLYSCH_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val BLUE_FROZEN_FLYSCH_ITEM: Item = BlockItem(BLUE_FROZEN_FLYSCH, Item.Properties().setId(BLUE_FROZEN_FLYSCH_ITEM_KEY))

    // 5. Shivering Shale Stone
    val SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("shivering_shale_stone")
    val SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("shivering_shale_stone")
    val SHIVERING_SHALE_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(SHIVERING_SHALE_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(SHIVERING_SHALE_STONE, Item.Properties().setId(SHIVERING_SHALE_STONE_ITEM_KEY))

    // 6. Gabbro Stone
    val GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("gabbro_stone")
    val GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("gabbro_stone")
    val GABBRO_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(GABBRO_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val GABBRO_STONE_ITEM: Item = BlockItem(GABBRO_STONE, Item.Properties().setId(GABBRO_STONE_ITEM_KEY))

    // 7. Ice Sheet Shivering Shale Stone
    val ICE_SHEET_SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("ice_sheet_shivering_shale_stone")
    val ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet_shivering_shale_stone")
    val ICE_SHEET_SHIVERING_SHALE_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(ICE_SHEET_SHIVERING_SHALE_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val ICE_SHEET_SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(ICE_SHEET_SHIVERING_SHALE_STONE, Item.Properties().setId(ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY))

    // 8. Blue Ice Sheet Shivering Shale Stone
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet_shivering_shale_stone")
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet_shivering_shale_stone")
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE, Item.Properties().setId(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY))

    // 9. Ice Sheet Gabbro Stone
    val ICE_SHEET_GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("ice_sheet_gabbro_stone")
    val ICE_SHEET_GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet_gabbro_stone")
    val ICE_SHEET_GABBRO_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(ICE_SHEET_GABBRO_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val ICE_SHEET_GABBRO_STONE_ITEM: Item = BlockItem(ICE_SHEET_GABBRO_STONE, Item.Properties().setId(ICE_SHEET_GABBRO_STONE_ITEM_KEY))

    // 10. Blue Ice Sheet Gabbro Stone
    val BLUE_ICE_SHEET_GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet_gabbro_stone")
    val BLUE_ICE_SHEET_GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet_gabbro_stone")
    val BLUE_ICE_SHEET_GABBRO_STONE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(BLUE_ICE_SHEET_GABBRO_STONE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val BLUE_ICE_SHEET_GABBRO_STONE_ITEM: Item = BlockItem(BLUE_ICE_SHEET_GABBRO_STONE, Item.Properties().setId(BLUE_ICE_SHEET_GABBRO_STONE_ITEM_KEY))

    // 11. Ice Sheet (Élément de surface type lichen/nappe)
    val ICE_SHEET_KEY: ResourceKey<Block> = blockKey("ice_sheet")
    val ICE_SHEET_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet")
    val ICE_SHEET: Block = GlowLichenBlock(
        BlockBehaviour.Properties.of()
            .setId(ICE_SHEET_KEY)
            .replaceable()
            .noCollision()
            .strength(0.2f)
            .sound(SoundType.GLASS)
    )
    val ICE_SHEET_ITEM: Item = BlockItem(ICE_SHEET, Item.Properties().setId(ICE_SHEET_ITEM_KEY))

    // 12. Blue Ice Sheet (Élément de surface type lichen/nappe)
    val BLUE_ICE_SHEET_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet")
    val BLUE_ICE_SHEET_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet")
    val BLUE_ICE_SHEET: Block = GlowLichenBlock(
        BlockBehaviour.Properties.of()
            .setId(BLUE_ICE_SHEET_KEY)
            .replaceable()
            .noCollision()
            .strength(0.2f)
            .sound(SoundType.GLASS)
    )
    val BLUE_ICE_SHEET_ITEM: Item = BlockItem(BLUE_ICE_SHEET, Item.Properties().setId(BLUE_ICE_SHEET_ITEM_KEY))

    // Onglet Créatif Dédié "Cryo Awakening"
    val CRYO_AWAKENING_TAB_KEY: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        CryoAwakening.id("cryo_awakening")
    )

    val CRYO_AWAKENING_TAB: CreativeModeTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .icon { ItemStack(CRYO_VENT_ITEM) }
        .title(Component.translatable("itemGroup.cryo-awakening.cryo_awakening"))
        .displayItems { _, output ->
            output.accept(CRYO_VENT_ITEM)
            output.accept(BISMUTH_ORE_SHIVERING_SHALE_ITEM)
            output.accept(FROZEN_FLYSCH_ITEM)
            output.accept(BLUE_FROZEN_FLYSCH_ITEM)
            output.accept(SHIVERING_SHALE_STONE_ITEM)
            output.accept(GABBRO_STONE_ITEM)
            output.accept(ICE_SHEET_SHIVERING_SHALE_STONE_ITEM)
            output.accept(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM)
            output.accept(ICE_SHEET_GABBRO_STONE_ITEM)
            output.accept(BLUE_ICE_SHEET_GABBRO_STONE_ITEM)
            output.accept(ICE_SHEET_ITEM)
            output.accept(BLUE_ICE_SHEET_ITEM)
        }
        .build()

    fun register() {
        // Enregistrement des blocs et items
        registerBlock(CRYO_VENT_KEY, CRYO_VENT, CRYO_VENT_ITEM_KEY, CRYO_VENT_ITEM)
        registerBlock(BISMUTH_ORE_SHIVERING_SHALE_KEY, BISMUTH_ORE_SHIVERING_SHALE, BISMUTH_ORE_SHIVERING_SHALE_ITEM_KEY, BISMUTH_ORE_SHIVERING_SHALE_ITEM)
        registerBlock(FROZEN_FLYSCH_KEY, FROZEN_FLYSCH, FROZEN_FLYSCH_ITEM_KEY, FROZEN_FLYSCH_ITEM)
        registerBlock(BLUE_FROZEN_FLYSCH_KEY, BLUE_FROZEN_FLYSCH, BLUE_FROZEN_FLYSCH_ITEM_KEY, BLUE_FROZEN_FLYSCH_ITEM)
        registerBlock(SHIVERING_SHALE_STONE_KEY, SHIVERING_SHALE_STONE, SHIVERING_SHALE_STONE_ITEM_KEY, SHIVERING_SHALE_STONE_ITEM)
        registerBlock(GABBRO_STONE_KEY, GABBRO_STONE, GABBRO_STONE_ITEM_KEY, GABBRO_STONE_ITEM)
        registerBlock(ICE_SHEET_SHIVERING_SHALE_STONE_KEY, ICE_SHEET_SHIVERING_SHALE_STONE, ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY, ICE_SHEET_SHIVERING_SHALE_STONE_ITEM)
        registerBlock(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_KEY, BLUE_ICE_SHEET_SHIVERING_SHALE_STONE, BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY, BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM)
        registerBlock(ICE_SHEET_GABBRO_STONE_KEY, ICE_SHEET_GABBRO_STONE, ICE_SHEET_GABBRO_STONE_ITEM_KEY, ICE_SHEET_GABBRO_STONE_ITEM)
        registerBlock(BLUE_ICE_SHEET_GABBRO_STONE_KEY, BLUE_ICE_SHEET_GABBRO_STONE, BLUE_ICE_SHEET_GABBRO_STONE_ITEM_KEY, BLUE_ICE_SHEET_GABBRO_STONE_ITEM)
        registerBlock(ICE_SHEET_KEY, ICE_SHEET, ICE_SHEET_ITEM_KEY, ICE_SHEET_ITEM)
        registerBlock(BLUE_ICE_SHEET_KEY, BLUE_ICE_SHEET, BLUE_ICE_SHEET_ITEM_KEY, BLUE_ICE_SHEET_ITEM)

        // BlockEntityType
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("cryo_vent"),
            CRYO_VENT_BLOCK_ENTITY_TYPE
        )

        // Onglet Créatif Mod
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            CRYO_AWAKENING_TAB_KEY,
            CRYO_AWAKENING_TAB
        )
    }

    private fun registerBlock(
        blockKey: ResourceKey<Block>,
        block: Block,
        itemKey: ResourceKey<Item>,
        item: Item
    ) {
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block)
        Registry.register(BuiltInRegistries.ITEM, itemKey, item)
    }
}
