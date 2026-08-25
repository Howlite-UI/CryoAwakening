package com.howlite.cryoawakening.item

import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.equipment.Equippable

/**
 * GaleMonocleItem
 *
 * Monocle mécanique équipé à la place du casque (EquipmentSlot.HEAD).
 * Permet de visualiser en surimpression HUD les flux et le niveau de stockage de vent des machines (Gale Tank, Cryo-Vent).
 */
class GaleMonocleItem(properties: Properties) : Item(
    properties.component(
        DataComponents.EQUIPPABLE,
        Equippable.builder(EquipmentSlot.HEAD)
            .setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC)
            .build()
    )
)
