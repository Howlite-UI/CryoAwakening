package com.howlite.cryoawakening.screen

import com.howlite.cryoawakening.item.ModItems
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
        SimpleContainer(4),
        SimpleContainerData(4)
    )

    init {
        checkContainerSize(container, 4)
        checkContainerDataCount(data, 4)

        // Slot 0 : Entrée 1 (Raw Bismuth)
        addSlot(Slot(container, 0, 45, 17))
        // Slot 1 : Entrée 2 (Raw Tellurium)
        addSlot(Slot(container, 1, 68, 17))
        // Slot 2 : Carburant / Catalyseur
        addSlot(Slot(container, 2, 56, 53))
        // Slot 3 : Sortie (Lingot de Tellurobismuthite)
        addSlot(BreezeFoundryOutputSlot(playerInventory.player, container, 3, 116, 35))

        // Inventaire du joueur (3 lignes de 9 slots)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }

        // Barre d'action rapide (Hotbar - 9 slots)
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
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

            if (index == 3) {
                // Transfert depuis le slot de sortie vers l'inventaire du joueur
                if (!moveItemStackTo(itemStack2, 4, 40, true)) {
                    return ItemStack.EMPTY
                }
                slot.onQuickCraft(itemStack2, itemStack)
            } else if (index < 4) {
                // Transfert depuis les slots d'entrée/carburant vers l'inventaire
                if (!moveItemStackTo(itemStack2, 4, 40, false)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Transfert depuis l'inventaire vers la machine
                if (itemStack2.`is`(ModItems.RAW_BISMUTH)) {
                    if (!moveItemStackTo(itemStack2, 0, 1, false) && !moveItemStackTo(itemStack2, 1, 2, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (itemStack2.`is`(ModItems.RAW_TELLURIUM)) {
                    if (!moveItemStackTo(itemStack2, 1, 2, false) && !moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (index in 4..30) {
                    if (!moveItemStackTo(itemStack2, 31, 40, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (index in 31..39 && !moveItemStackTo(itemStack2, 4, 31, false)) {
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
