package com.howlite.cryoawakening.client.model.property

import com.howlite.cryoawakening.entity.ClawshotAnchorEntity
import com.howlite.cryoawakening.item.ClawshotItem
import com.mojang.serialization.MapCodec
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

/**
 * Propriété numérique interpolée pour animer progressivement l'ouverture des griffes du Clawshot.
 * Valeur retournée : 0.0f (fermé) à 1.0f (ouvert à 100%).
 */
class ClawshotAimingProgressProperty : RangeSelectItemModelProperty {

    companion object {
        val MAP_CODEC: MapCodec<ClawshotAimingProgressProperty> = MapCodec.unit(ClawshotAimingProgressProperty())

        private var currentProgress: Float = 0.0f
        private var lastUpdateTime: Long = System.currentTimeMillis()

        private fun updateProgress(isAiming: Boolean): Float {
            val now = System.currentTimeMillis()
            val dt = ((now - lastUpdateTime).coerceIn(0, 100)) / 1000.0f
            lastUpdateTime = now

            val target = if (isAiming) 1.0f else 0.0f
            // Vitesse d'ouverture/fermeture : transition complète en environ 0.22s
            val speed = 4.5f

            if (currentProgress < target) {
                currentProgress = (currentProgress + speed * dt).coerceAtMost(target)
            } else if (currentProgress > target) {
                currentProgress = (currentProgress - speed * dt).coerceAtLeast(target)
            }
            return currentProgress
        }

        fun updateAndGetProgress(hand: net.minecraft.world.InteractionHand? = null): Float {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return 0.0f
            val lvl = mc.level ?: return 0.0f

            // Si CETTE main a déjà son grappin déployé, cette main n'anime pas ses griffes
            val hasActiveAnchor = ClawshotItem.findActiveAnchor(lvl, player, hand) != null
            if (hasActiveAnchor) {
                return updateProgress(false)
            }

            val eyePos = player.eyePosition
            val look = player.lookAngle
            val reach = ClawshotAnchorEntity.MAX_RANGE
            val endPos = eyePos.add(look.scale(reach))

            // 1. Raycast blocs à portée
            val blockHit = lvl.clip(
                ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
            )
            val hitDistance = if (blockHit.type != HitResult.Type.MISS) {
                eyePos.distanceTo(blockHit.location)
            } else {
                reach
            }

            // 2. Détection entités vivantes à portée
            var hitAny = blockHit.type != HitResult.Type.MISS
            if (!hitAny) {
                val searchBox = AABB(eyePos, eyePos.add(look.scale(hitDistance))).inflate(1.0)
                for (entity in lvl.getEntities(player, searchBox)) {
                    if (entity.isAlive && entity.isPickable) {
                        val bb = entity.boundingBox.inflate(0.3)
                        val clipOpt = bb.clip(eyePos, endPos)
                        if (clipOpt.isPresent && eyePos.distanceTo(clipOpt.get()) <= hitDistance) {
                            hitAny = true
                            break
                        }
                    }
                }
            }

            return updateProgress(hitAny)
        }

        fun getProgress(): Float = currentProgress
    }

    override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float {
        return updateAndGetProgress()
    }

    override fun type(): MapCodec<out RangeSelectItemModelProperty> = MAP_CODEC
}
