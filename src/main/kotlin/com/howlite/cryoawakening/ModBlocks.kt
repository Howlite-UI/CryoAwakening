package com.howlite.cryoawakening

import com.howlite.cryoawakening.block.CryoTombBlock
import com.howlite.cryoawakening.block.CryoVentBlock
import com.howlite.cryoawakening.block.GalePipeBlock
import com.howlite.cryoawakening.block.GaleTankBlock
import com.howlite.cryoawakening.block.ModBushBlock
import com.howlite.cryoawakening.block.ModButtonBlock
import com.howlite.cryoawakening.block.ModDoorBlock
import com.howlite.cryoawakening.block.ModPressurePlateBlock
import com.howlite.cryoawakening.block.ModStairBlock
import com.howlite.cryoawakening.block.LumeshStemBlock
import com.howlite.cryoawakening.block.PetrifiedLilacLeavesBlock
import com.howlite.cryoawakening.block.ModTrapDoorBlock
import com.howlite.cryoawakening.block.entity.CryoTombBlockEntity
import com.howlite.cryoawakening.block.entity.CryoVentBlockEntity
import com.howlite.cryoawakening.block.entity.GalePipeBlockEntity
import com.howlite.cryoawakening.block.entity.GaleTankBlockEntity
import com.howlite.cryoawakening.block.entity.LumeshStemBlockEntity
import com.howlite.cryoawakening.block.entity.PetrifiedLilacLeavesBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
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
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.GlowLichenBlock
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.properties.WoodType
import net.minecraft.world.level.material.PushReaction

object ModBlocks {

    private fun blockKey(name: String): ResourceKey<Block> =
        ResourceKey.create(Registries.BLOCK, CryoAwakening.id(name))

    private fun itemKey(name: String): ResourceKey<Item> =
        ResourceKey.create(Registries.ITEM, CryoAwakening.id(name))

    // --- 1. Blocs Fonctionnels & Minerais Spéciaux ---
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

