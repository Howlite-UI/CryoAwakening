package com.howlite.cryoawakening.block.entity

import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.energy.WindStorage
import com.howlite.cryoawakening.recipe.BreezeFoundryRecipes
import com.howlite.cryoawakening.screen.BreezeFoundryMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * BlockEntity pour la Breeze Foundry (Fonderie de Bourrasque).
 *
 * Fonctionnement :
 * - Combine deux ingrédients grâce à la puissance du Vent.
 * - Système de recettes dynamique via BreezeFoundryRecipes.
 */
class BreezeFoundryBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.BREEZE_FOUNDRY_BLOCK_ENTITY_TYPE, pos, state),
    WorldlyContainer, IWindHolder, MenuProvider {

    val items: NonNullList<ItemStack> = NonNullList.withSize(3, ItemStack.EMPTY)
    val windStorage = WindStorage(capacity = 10_000, maxReceive = 100, maxExtract = 100)

    var cookTime: Int = 0
    var cookTimeTotal: Int = 100

    val dataAccess: ContainerData = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            0 -> windStorage.wind
            1 -> windStorage.capacity
            2 -> cookTime
            3 -> cookTimeTotal
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> windStorage.wind = value
                1 -> windStorage.capacity = value
                2 -> cookTime = value
                3 -> cookTimeTotal = value
            }
        }

        override fun getCount(): Int = 4
    }

    override fun getWindStorage(side: Direction?): WindStorage = windStorage

    override fun getContainerSize(): Int = items.size

    override fun isEmpty(): Boolean = items.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val stack = ContainerHelper.removeItem(items, slot, amount)
        if (!stack.isEmpty) setChanged()
        return stack
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack =
        ContainerHelper.takeItem(items, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        stack.limitSize(maxStackSize)
        setChanged()
    }

    override fun stillValid(player: Player): Boolean =
        Container.stillValidBlockEntity(this, player)

    override fun clearContent() {
        items.clear()
        setChanged()
    }

    override fun getDisplayName(): Component =
        Component.translatable("container.cryo-awakening.breeze_foundry")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        BreezeFoundryMenu(containerId, playerInventory, this, dataAccess)

    // Gestion de l'automatisation par entonnoirs / hoppers
    override fun getSlotsForFace(side: Direction): IntArray = when (side) {
        Direction.UP -> intArrayOf(0)       // Dessus -> Entrée 1 (Top Input)
        Direction.DOWN -> intArrayOf(2)     // Dessous -> Sortie (Output)
        else -> intArrayOf(1)               // Côtés -> Entrée 2 (Bottom Input)
    }

    override fun canPlaceItemThroughFace(index: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        if (index == 2) return false
        return BreezeFoundryRecipes.isValidIngredient(itemStack)
    }

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean =
        index == 2

    companion object {
        const val WIND_PER_TICK: Int = 2

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, be: BreezeFoundryBlockEntity) {
            val in1 = be.items[0]
            val in2 = be.items[1]
            val out = be.items[2]

            val recipe = BreezeFoundryRecipes.findRecipe(in1, in2)

            if (recipe != null) {
                val canFitOutput = out.isEmpty ||
                        (ItemStack.isSameItemSameComponents(out, recipe.output) && out.count + recipe.output.count <= out.maxStackSize)
                val hasEnoughWind = be.windStorage.wind >= WIND_PER_TICK

                be.cookTimeTotal = recipe.cookTimeTicks

                if (canFitOutput && hasEnoughWind) {
                    be.windStorage.wind -= WIND_PER_TICK
                    be.cookTime++

                    if (be.cookTime >= be.cookTimeTotal) {
                        be.cookTime = 0
                        in1.shrink(1)
                        in2.shrink(1)

                        if (out.isEmpty) {
                            be.items[2] = recipe.output.copy()
                        } else {
                            out.grow(recipe.output.count)
                        }
                        level.sendBlockUpdated(pos, state, state, 2)
                    }
                    be.setChanged()
                } else {
                    if (be.cookTime > 0) {
                        be.cookTime = maxOf(0, be.cookTime - 2)
                        be.setChanged()
                    }
                }
            } else {
                if (be.cookTime > 0) {
                    be.cookTime = maxOf(0, be.cookTime - 2)
                    be.setChanged()
                }
            }
        }
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putInt("cook_time", cookTime)
        output.putInt("cook_time_total", cookTimeTotal)
        windStorage.save(output)
        ContainerHelper.saveAllItems(output, items)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        cookTime = input.getInt("cook_time").orElse(0)
        cookTimeTotal = input.getInt("cook_time_total").orElse(100)
        windStorage.load(input)
        ContainerHelper.loadAllItems(input, items)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("wind_amount", windStorage.wind)
        tag.putInt("wind_capacity", windStorage.capacity)
        tag.putInt("cook_time", cookTime)
        tag.putInt("cook_time_total", cookTimeTotal)
        return tag
    }
}
