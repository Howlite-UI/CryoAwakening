package com.howlite.cryoawakening.client.event

import com.howlite.cryoawakening.block.GalePipeExhaustBlock
import com.howlite.cryoawakening.block.entity.GalePipeExhaustBlockEntity
import com.howlite.cryoawakening.network.SetPipeExhaustSpeedPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.roundToInt

/**
 * ValueSettingsClientHandler
 *
 * Reproduit fidèlement le système d'interaction "Value Settings Screen" du mod Create :
 * - Maintenir le Clic-Droit (touche d'utilisation) sur la zone du bloc pour entrer en mode réglage.
 * - Déplacer la souris (gauche/droite) ou faire défiler la molette pour modifier la valeur (0 à 50 V/t).
 * - Maintenir Shift (Sneak) pour aligner instantanément sur des multiples de 8 (0, 8, 16, 24, 32, 40, 48, 50).
 * - Relâcher le Clic-Droit pour confirmer et verrouiller la nouvelle valeur.
 */
object ValueSettingsClientHandler {

    var isHolding: Boolean = false
        private set

    var targetPos: BlockPos? = null
        private set

    var currentValue: Int = 0
    private var rawFloatValue: Float = 0.0f
    private var wasUseKeyDown: Boolean = false

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val level = client.level ?: return@register
            if (client.gui.screen() != null) {
                if (isHolding) cancelHold()
                return@register
            }

            val isUseKeyDown = client.options.keyUse.isDown

            if (!isHolding) {
                // Détection de l'appui sur le clic-droit pour commencer le réglage
                if (isUseKeyDown && !wasUseKeyDown) {
                    val hit = client.hitResult as? BlockHitResult
                    if (hit != null && hit.type == HitResult.Type.BLOCK) {
                        val pos = hit.blockPos
                        val state = level.getBlockState(pos)
                        if (state.block is GalePipeExhaustBlock) {
                            val be = level.getBlockEntity(pos) as? GalePipeExhaustBlockEntity
                            if (be != null) {
                                isHolding = true
                                targetPos = pos
                                currentValue = be.outputRate
                                rawFloatValue = be.outputRate.toFloat()

                                // Clic d'ouverture
                                client.soundManager.play(
                                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.4f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Pendant le maintien du réglage
                val pos = targetPos
                if (pos == null || level.getBlockState(pos).block !is GalePipeExhaustBlock) {
                    cancelHold()
                } else if (!isUseKeyDown) {
                    // Relâchement du clic-droit : Confirmation et envoi au serveur !
                    isHolding = false
                    ClientPlayNetworking.send(SetPipeExhaustSpeedPayload(pos, currentValue))

                    // Son de verrouillage/confirmation
                    client.soundManager.play(
                        SimpleSoundInstance.forUI(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.4f, 0.5f)
                    )
                    targetPos = null
                } else {
                    // Maintien actif : synchronisation visuelle locale continue
                    val be = level.getBlockEntity(pos) as? GalePipeExhaustBlockEntity
                    be?.outputRate = currentValue
                }
            }

            wasUseKeyDown = isUseKeyDown
        }
    }

    fun handleMouseMove(dx: Double, dy: Double): Boolean {
        if (!isHolding) return false

        val sensitivity = 0.22f
        rawFloatValue = (rawFloatValue + (dx * sensitivity).toFloat()).coerceIn(0.0f, 50.0f)

        val client = Minecraft.getInstance()
        val isSneaking = client.player?.isShiftKeyDown == true

        val newInt = if (isSneaking) {
            // Snap sur les multiples de 8 (style Create mod)
            val snapped = ((rawFloatValue / 8.0f).roundToInt() * 8)
            snapped.coerceIn(0, 50)
        } else {
            rawFloatValue.roundToInt().coerceIn(0, 50)
        }

        if (newInt != currentValue) {
            currentValue = newInt
            playRatchetClick(newInt)
        }

        return true // Bloque la rotation de la caméra pendant le glissement du curseur
    }

    fun handleMouseScroll(delta: Double): Boolean {
        if (!isHolding) return false

        val client = Minecraft.getInstance()
        val isSneaking = client.player?.isShiftKeyDown == true
        val step = if (isSneaking) 8 else 1
        val change = if (delta > 0) step else -step
        val newInt = (currentValue + change).coerceIn(0, 50)

        if (newInt != currentValue) {
            currentValue = newInt
            rawFloatValue = newInt.toFloat()
            playRatchetClick(newInt)
        }
        return true
    }

    private fun cancelHold() {
        isHolding = false
        targetPos = null
    }

    private fun playRatchetClick(value: Int) {
        val pitch = 0.65f + 0.75f * (value.toFloat() / 50.0f)
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.4f)
        )
    }
}
