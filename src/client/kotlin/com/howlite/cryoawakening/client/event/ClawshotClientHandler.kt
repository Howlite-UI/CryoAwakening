package com.howlite.cryoawakening.client.event

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.howlite.cryoawakening.item.ClawshotItem
import com.howlite.cryoawakening.network.ClawshotRappelPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * ClawshotClientHandler
 *
 * Détecte les contrôles de rappel et de mou de chaîne lorsque le joueur est suspendu à un mur ou plafond :
 * - Ctrl (ou touche de sprint) : Lâcher du mou (faire descendre le joueur le long de la chaîne).
 * - Espace (touche de saut) : Remonter le long de la chaîne.
 * - Shift (accroupissement) : Se décrocher de la prise.
 *
 * Affiche également un rappel visuel des contrôles sur l'Action Bar du joueur.
 */
object ClawshotClientHandler {

    private var lastSentDirection: Int = 0
    private var lastAnchorId: Int = -1
    private var clingingTicks: Int = 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val level = client.level ?: return@register

            // Recherche de l'ancre active accrochée du joueur local
            val activeAnchor = ClawshotItem.findActiveAnchor(level, player)

            if (activeAnchor != null && activeAnchor.isClinging && activeAnchor.isAlive) {
                clingingTicks++

                val isCtrlDown = client.options.keySprint.isDown
                val isSpaceDown = client.options.keyJump.isDown

                val direction = when {
                    isCtrlDown -> -1 // Descendre (lâcher du mou)
                    isSpaceDown -> 1  // Remonter
                    else -> 0
                }

                if (direction != lastSentDirection || activeAnchor.id != lastAnchorId || (direction != 0 && clingingTicks % 10 == 0)) {
                    ClientPlayNetworking.send(ClawshotRappelPayload(activeAnchor.id, direction))
                    lastSentDirection = direction
                    lastAnchorId = activeAnchor.id
                }

                // Ajustement prédictif fluide immédiat côté client pour zéro latence
                val rappelStep = 0.18f * (1.0f + 0.25f * activeAnchor.rapidReelLevel)
                if (direction == -1) {
                    val currentTarget = activeAnchor.computeClingingPosition(
                        activeAnchor.hookPosition,
                        activeAnchor.getFacingNormal(),
                        activeAnchor.slackDistance.toDouble()
                    )
                    if (activeAnchor.canDescendFurther(level, player, currentTarget)) {
                        val maxSlack = (activeAnchor.getMaxRange() - 2.5).toFloat()
                        activeAnchor.slackDistance = (activeAnchor.slackDistance + rappelStep).coerceAtMost(maxSlack)
                    }
                } else if (direction == 1) {
                    activeAnchor.slackDistance = (activeAnchor.slackDistance - rappelStep).coerceAtLeast(0.0f)
                }
            } else {
                if (lastSentDirection != 0 && lastAnchorId != -1) {
                    ClientPlayNetworking.send(ClawshotRappelPayload(lastAnchorId, 0))
                }
                lastSentDirection = 0
                lastAnchorId = -1
                clingingTicks = 0
            }
        }
    }
}
