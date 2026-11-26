package com.howlite.cryoawakening.infiltration

import net.minecraft.world.entity.player.Player

/**
 * Pont léger (sans dépendance directe aux classes client) permettant aux mixins communs
 * (PlayerInfiltrationMixin, InventoryMixin) de savoir si le joueur local est en cours d'infiltration.
 */
object InfiltrationClientBridge {
    @Volatile
    var clientInfiltratingCheck: ((Player) -> Boolean)? = null

    @JvmStatic
    fun isClientPlayerInfiltrating(player: Player): Boolean {
        return clientInfiltratingCheck?.invoke(player) ?: false
    }
}
