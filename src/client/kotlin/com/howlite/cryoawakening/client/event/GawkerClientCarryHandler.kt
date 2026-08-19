package com.howlite.cryoawakening.client.event

import com.howlite.cryoawakening.entity.GawkerEntity
import com.howlite.cryoawakening.network.ThrowGawkerPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import kotlin.math.min

/**
 * GawkerClientCarryHandler
 *
 * Gère la détection du maintien d'un NOUVEAU Clic Droit (keyUse) côté client pour charger
 * la force de projection du Gawker, l'affichage HUD de la jauge d'armement
 * sur l'Action Bar, et l'envoi du paquet réseau au relâchement.
 */
object GawkerClientCarryHandler {

    private const val MAX_CHARGE_TICKS: Int = 30 // 1.5 seconde de charge maximale
    private var chargeTicks: Int = 0
    private var readyForNewClick: Boolean = false
    private var wasCarryingLastTick: Boolean = false

    private fun getCarriedGawker(player: Player): GawkerEntity? {
        val level = player.level()
        return level.getEntitiesOfClass(
            GawkerEntity::class.java,
            player.boundingBox.inflate(3.0)
        ) { it.isCarried && it.carrierId == player.id }.firstOrNull()
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val carriedGawker = getCarriedGawker(player)
            val isCarryingGawker = carriedGawker != null

            if (isCarryingGawker) {
                // Consommer les touches Drop et SwapOffhand pour empêcher de lâcher ou changer d'item
                while (client.options.keyDrop.consumeClick()) {}
                while (client.options.keySwapOffhand.consumeClick()) {}

                // Si on vient tout juste de le ramasser : attendre qu'on relâche le clic initial du ramassage
                if (!wasCarryingLastTick) {
                    readyForNewClick = !client.options.keyUse.isDown
                } else if (!client.options.keyUse.isDown && !readyForNewClick) {
                    readyForNewClick = true
                }

                // Une fois le clic initial relâché, un nouveau clic droit enclenche la charge
                if (readyForNewClick && client.options.keyUse.isDown) {
                    chargeTicks = min(chargeTicks + 1, MAX_CHARGE_TICKS)

                    val percent = (chargeTicks.toFloat() / MAX_CHARGE_TICKS * 10).toInt()
                    val force = 0.8f + (chargeTicks.toFloat() / MAX_CHARGE_TICKS) * 1.4f

                    val bar = "■".repeat(percent) + "░".repeat(10 - percent)
                    val forceFormatted = String.format("%.1f", force)

                    // Affichage HUD sur l'Action Bar uniquement pendant la charge
                    client.gui.hud.setOverlayMessage(
                        Component.literal("§b❄ Lancer le Gawker : §f[§b$bar§f] §7(${forceFormatted}x)"),
                        false
                    )

                    // Son d'armement à tonalité montante
                    if (chargeTicks % 6 == 1) {
                        player.playSound(
                            SoundEvents.NOTE_BLOCK_CHIME.value(),
                            0.5f,
                            0.7f + (chargeTicks.toFloat() / MAX_CHARGE_TICKS) * 0.8f
                        )
                    }
                } else if (chargeTicks > 0) {
                    // Clic droit relâché : projection du Gawker
                    val force = 0.8f + (chargeTicks.toFloat() / MAX_CHARGE_TICKS) * 1.4f
                    ClientPlayNetworking.send(ThrowGawkerPayload(force))
                    chargeTicks = 0
                    readyForNewClick = false
                }
            } else {
                chargeTicks = 0
                readyForNewClick = false
            }

            wasCarryingLastTick = isCarryingGawker
        }
    }
}