    val CRYO_VENT_BLOCK_ENTITY_TYPE: BlockEntityType<CryoVentBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::CryoVentBlockEntity, CRYO_VENT).build()

    val CRYO_TOMB_KEY: ResourceKey<Block> = blockKey("cryo_tomb")
    val CRYO_TOMB_ITEM_KEY: ResourceKey<Item> = itemKey("cryo_tomb")
    val CRYO_TOMB: Block = CryoTombBlock(
        BlockBehaviour.Properties.of()
            .setId(CRYO_TOMB_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.GLASS)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    )
    val CRYO_TOMB_ITEM: Item = BlockItem(CRYO_TOMB, Item.Properties().setId(CRYO_TOMB_ITEM_KEY))

    val CRYO_TOMB_BLOCK_ENTITY_TYPE: BlockEntityType<CryoTombBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::CryoTombBlockEntity, CRYO_TOMB).build()

    val GALE_TANK_KEY: ResourceKey<Block> = blockKey("gale_tank")
    val GALE_TANK_ITEM_KEY: ResourceKey<Item> = itemKey("gale_tank")
    val GALE_TANK: Block = GaleTankBlock(
        BlockBehaviour.Properties.of()
            .setId(GALE_TANK_KEY)
            .strength(3.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    )
    val GALE_TANK_ITEM: Item = BlockItem(GALE_TANK, Item.Properties().setId(GALE_TANK_ITEM_KEY))

    val GALE_TANK_BLOCK_ENTITY_TYPE: BlockEntityType<GaleTankBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::GaleTankBlockEntity, GALE_TANK).build()

    val GALE_PIPE_KEY: ResourceKey<Block> = blockKey("gale_pipe")
    val GALE_PIPE_ITEM_KEY: ResourceKey<Item> = itemKey("gale_pipe")
    val GALE_PIPE: Block = GalePipeBlock(
        BlockBehaviour.Properties.of()
            .setId(GALE_PIPE_KEY)
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    )
    val GALE_PIPE_ITEM: Item = BlockItem(GALE_PIPE, Item.Properties().setId(GALE_PIPE_ITEM_KEY))

    val GALE_PIPE_BLOCK_ENTITY_TYPE: BlockEntityType<GalePipeBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::GalePipeBlockEntity, GALE_PIPE).build()

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

    val TELLURIUM_ORE_KEY: ResourceKey<Block> = blockKey("tellurium_ore")
    val TELLURIUM_ORE_ITEM_KEY: ResourceKey<Item> = itemKey("tellurium_ore")
    val TELLURIUM_ORE: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(TELLURIUM_ORE_KEY)
            .strength(3.0f, 3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
    )
    val TELLURIUM_ORE_ITEM: Item = BlockItem(TELLURIUM_ORE, Item.Properties().setId(TELLURIUM_ORE_ITEM_KEY))

    // --- 2. Roches & Glaces de la Caverne ---
    val FROZEN_FLYSCH_KEY: ResourceKey<Block> = blockKey("frozen_flysch")
    val FROZEN_FLYSCH_ITEM_KEY: ResourceKey<Item> = itemKey("frozen_flysch")
    val FROZEN_FLYSCH: Block = Block(BlockBehaviour.Properties.of().setId(FROZEN_FLYSCH_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val FROZEN_FLYSCH_ITEM: Item = BlockItem(FROZEN_FLYSCH, Item.Properties().setId(FROZEN_FLYSCH_ITEM_KEY))

    val BLUE_FROZEN_FLYSCH_KEY: ResourceKey<Block> = blockKey("blue_frozen_flysch")
    val BLUE_FROZEN_FLYSCH_ITEM_KEY: ResourceKey<Item> = itemKey("blue_frozen_flysch")
    val BLUE_FROZEN_FLYSCH: Block = Block(BlockBehaviour.Properties.of().setId(BLUE_FROZEN_FLYSCH_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val BLUE_FROZEN_FLYSCH_ITEM: Item = BlockItem(BLUE_FROZEN_FLYSCH, Item.Properties().setId(BLUE_FROZEN_FLYSCH_ITEM_KEY))

    val SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("shivering_shale_stone")
    val SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("shivering_shale_stone")
    val SHIVERING_SHALE_STONE: Block = Block(BlockBehaviour.Properties.of().setId(SHIVERING_SHALE_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(SHIVERING_SHALE_STONE, Item.Properties().setId(SHIVERING_SHALE_STONE_ITEM_KEY))

    val GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("gabbro_stone")
    val GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("gabbro_stone")
    val GABBRO_STONE: Block = Block(BlockBehaviour.Properties.of().setId(GABBRO_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val GABBRO_STONE_ITEM: Item = BlockItem(GABBRO_STONE, Item.Properties().setId(GABBRO_STONE_ITEM_KEY))

    val ICE_SHEET_SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("ice_sheet_shivering_shale_stone")
    val ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet_shivering_shale_stone")
    val ICE_SHEET_SHIVERING_SHALE_STONE: Block = Block(BlockBehaviour.Properties.of().setId(ICE_SHEET_SHIVERING_SHALE_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val ICE_SHEET_SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(ICE_SHEET_SHIVERING_SHALE_STONE, Item.Properties().setId(ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY))

    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet_shivering_shale_stone")
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet_shivering_shale_stone")
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE: Block = Block(BlockBehaviour.Properties.of().setId(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM: Item = BlockItem(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE, Item.Properties().setId(BLUE_ICE_SHEET_SHIVERING_SHALE_STONE_ITEM_KEY))

    val ICE_SHEET_GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("ice_sheet_gabbro_stone")
    val ICE_SHEET_GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet_gabbro_stone")
    val ICE_SHEET_GABBRO_STONE: Block = Block(BlockBehaviour.Properties.of().setId(ICE_SHEET_GABBRO_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val ICE_SHEET_GABBRO_STONE_ITEM: Item = BlockItem(ICE_SHEET_GABBRO_STONE, Item.Properties().setId(ICE_SHEET_GABBRO_STONE_ITEM_KEY))

    val BLUE_ICE_SHEET_GABBRO_STONE_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet_gabbro_stone")
    val BLUE_ICE_SHEET_GABBRO_STONE_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet_gabbro_stone")
    val BLUE_ICE_SHEET_GABBRO_STONE: Block = Block(BlockBehaviour.Properties.of().setId(BLUE_ICE_SHEET_GABBRO_STONE_KEY).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())
    val BLUE_ICE_SHEET_GABBRO_STONE_ITEM: Item = BlockItem(BLUE_ICE_SHEET_GABBRO_STONE, Item.Properties().setId(BLUE_ICE_SHEET_GABBRO_STONE_ITEM_KEY))

    val ICE_SHEET_KEY: ResourceKey<Block> = blockKey("ice_sheet")
    val ICE_SHEET_ITEM_KEY: ResourceKey<Item> = itemKey("ice_sheet")
    val ICE_SHEET: Block = GlowLichenBlock(BlockBehaviour.Properties.of().setId(ICE_SHEET_KEY).replaceable().noCollision().strength(0.2f).sound(SoundType.GLASS))
    val ICE_SHEET_ITEM: Item = BlockItem(ICE_SHEET, Item.Properties().setId(ICE_SHEET_ITEM_KEY))

    val BLUE_ICE_SHEET_KEY: ResourceKey<Block> = blockKey("blue_ice_sheet")
    val BLUE_ICE_SHEET_ITEM_KEY: ResourceKey<Item> = itemKey("blue_ice_sheet")
    val BLUE_ICE_SHEET: Block = GlowLichenBlock(BlockBehaviour.Properties.of().setId(BLUE_ICE_SHEET_KEY).replaceable().noCollision().strength(0.2f).sound(SoundType.GLASS))
    val BLUE_ICE_SHEET_ITEM: Item = BlockItem(BLUE_ICE_SHEET, Item.Properties().setId(BLUE_ICE_SHEET_ITEM_KEY))

    // --- 3. Sols de Caverne & Végétation ---
    val RIMECRUST_KEY: ResourceKey<Block> = blockKey("rimecrust")
    val RIMECRUST_ITEM_KEY: ResourceKey<Item> = itemKey("rimecrust")
    val RIMECRUST: Block = Block(BlockBehaviour.Properties.of().setId(RIMECRUST_KEY).strength(0.6f).sound(SoundType.GRAVEL))
    val RIMECRUST_ITEM: Item = BlockItem(RIMECRUST, Item.Properties().setId(RIMECRUST_ITEM_KEY))

    val RIMECRUST_LICHEN_KEY: ResourceKey<Block> = blockKey("rimecrust_lichen")
    val RIMECRUST_LICHEN_ITEM_KEY: ResourceKey<Item> = itemKey("rimecrust_lichen")
    val RIMECRUST_LICHEN: Block = Block(BlockBehaviour.Properties.of().setId(RIMECRUST_LICHEN_KEY).strength(0.7f).sound(SoundType.MOSS))
    val RIMECRUST_LICHEN_ITEM: Item = BlockItem(RIMECRUST_LICHEN, Item.Properties().setId(RIMECRUST_LICHEN_ITEM_KEY))

    val SMALL_LICHEN_BUSH_KEY: ResourceKey<Block> = blockKey("small_lichen_bush")
    val SMALL_LICHEN_BUSH_ITEM_KEY: ResourceKey<Item> = itemKey("small_lichen_bush")
    val SMALL_LICHEN_BUSH: Block = com.howlite.cryoawakening.block.LichenBushBlock(
        BlockBehaviour.Properties.of()
            .setId(SMALL_LICHEN_BUSH_KEY)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    )
    val SMALL_LICHEN_BUSH_ITEM: Item = BlockItem(SMALL_LICHEN_BUSH, Item.Properties().setId(SMALL_LICHEN_BUSH_ITEM_KEY))

    val RIMEBLOOM_KEY: ResourceKey<Block> = blockKey("rimebloom")
    val RIMEBLOOM_ITEM_KEY: ResourceKey<Item> = itemKey("rimebloom")
    val RIMEBLOOM: Block = Block(BlockBehaviour.Properties.of().setId(RIMEBLOOM_KEY).strength(0.6f).sound(SoundType.GRAVEL))
    val RIMEBLOOM_ITEM: Item = BlockItem(RIMEBLOOM, Item.Properties().setId(RIMEBLOOM_ITEM_KEY))

    val RIMEBLOOM_GRASS_KEY: ResourceKey<Block> = blockKey("rimebloom_grass")
    val RIMEBLOOM_GRASS_ITEM_KEY: ResourceKey<Item> = itemKey("rimebloom_grass")
    val RIMEBLOOM_GRASS: Block = Block(BlockBehaviour.Properties.of().setId(RIMEBLOOM_GRASS_KEY).strength(0.7f).sound(SoundType.GRASS))
    val RIMEBLOOM_GRASS_ITEM: Item = BlockItem(RIMEBLOOM_GRASS, Item.Properties().setId(RIMEBLOOM_GRASS_ITEM_KEY))

    // --- 4. Lumesh (Fruits / Blocs Luminescents) ---
    val ORANGE_LUMESH_KEY: ResourceKey<Block> = blockKey("orange_lumesh")
    val ORANGE_LUMESH_ITEM_KEY: ResourceKey<Item> = itemKey("orange_lumesh")
    val ORANGE_LUMESH: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(ORANGE_LUMESH_KEY)
            .strength(1.0f)
            .lightLevel { 12 }
            .sound(SoundType.WOOD)
            .pushReaction(PushReaction.DESTROY)
    )
    val ORANGE_LUMESH_ITEM: Item = BlockItem(ORANGE_LUMESH, Item.Properties().setId(ORANGE_LUMESH_ITEM_KEY))

    val YELLOW_LUMESH_KEY: ResourceKey<Block> = blockKey("yellow_lumesh")
    val YELLOW_LUMESH_ITEM_KEY: ResourceKey<Item> = itemKey("yellow_lumesh")
    val YELLOW_LUMESH: Block = Block(
        BlockBehaviour.Properties.of()
            .setId(YELLOW_LUMESH_KEY)
            .strength(1.0f)
            .lightLevel { 12 }
            .sound(SoundType.WOOD)
            .pushReaction(PushReaction.DESTROY)
    )
    val YELLOW_LUMESH_ITEM: Item = BlockItem(YELLOW_LUMESH, Item.Properties().setId(YELLOW_LUMESH_ITEM_KEY))

    val LUMESH_STEM_KEY: ResourceKey<Block> = blockKey("lumesh_stem")
    val LUMESH_STEM: LumeshStemBlock = LumeshStemBlock(
        BlockBehaviour.Properties.of()
            .setId(LUMESH_STEM_KEY)
            .noCollision()
            .instabreak()
            .randomTicks()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    )

    val LUMESH_STEM_BLOCK_ENTITY_TYPE: BlockEntityType<LumeshStemBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::LumeshStemBlockEntity, LUMESH_STEM).build()

    // --- 5. Ancient Lilac Woodset ---
    val ANCIENT_LILAC_LOG_KEY: ResourceKey<Block> = blockKey("ancient_lilac_log")
    val ANCIENT_LILAC_LOG_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_log")
    val ANCIENT_LILAC_LOG: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_LOG_KEY).strength(2.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_LOG_ITEM: Item = BlockItem(ANCIENT_LILAC_LOG, Item.Properties().setId(ANCIENT_LILAC_LOG_ITEM_KEY))

    val ANCIENT_LILAC_WOOD_KEY: ResourceKey<Block> = blockKey("ancient_lilac_wood")
    val ANCIENT_LILAC_WOOD_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_wood")
    val ANCIENT_LILAC_WOOD: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_WOOD_KEY).strength(2.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_WOOD_ITEM: Item = BlockItem(ANCIENT_LILAC_WOOD, Item.Properties().setId(ANCIENT_LILAC_WOOD_ITEM_KEY))

    val STRIPPED_ANCIENT_LILAC_LOG_KEY: ResourceKey<Block> = blockKey("stripped_ancient_lilac_log")
    val STRIPPED_ANCIENT_LILAC_LOG_ITEM_KEY: ResourceKey<Item> = itemKey("stripped_ancient_lilac_log")
    val STRIPPED_ANCIENT_LILAC_LOG: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(STRIPPED_ANCIENT_LILAC_LOG_KEY).strength(2.0f).sound(SoundType.WOOD))
    val STRIPPED_ANCIENT_LILAC_LOG_ITEM: Item = BlockItem(STRIPPED_ANCIENT_LILAC_LOG, Item.Properties().setId(STRIPPED_ANCIENT_LILAC_LOG_ITEM_KEY))

    val STRIPPED_ANCIENT_LILAC_WOOD_KEY: ResourceKey<Block> = blockKey("stripped_ancient_lilac_wood")
    val STRIPPED_ANCIENT_LILAC_WOOD_ITEM_KEY: ResourceKey<Item> = itemKey("stripped_ancient_lilac_wood")
    val STRIPPED_ANCIENT_LILAC_WOOD: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(STRIPPED_ANCIENT_LILAC_WOOD_KEY).strength(2.0f).sound(SoundType.WOOD))
    val STRIPPED_ANCIENT_LILAC_WOOD_ITEM: Item = BlockItem(STRIPPED_ANCIENT_LILAC_WOOD, Item.Properties().setId(STRIPPED_ANCIENT_LILAC_WOOD_ITEM_KEY))

    val ANCIENT_LILAC_PLANKS_KEY: ResourceKey<Block> = blockKey("ancient_lilac_planks")
    val ANCIENT_LILAC_PLANKS_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_planks")
    val ANCIENT_LILAC_PLANKS: Block = Block(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_PLANKS_KEY).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_PLANKS_ITEM: Item = BlockItem(ANCIENT_LILAC_PLANKS, Item.Properties().setId(ANCIENT_LILAC_PLANKS_ITEM_KEY))

    val ANCIENT_LILAC_STAIRS_KEY: ResourceKey<Block> = blockKey("ancient_lilac_stairs")
    val ANCIENT_LILAC_STAIRS_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_stairs")
    val ANCIENT_LILAC_STAIRS: Block = ModStairBlock(ANCIENT_LILAC_PLANKS.defaultBlockState(), BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_STAIRS_KEY).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_STAIRS_ITEM: Item = BlockItem(ANCIENT_LILAC_STAIRS, Item.Properties().setId(ANCIENT_LILAC_STAIRS_ITEM_KEY))

    val ANCIENT_LILAC_SLAB_KEY: ResourceKey<Block> = blockKey("ancient_lilac_slab")
    val ANCIENT_LILAC_SLAB_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_slab")
    val ANCIENT_LILAC_SLAB: Block = SlabBlock(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_SLAB_KEY).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_SLAB_ITEM: Item = BlockItem(ANCIENT_LILAC_SLAB, Item.Properties().setId(ANCIENT_LILAC_SLAB_ITEM_KEY))

    val ANCIENT_LILAC_FENCE_KEY: ResourceKey<Block> = blockKey("ancient_lilac_fence")
    val ANCIENT_LILAC_FENCE_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_fence")
    val ANCIENT_LILAC_FENCE: Block = FenceBlock(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_FENCE_KEY).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_FENCE_ITEM: Item = BlockItem(ANCIENT_LILAC_FENCE, Item.Properties().setId(ANCIENT_LILAC_FENCE_ITEM_KEY))

    val ANCIENT_LILAC_FENCE_GATE_KEY: ResourceKey<Block> = blockKey("ancient_lilac_fence_gate")
    val ANCIENT_LILAC_FENCE_GATE_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_fence_gate")
    val ANCIENT_LILAC_FENCE_GATE: Block = FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_FENCE_GATE_KEY).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_FENCE_GATE_ITEM: Item = BlockItem(ANCIENT_LILAC_FENCE_GATE, Item.Properties().setId(ANCIENT_LILAC_FENCE_GATE_ITEM_KEY))

    val ANCIENT_LILAC_DOOR_KEY: ResourceKey<Block> = blockKey("ancient_lilac_door")
    val ANCIENT_LILAC_DOOR_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_door")
    val ANCIENT_LILAC_DOOR: Block = ModDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_DOOR_KEY).strength(3.0f).sound(SoundType.WOOD).noOcclusion())
    val ANCIENT_LILAC_DOOR_ITEM: Item = BlockItem(ANCIENT_LILAC_DOOR, Item.Properties().setId(ANCIENT_LILAC_DOOR_ITEM_KEY))

    val ANCIENT_LILAC_TRAPDOOR_KEY: ResourceKey<Block> = blockKey("ancient_lilac_trapdoor")
    val ANCIENT_LILAC_TRAPDOOR_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_trapdoor")
    val ANCIENT_LILAC_TRAPDOOR: Block = ModTrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_TRAPDOOR_KEY).strength(3.0f).sound(SoundType.WOOD).noOcclusion())
    val ANCIENT_LILAC_TRAPDOOR_ITEM: Item = BlockItem(ANCIENT_LILAC_TRAPDOOR, Item.Properties().setId(ANCIENT_LILAC_TRAPDOOR_ITEM_KEY))

    val ANCIENT_LILAC_PRESSURE_PLATE_KEY: ResourceKey<Block> = blockKey("ancient_lilac_pressure_plate")
    val ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_pressure_plate")
    val ANCIENT_LILAC_PRESSURE_PLATE: Block = ModPressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_PRESSURE_PLATE_KEY).strength(0.5f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_PRESSURE_PLATE_ITEM: Item = BlockItem(ANCIENT_LILAC_PRESSURE_PLATE, Item.Properties().setId(ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY))

    val ANCIENT_LILAC_BUTTON_KEY: ResourceKey<Block> = blockKey("ancient_lilac_button")
    val ANCIENT_LILAC_BUTTON_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_button")
    val ANCIENT_LILAC_BUTTON: Block = ModButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_BUTTON_KEY).strength(0.5f).sound(SoundType.WOOD))
    val ANCIENT_LILAC_BUTTON_ITEM: Item = BlockItem(ANCIENT_LILAC_BUTTON, Item.Properties().setId(ANCIENT_LILAC_BUTTON_ITEM_KEY))

    val ANCIENT_LILAC_LEAVES_KEY: ResourceKey<Block> = blockKey("ancient_lilac_leaves")
    val ANCIENT_LILAC_LEAVES_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_leaves")
    val ANCIENT_LILAC_LEAVES: Block = com.howlite.cryoawakening.block.ModLeavesBlock(BlockBehaviour.Properties.of().setId(ANCIENT_LILAC_LEAVES_KEY).strength(0.2f).sound(SoundType.GRASS).noOcclusion())
    val ANCIENT_LILAC_LEAVES_ITEM: Item = BlockItem(ANCIENT_LILAC_LEAVES, Item.Properties().setId(ANCIENT_LILAC_LEAVES_ITEM_KEY))

    val ANCIENT_LILAC_SAPLING_KEY: ResourceKey<Block> = blockKey("ancient_lilac_sapling")
    val ANCIENT_LILAC_SAPLING_ITEM_KEY: ResourceKey<Item> = itemKey("ancient_lilac_sapling")
    val ANCIENT_LILAC_SAPLING: Block = com.howlite.cryoawakening.block.ModSaplingBlock(
        BlockBehaviour.Properties.of()
            .setId(ANCIENT_LILAC_SAPLING_KEY)
            .noCollision()
            .instabreak()
            .randomTicks()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    )
    val ANCIENT_LILAC_SAPLING_ITEM: Item = BlockItem(ANCIENT_LILAC_SAPLING, Item.Properties().setId(ANCIENT_LILAC_SAPLING_ITEM_KEY))

    // --- 6. Petrified Ancient Lilac Woodset ---
    val PETRIFIED_ANCIENT_LILAC_LOG_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_log")
    val PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_log")
    val PETRIFIED_ANCIENT_LILAC_LOG: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_LOG_KEY).strength(2.5f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_LOG_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_LOG, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_WOOD_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_wood")
    val PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_wood")
    val PETRIFIED_ANCIENT_LILAC_WOOD: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_WOOD_KEY).strength(2.5f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_WOOD_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_WOOD, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY))

    val STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_KEY: ResourceKey<Block> = blockKey("stripped_petrified_ancient_lilac_log")
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY: ResourceKey<Item> = itemKey("stripped_petrified_ancient_lilac_log")
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_KEY).strength(2.5f).sound(SoundType.STONE))
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM: Item = BlockItem(STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG, Item.Properties().setId(STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY))

    val STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_KEY: ResourceKey<Block> = blockKey("stripped_petrified_ancient_lilac_wood")
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY: ResourceKey<Item> = itemKey("stripped_petrified_ancient_lilac_wood")
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD: Block = RotatedPillarBlock(BlockBehaviour.Properties.of().setId(STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_KEY).strength(2.5f).sound(SoundType.STONE))
    val STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM: Item = BlockItem(STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD, Item.Properties().setId(STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_PLANKS_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_planks")
    val PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_planks")
    val PETRIFIED_ANCIENT_LILAC_PLANKS: Block = Block(BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_PLANKS_KEY).strength(2.5f, 4.0f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_PLANKS, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_STAIRS_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_stairs")
    val PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_stairs")
    val PETRIFIED_ANCIENT_LILAC_STAIRS: Block = ModStairBlock(PETRIFIED_ANCIENT_LILAC_PLANKS.defaultBlockState(), BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_STAIRS_KEY).strength(2.5f, 4.0f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_STAIRS, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_SLAB_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_slab")
    val PETRIFIED_ANCIENT_LILAC_SLAB_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_slab")
    val PETRIFIED_ANCIENT_LILAC_SLAB: Block = SlabBlock(BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_SLAB_KEY).strength(2.5f, 4.0f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_SLAB_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_SLAB, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_SLAB_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_FENCE_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_fence")
    val PETRIFIED_ANCIENT_LILAC_FENCE_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_fence")
    val PETRIFIED_ANCIENT_LILAC_FENCE: Block = FenceBlock(BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_FENCE_KEY).strength(2.5f, 4.0f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_FENCE_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_FENCE, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_FENCE_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_FENCE_GATE_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_fence_gate")
    val PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_fence_gate")
    val PETRIFIED_ANCIENT_LILAC_FENCE_GATE: Block = FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_FENCE_GATE_KEY).strength(2.5f, 4.0f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_FENCE_GATE, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_DOOR_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_door")
    val PETRIFIED_ANCIENT_LILAC_DOOR_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_door")
    val PETRIFIED_ANCIENT_LILAC_DOOR: Block = ModDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_DOOR_KEY).strength(3.5f).sound(SoundType.STONE).noOcclusion())
    val PETRIFIED_ANCIENT_LILAC_DOOR_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_DOOR, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_DOOR_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_TRAPDOOR_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_trapdoor")
    val PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_trapdoor")
    val PETRIFIED_ANCIENT_LILAC_TRAPDOOR: Block = ModTrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_TRAPDOOR_KEY).strength(3.5f).sound(SoundType.STONE).noOcclusion())
    val PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_TRAPDOOR, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_pressure_plate")
    val PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_pressure_plate")
    val PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE: Block = ModPressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_KEY).strength(0.8f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY))

    val PETRIFIED_ANCIENT_LILAC_BUTTON_KEY: ResourceKey<Block> = blockKey("petrified_ancient_lilac_button")
    val PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_ancient_lilac_button")
    val PETRIFIED_ANCIENT_LILAC_BUTTON: Block = ModButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of().setId(PETRIFIED_ANCIENT_LILAC_BUTTON_KEY).strength(0.8f).sound(SoundType.STONE))
    val PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM: Item = BlockItem(PETRIFIED_ANCIENT_LILAC_BUTTON, Item.Properties().setId(PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM_KEY))

    val PETRIFIED_LILAC_LEAVES_KEY: ResourceKey<Block> = blockKey("petrified_lilac_leaves")
    val PETRIFIED_LILAC_LEAVES_ITEM_KEY: ResourceKey<Item> = itemKey("petrified_lilac_leaves")
    val PETRIFIED_LILAC_LEAVES: Block = PetrifiedLilacLeavesBlock(BlockBehaviour.Properties.of().setId(PETRIFIED_LILAC_LEAVES_KEY).strength(0.2f).sound(SoundType.GRASS).noOcclusion())
    val PETRIFIED_LILAC_LEAVES_ITEM: Item = BlockItem(PETRIFIED_LILAC_LEAVES, Item.Properties().setId(PETRIFIED_LILAC_LEAVES_ITEM_KEY))

    val PETRIFIED_LILAC_LEAVES_BLOCK_ENTITY_TYPE: BlockEntityType<PetrifiedLilacLeavesBlockEntity> =
        FabricBlockEntityTypeBuilder.create(::PetrifiedLilacLeavesBlockEntity, PETRIFIED_LILAC_LEAVES).build()

    // --- Onglet Créatif Dédié "Cryo Awakening" ---
    val CRYO_AWAKENING_TAB_KEY: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        CryoAwakening.id("cryo_awakening")
    )

    val CRYO_AWAKENING_TAB: CreativeModeTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .icon { ItemStack(CRYO_VENT_ITEM) }
        .title(Component.translatable("itemGroup.cryo-awakening.cryo_awakening"))
        .displayItems { itemDisplayParameters, output ->
            // Mécanismes & Glaces
            output.accept(CRYO_VENT_ITEM)
            output.accept(CRYO_TOMB_ITEM)
            output.accept(GALE_TANK_ITEM)
            output.accept(GALE_PIPE_ITEM)
            output.accept(BISMUTH_ORE_SHIVERING_SHALE_ITEM)
            output.accept(TELLURIUM_ORE_ITEM)
            output.accept(com.howlite.cryoawakening.item.ModItems.RAW_BISMUTH)
            output.accept(com.howlite.cryoawakening.item.ModItems.RAW_TELLURIUM)
            output.accept(com.howlite.cryoawakening.item.ModItems.TELLUROBISMUTHITE_INGOT)
            output.accept(com.howlite.cryoawakening.item.ModItems.BISMUTH_DROSS)
            output.accept(com.howlite.cryoawakening.item.ModItems.DROSS_GLASS_LENS)
            output.accept(com.howlite.cryoawakening.item.ModItems.KALEIDOSCOPE)
            output.accept(com.howlite.cryoawakening.item.ModItems.WRENCH)
            output.accept(com.howlite.cryoawakening.item.ModItems.GALE_MONOCLE)
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

            // Sols de caverne & Flore
            output.accept(RIMECRUST_ITEM)
            output.accept(RIMECRUST_LICHEN_ITEM)
            output.accept(RIMEBLOOM_ITEM)
            output.accept(RIMEBLOOM_GRASS_ITEM)
            output.accept(SMALL_LICHEN_BUSH_ITEM)

            // Lumesh (Blocs, Graines, Fruits)
            output.accept(ORANGE_LUMESH_ITEM)
            output.accept(YELLOW_LUMESH_ITEM)
            output.accept(com.howlite.cryoawakening.item.ModItems.LUMESH_SEED)
            output.accept(com.howlite.cryoawakening.item.ModItems.ORANGE_LUMESH_SLICE)
            output.accept(com.howlite.cryoawakening.item.ModItems.ORANGE_LUMESH_HULL)
            output.accept(com.howlite.cryoawakening.item.ModItems.YELLOW_LUMESH_SLICE)
            output.accept(com.howlite.cryoawakening.item.ModItems.YELLOW_LUMESH_HULL)

            // Set Ancient Lilac
            output.accept(ANCIENT_LILAC_LOG_ITEM)
            output.accept(ANCIENT_LILAC_WOOD_ITEM)
            output.accept(STRIPPED_ANCIENT_LILAC_LOG_ITEM)
            output.accept(STRIPPED_ANCIENT_LILAC_WOOD_ITEM)
            output.accept(ANCIENT_LILAC_PLANKS_ITEM)
            output.accept(ANCIENT_LILAC_STAIRS_ITEM)
            output.accept(ANCIENT_LILAC_SLAB_ITEM)
            output.accept(ANCIENT_LILAC_FENCE_ITEM)
            output.accept(ANCIENT_LILAC_FENCE_GATE_ITEM)
            output.accept(ANCIENT_LILAC_DOOR_ITEM)
            output.accept(ANCIENT_LILAC_TRAPDOOR_ITEM)
            output.accept(ANCIENT_LILAC_PRESSURE_PLATE_ITEM)
            output.accept(ANCIENT_LILAC_BUTTON_ITEM)
            output.accept(ANCIENT_LILAC_LEAVES_ITEM)
            output.accept(ANCIENT_LILAC_SAPLING_ITEM)

            // Set Petrified Ancient Lilac
            output.accept(PETRIFIED_ANCIENT_LILAC_LOG_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_WOOD_ITEM)
            output.accept(STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM)
            output.accept(STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_SLAB_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_FENCE_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_DOOR_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM)
            output.accept(PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM)
            output.accept(PETRIFIED_LILAC_LEAVES_ITEM)
            output.accept(com.howlite.cryoawakening.item.ModItems.FOSSILIZED_LILAC_LEAF)

            // Armures Cryo Awakening
            com.howlite.cryoawakening.item.ModItems.ALL_ARMOR_ITEMS.forEach { armorItem ->
                output.accept(armorItem)
            }

            // Entités & Armes
            output.accept(com.howlite.cryoawakening.item.ModItems.GAWKER_SPAWN_EGG)
            output.accept(com.howlite.cryoawakening.item.ModItems.GAWKER_FUR)
            output.accept(com.howlite.cryoawakening.item.ModItems.GAWK_BOMB)
            output.accept(com.howlite.cryoawakening.item.ModItems.GALE_BOOMERANG)

            // Livres enchantés du Gale Boomerang
            val enchantRegistry = itemDisplayParameters.holders().lookup(Registries.ENCHANTMENT).orElse(null)
            if (enchantRegistry != null) {
                val allEnchants = listOf(
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.HEAVYWEIGHT, 3),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.ZEPHYR, 3),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.RICOCHET, 4),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.HAWKEYE, 5),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.SOAR, 3),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.FROSTWIND, 2),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.ORBIT, 3),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.RETRIEVAL, 1),
                    Pair(com.howlite.cryoawakening.enchantment.ModEnchantments.GALE_VORTEX, 3)
                )
                for ((key, maxLvl) in allEnchants) {
                    val holder = enchantRegistry.get(key).orElse(null) ?: continue
                    for (lvl in 1..maxLvl) {
                        output.accept(
                            net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(
                                net.minecraft.world.item.enchantment.EnchantmentInstance(holder, lvl)
                            )
                        )
                    }
                }
            }
        }
        .build()

    fun register() {
        // Enregistrement des blocs et items de base
        registerBlock(CRYO_VENT_KEY, CRYO_VENT, CRYO_VENT_ITEM_KEY, CRYO_VENT_ITEM)
        registerBlock(CRYO_TOMB_KEY, CRYO_TOMB, CRYO_TOMB_ITEM_KEY, CRYO_TOMB_ITEM)
        registerBlock(GALE_TANK_KEY, GALE_TANK, GALE_TANK_ITEM_KEY, GALE_TANK_ITEM)
        registerBlock(GALE_PIPE_KEY, GALE_PIPE, GALE_PIPE_ITEM_KEY, GALE_PIPE_ITEM)
        registerBlock(BISMUTH_ORE_SHIVERING_SHALE_KEY, BISMUTH_ORE_SHIVERING_SHALE, BISMUTH_ORE_SHIVERING_SHALE_ITEM_KEY, BISMUTH_ORE_SHIVERING_SHALE_ITEM)
        registerBlock(TELLURIUM_ORE_KEY, TELLURIUM_ORE, TELLURIUM_ORE_ITEM_KEY, TELLURIUM_ORE_ITEM)
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

        // Sols & Flore
        registerBlock(RIMECRUST_KEY, RIMECRUST, RIMECRUST_ITEM_KEY, RIMECRUST_ITEM)
        registerBlock(RIMECRUST_LICHEN_KEY, RIMECRUST_LICHEN, RIMECRUST_LICHEN_ITEM_KEY, RIMECRUST_LICHEN_ITEM)
        registerBlock(RIMEBLOOM_KEY, RIMEBLOOM, RIMEBLOOM_ITEM_KEY, RIMEBLOOM_ITEM)
        registerBlock(RIMEBLOOM_GRASS_KEY, RIMEBLOOM_GRASS, RIMEBLOOM_GRASS_ITEM_KEY, RIMEBLOOM_GRASS_ITEM)
        registerBlock(SMALL_LICHEN_BUSH_KEY, SMALL_LICHEN_BUSH, SMALL_LICHEN_BUSH_ITEM_KEY, SMALL_LICHEN_BUSH_ITEM)

        // Lumesh
        registerBlock(ORANGE_LUMESH_KEY, ORANGE_LUMESH, ORANGE_LUMESH_ITEM_KEY, ORANGE_LUMESH_ITEM)
        registerBlock(YELLOW_LUMESH_KEY, YELLOW_LUMESH, YELLOW_LUMESH_ITEM_KEY, YELLOW_LUMESH_ITEM)
        Registry.register(BuiltInRegistries.BLOCK, LUMESH_STEM_KEY, LUMESH_STEM)

        // Ancient Lilac
        registerBlock(ANCIENT_LILAC_LOG_KEY, ANCIENT_LILAC_LOG, ANCIENT_LILAC_LOG_ITEM_KEY, ANCIENT_LILAC_LOG_ITEM)
        registerBlock(ANCIENT_LILAC_WOOD_KEY, ANCIENT_LILAC_WOOD, ANCIENT_LILAC_WOOD_ITEM_KEY, ANCIENT_LILAC_WOOD_ITEM)
        registerBlock(STRIPPED_ANCIENT_LILAC_LOG_KEY, STRIPPED_ANCIENT_LILAC_LOG, STRIPPED_ANCIENT_LILAC_LOG_ITEM_KEY, STRIPPED_ANCIENT_LILAC_LOG_ITEM)
        registerBlock(STRIPPED_ANCIENT_LILAC_WOOD_KEY, STRIPPED_ANCIENT_LILAC_WOOD, STRIPPED_ANCIENT_LILAC_WOOD_ITEM_KEY, STRIPPED_ANCIENT_LILAC_WOOD_ITEM)
        registerBlock(ANCIENT_LILAC_PLANKS_KEY, ANCIENT_LILAC_PLANKS, ANCIENT_LILAC_PLANKS_ITEM_KEY, ANCIENT_LILAC_PLANKS_ITEM)
        registerBlock(ANCIENT_LILAC_STAIRS_KEY, ANCIENT_LILAC_STAIRS, ANCIENT_LILAC_STAIRS_ITEM_KEY, ANCIENT_LILAC_STAIRS_ITEM)
        registerBlock(ANCIENT_LILAC_SLAB_KEY, ANCIENT_LILAC_SLAB, ANCIENT_LILAC_SLAB_ITEM_KEY, ANCIENT_LILAC_SLAB_ITEM)
        registerBlock(ANCIENT_LILAC_FENCE_KEY, ANCIENT_LILAC_FENCE, ANCIENT_LILAC_FENCE_ITEM_KEY, ANCIENT_LILAC_FENCE_ITEM)
        registerBlock(ANCIENT_LILAC_FENCE_GATE_KEY, ANCIENT_LILAC_FENCE_GATE, ANCIENT_LILAC_FENCE_GATE_ITEM_KEY, ANCIENT_LILAC_FENCE_GATE_ITEM)
        registerBlock(ANCIENT_LILAC_DOOR_KEY, ANCIENT_LILAC_DOOR, ANCIENT_LILAC_DOOR_ITEM_KEY, ANCIENT_LILAC_DOOR_ITEM)
        registerBlock(ANCIENT_LILAC_TRAPDOOR_KEY, ANCIENT_LILAC_TRAPDOOR, ANCIENT_LILAC_TRAPDOOR_ITEM_KEY, ANCIENT_LILAC_TRAPDOOR_ITEM)
        registerBlock(ANCIENT_LILAC_PRESSURE_PLATE_KEY, ANCIENT_LILAC_PRESSURE_PLATE, ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY, ANCIENT_LILAC_PRESSURE_PLATE_ITEM)
        registerBlock(ANCIENT_LILAC_BUTTON_KEY, ANCIENT_LILAC_BUTTON, ANCIENT_LILAC_BUTTON_ITEM_KEY, ANCIENT_LILAC_BUTTON_ITEM)
        registerBlock(ANCIENT_LILAC_LEAVES_KEY, ANCIENT_LILAC_LEAVES, ANCIENT_LILAC_LEAVES_ITEM_KEY, ANCIENT_LILAC_LEAVES_ITEM)
        registerBlock(ANCIENT_LILAC_SAPLING_KEY, ANCIENT_LILAC_SAPLING, ANCIENT_LILAC_SAPLING_ITEM_KEY, ANCIENT_LILAC_SAPLING_ITEM)

        // Petrified Ancient Lilac
        registerBlock(PETRIFIED_ANCIENT_LILAC_LOG_KEY, PETRIFIED_ANCIENT_LILAC_LOG, PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_LOG_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_WOOD_KEY, PETRIFIED_ANCIENT_LILAC_WOOD, PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_WOOD_ITEM)
        registerBlock(STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_KEY, STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG, STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM_KEY, STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG_ITEM)
        registerBlock(STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_KEY, STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD, STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM_KEY, STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_PLANKS_KEY, PETRIFIED_ANCIENT_LILAC_PLANKS, PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_PLANKS_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_STAIRS_KEY, PETRIFIED_ANCIENT_LILAC_STAIRS, PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_STAIRS_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_SLAB_KEY, PETRIFIED_ANCIENT_LILAC_SLAB, PETRIFIED_ANCIENT_LILAC_SLAB_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_SLAB_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_FENCE_KEY, PETRIFIED_ANCIENT_LILAC_FENCE, PETRIFIED_ANCIENT_LILAC_FENCE_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_FENCE_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_FENCE_GATE_KEY, PETRIFIED_ANCIENT_LILAC_FENCE_GATE, PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_FENCE_GATE_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_DOOR_KEY, PETRIFIED_ANCIENT_LILAC_DOOR, PETRIFIED_ANCIENT_LILAC_DOOR_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_DOOR_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_TRAPDOOR_KEY, PETRIFIED_ANCIENT_LILAC_TRAPDOOR, PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_TRAPDOOR_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_KEY, PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE, PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_PRESSURE_PLATE_ITEM)
        registerBlock(PETRIFIED_ANCIENT_LILAC_BUTTON_KEY, PETRIFIED_ANCIENT_LILAC_BUTTON, PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM_KEY, PETRIFIED_ANCIENT_LILAC_BUTTON_ITEM)
        registerBlock(PETRIFIED_LILAC_LEAVES_KEY, PETRIFIED_LILAC_LEAVES, PETRIFIED_LILAC_LEAVES_ITEM_KEY, PETRIFIED_LILAC_LEAVES_ITEM)

        // Strippable Logs & Woods (Écorçage à la hache)
        StrippableBlockRegistry.register(ANCIENT_LILAC_LOG, STRIPPED_ANCIENT_LILAC_LOG)
        StrippableBlockRegistry.register(ANCIENT_LILAC_WOOD, STRIPPED_ANCIENT_LILAC_WOOD)
        StrippableBlockRegistry.register(PETRIFIED_ANCIENT_LILAC_LOG, STRIPPED_PETRIFIED_ANCIENT_LILAC_LOG)
        StrippableBlockRegistry.register(PETRIFIED_ANCIENT_LILAC_WOOD, STRIPPED_PETRIFIED_ANCIENT_LILAC_WOOD)

        // BlockEntityType
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("cryo_vent"),
            CRYO_VENT_BLOCK_ENTITY_TYPE
        )
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("cryo_tomb"),
            CRYO_TOMB_BLOCK_ENTITY_TYPE
        )
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("lumesh_stem"),
            LUMESH_STEM_BLOCK_ENTITY_TYPE
        )
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("petrified_lilac_leaves"),
            PETRIFIED_LILAC_LEAVES_BLOCK_ENTITY_TYPE
        )
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("gale_tank"),
            GALE_TANK_BLOCK_ENTITY_TYPE
        )
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CryoAwakening.id("gale_pipe"),
            GALE_PIPE_BLOCK_ENTITY_TYPE
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
