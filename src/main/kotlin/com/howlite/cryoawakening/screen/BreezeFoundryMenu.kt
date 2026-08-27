package com.howlite.cryoawakening.screen

import com.howlite.cryoawakening.recipe.BreezeFoundryRecipes
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Menu / Container pour la Breeze Foundry (Fonderie de Bourrasque).
 */
class BreezeFoundryMenu(
    containerId: Int,
    playerInventory: Inventory,
    val container: Container,
    val data: ContainerData
) : AbstractContainerMenu(ModMenuTypes.BREEZE_FOUNDRY, containerId) {

    // Constructeur client
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(3),
        SimpleContainerData(4)
    )

    init {
        checkContainerSize(container, 3)
        checkContainerDataCount(data, 4)

        // Slot 0 : Entrée Haute (x = 57, y = 24)
        addSlot(Slot(container, 0, 57, 24))
        // Slot 1 : Entrée Basse (x = 57, y = 46)
        addSlot(Slot(container, 1, 57, 46))
        // Slot 2 : Sortie Lingot (x = 116, y = 35)
        addSlot(BreezeFoundryOutputSlot(playerInventory.player, container, 2, 116, 35))

        // Inventaire du joueur (3 lignes de 9 slots)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18))
            }
        }

        // Barre d'action rapide (Hotbar - 9 slots)
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 143))
        }

        addDataSlots(data)
    }

    val wind: Int
        get() = data.get(0)

    val windCapacity: Int
        get() = data.get(1)

    val cookTime: Int
        get() = data.get(2)

    val cookTimeTotal: Int
        get() = data.get(3)

    val isLit: Boolean
        get() = cookTime > 0

    fun getCookProgress(pixels: Int): Int {
        val total = if (cookTimeTotal != 0) cookTimeTotal else 100
        return (cookTime * pixels) / total
    }

    fun getWindRatio(): Float {
        val cap = if (windCapacity != 0) windCapacity else 10000
        return (wind.toFloat() / cap.toFloat()).coerceIn(0.0f, 1.0f)
    }

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val itemStack2 = slot.item
            itemStack = itemStack2.copy()

            if (index == 2) {
                // Transfert depuis le slot de sortie vers l'inventaire du joueur
                if (!moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY
                }
                slot.onQuickCraft(itemStack2, itemStack)
            } else if (index < 2) {
                // Transfert depuis les slots d'entrée vers l'inventaire
                if (!moveItemStackTo(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Transfert depuis l'inventaire vers la machine
                if (BreezeFoundryRecipes.isValidIngredient(itemStack2)) {
                    if (!moveItemStackTo(itemStack2, 0, 2, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (index in 3..29) {
                    if (!moveItemStackTo(itemStack2, 30, 39, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (index in 30..38 && !moveItemStackTo(itemStack2, 3, 30, false)) {
                    return ItemStack.EMPTY
                }
            }

            if (itemStack2.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            if (itemStack2.count == itemStack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(player, itemStack2)
        }
        return itemStack
    }

    class BreezeFoundryOutputSlot(
        val player: Player,
        container: Container,
        slot: Int,
        x: Int,
        y: Int
    ) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
}
