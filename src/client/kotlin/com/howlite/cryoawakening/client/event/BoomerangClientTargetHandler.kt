package com.howlite.cryoawakening.client.event

import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.network.ThrowBoomerangPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * BoomerangClientTargetHandler
 *
 * Gère le système de visée et de verrouillage multi-cibles (Lock-on style Zelda: Twilight Princess) :
 * 1. Détecte le maintien du clic droit avec le Gale Boomerang.
 * 2. Effectue un raycast continu pour identifier et marquer jusqu'à 5 cibles distinctes.
 * 3. Émet des retours sonores harmoniques à chaque cible verrouillée.
 * 4. Affiche l'état des cibles sur l'Action Bar.
 * 5. Envoie le paquet réseau ThrowBoomerangPayload au relâchement.
 */
object BoomerangClientTargetHandler {

    const val MAX_TARGETS: Int = 5
    const val MAX_CHARGE_TICKS: Int = 25

    private var chargeTicks: Int = 0
    private var currentMaxChargeTicks: Int = 25
    private var isUsingBoomerang: Boolean = false
    val markedTargetIds = ArrayList<Int>()

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val level = client.level ?: return@register

            val isHoldingBoomerang = player.isUsingItem && player.useItem.`is`(ModItems.GALE_BOOMERANG)

            if (isHoldingBoomerang) {
                isUsingBoomerang = true
                val stack = player.useItem

                val zephyrLvl = com.howlite.cryoawakening.enchantment.ModEnchantments.getLevel(
                    stack,
                    com.howlite.cryoawakening.enchantment.ModEnchantments.ZEPHYR,
                    level
                )
                val ricochetLvl = com.howlite.cryoawakening.enchantment.ModEnchantments.getLevel(
                    stack,
                    com.howlite.cryoawakening.enchantment.ModEnchantments.RICOCHET,
                    level
                )
                val hawkeyeLvl = com.howlite.cryoawakening.enchantment.ModEnchantments.getLevel(
                    stack,
                    com.howlite.cryoawakening.enchantment.ModEnchantments.HAWKEYE,
                    level
                )

                currentMaxChargeTicks = (25 * (1.0f - (zephyrLvl * 0.25f))).toInt().coerceAtLeast(6)
                val dynamicMaxTargets = (4 + (ricochetLvl * 3)).coerceIn(4, 16)
                val maxReach = 28.0 + (zephyrLvl * 6.0) + (hawkeyeLvl * 8.0)

                chargeTicks = min(chargeTicks + 1, currentMaxChargeTicks)

                // Raycast pour verrouiller des cibles sous le réticule
                val lookVec = player.getViewVector(1.0f)
                val startPos = player.eyePosition
                val endPos = startPos.add(lookVec.scale(maxReach))
                val searchBox = player.boundingBox.expandTowards(lookVec.scale(maxReach)).inflate(2.0)

                var closestEntity: Entity? = null
                var closestDistSq = maxReach * maxReach

                val candidates = level.getEntities(
                    player,
                    searchBox
                ) { entity ->
                    entity != player && entity.isAlive &&
                    (entity is LivingEntity || entity is ItemEntity)
                }

                for (entity in candidates) {
                    val box = entity.boundingBox.inflate(0.4)
                    val clip = box.clip(startPos, endPos)
                    if (clip.isPresent) {
                        val distSq = startPos.distanceToSqr(clip.get())
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq
                            closestEntity = entity
                        }
                    }
                }

                if (closestEntity != null) {
                    if (!markedTargetIds.contains(closestEntity.id) && markedTargetIds.size < dynamicMaxTargets) {
                        markedTargetIds.add(closestEntity.id)

                        // Son de verrouillage Zelda avec pitch montant : 0.9f à 2.0f
                        val soundPitch = 0.9f + (markedTargetIds.size * 0.15f).coerceAtMost(1.2f)
                        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, soundPitch)
                        player.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.6f, soundPitch)
                    }
                }

                // Mobilité Zephyr : réactivité naturelle sans effet de glisse sur glace ni déformation du FOV
                if (zephyrLvl > 0) {
                    val keyPresses = player.input.keyPresses
                    val isMoving = keyPresses.forward() || keyPresses.backward() || keyPresses.left() || keyPresses.right()

                    if (isMoving) {
                        val m = player.deltaMovement
                        val horizSpeed = Math.sqrt(m.x * m.x + m.z * m.z)
                        if (horizSpeed > 0.005) {
                            val baseTarget = if (player.isSprinting) 0.25 else 0.17
                            val maxAllowedSpeed = baseTarget * (0.75 + (zephyrLvl * 0.15))
                            if (horizSpeed < maxAllowedSpeed) {
                                val scale = (maxAllowedSpeed / horizSpeed).coerceAtMost(2.2)
                                player.deltaMovement = net.minecraft.world.phys.Vec3(m.x * scale, m.y, m.z * scale)
                            }
                        }
                    }
                }

                // Son d'armement périodique pendant la charge
                if (chargeTicks % 6 == 1) {
                    player.playSound(
                        SoundEvents.NOTE_BLOCK_CHIME.value(),
                        0.4f,
                        0.8f + (chargeTicks.toFloat() / currentMaxChargeTicks) * 0.6f
                    )
                }
            } else if (isUsingBoomerang) {
                // Relâchement du clic droit : lancer du boomerang avec les cibles enregistrées
                if (chargeTicks > 0) {
                    val chargeRatio = (chargeTicks.toFloat() / currentMaxChargeTicks.toFloat()).coerceIn(0.0f, 1.0f)
                    val force = 0.35f + (chargeRatio * 0.65f)
                    ClientPlayNetworking.send(ThrowBoomerangPayload(force, markedTargetIds.toList()))
                }

                isUsingBoomerang = false
                chargeTicks = 0
                currentMaxChargeTicks = 25
                markedTargetIds.clear()
            }
        }
    }

    fun isCharging(): Boolean = isUsingBoomerang && chargeTicks > 0

    fun getChargeProgress(): Float =
        if (currentMaxChargeTicks > 0) (chargeTicks.toFloat() / currentMaxChargeTicks.toFloat()).coerceIn(0.0f, 1.0f) else 0.0f

    fun getTargetCount(): Int = markedTargetIds.size
}
