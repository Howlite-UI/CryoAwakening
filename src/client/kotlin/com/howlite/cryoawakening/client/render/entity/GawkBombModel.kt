package com.howlite.cryoawakening.client.render.entity

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.GawkBombEntity
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier
import kotlin.math.cos
import kotlin.math.sin

/**
 * GawkBombModel
 *
 * Modèle GeckoLib pour la Gawk-Bomb utilisant la texture dead_gawker.png.
 */
class GawkBombModel : GeoModel<GawkBombEntity>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("gawker")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/entity/dead_gawker.png")

    override fun getAnimationResource(animatable: GawkBombEntity): Identifier =
        CryoAwakening.id("gawker")

    fun setCustomAnimations(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        val state = renderPassInfo.renderState()
        val age = state.ageInTicks

        val isMine = renderPassInfo.getOrDefaultGeckolibData(GawkBombRenderer.IS_MINE, false) ?: false
        val powderCharge = renderPassInfo.getOrDefaultGeckolibData(GawkBombRenderer.POWDER_CHARGE, 0) ?: 0
        val isArmed = renderPassInfo.getOrDefaultGeckolibData(GawkBombRenderer.IS_ARMED, false) ?: false
        val fuseTicks = renderPassInfo.getOrDefaultGeckolibData(GawkBombRenderer.FUSE_TICKS, -1) ?: -1
        val flightTicks = renderPassInfo.getOrDefaultGeckolibData(GawkBombRenderer.FLIGHT_TICKS, 0) ?: 0

        // Vibrations de charge ou de mèche amorcée
        val isPrimed = fuseTicks in 0..15
        val shakeFreq = if (isPrimed) 35.0f else (16.0f + powderCharge * 10.0f)
        val shakeIntensity = if (isPrimed) 0.08f else (powderCharge * 0.035f)
        val shakeX = if (powderCharge > 0 || isPrimed) sin(age * shakeFreq) * shakeIntensity else 0.0f
        val shakeY = if (powderCharge > 0 || isPrimed) cos(age * (shakeFreq + 3.0f)) * (shakeIntensity * 0.6f) else 0.0f
        val shakeZ = if (powderCharge > 0 || isPrimed) sin(age * (shakeFreq + 5.0f)) * shakeIntensity else 0.0f
        val shakeRotZ = if (powderCharge > 0 || isPrimed) sin(age * shakeFreq * 0.7f) * (shakeIntensity * 0.8f) else 0.0f

        if (isMine) {
            // --- 1. Mode Mine au Sol ---
            // Yeux endormis/tombants
            val eyeDroop = 0.35f
            boneSnapshots.get("eye_left").ifPresent {
                it.setRotation(eyeDroop, 0.1f, 0.15f)
                it.setTranslation(shakeX * 0.5f, shakeY * 0.5f, shakeZ * 0.5f)
            }
            boneSnapshots.get("eye_right").ifPresent {
                it.setRotation(eyeDroop, -0.1f, -0.15f)
                it.setTranslation(-shakeX * 0.5f, shakeY * 0.5f, shakeZ * 0.5f)
            }

            // Bouche entrouverte selon la charge (plus ouverte si la mèche est allumée)
            val mouthOpen = if (isPrimed) 0.95f else (0.35f + powderCharge * 0.22f)
            boneSnapshots.get("mouth").ifPresent {
                it.setRotation(mouthOpen, 0.0f, shakeRotZ)
                it.setTranslation(shakeX * 0.6f, shakeY, shakeZ * 0.6f)
            }

            // Pattes posées à plat au sol
            boneSnapshots.get("leg_front_left").ifPresent { it.setRotation(0.1f, 0f, 0.1f); it.setTranslation(shakeX, 0f, shakeZ) }
            boneSnapshots.get("leg_back_right").ifPresent { it.setRotation(-0.1f, 0f, -0.1f); it.setTranslation(-shakeX, 0f, -shakeZ) }
            boneSnapshots.get("leg_front_right").ifPresent { it.setRotation(0.1f, 0f, -0.1f); it.setTranslation(shakeX, 0f, -shakeZ) }
            boneSnapshots.get("leg_back_left").ifPresent { it.setRotation(-0.1f, 0f, 0.1f); it.setTranslation(-shakeX, 0f, shakeZ) }

            // Corps posé au sol
            boneSnapshots.get("body").ifPresent {
                it.setTranslation(shakeX, shakeY, shakeZ)
                it.setRotation(0.0f, 0.0f, shakeRotZ)
            }
        } else {
            // --- 2. Mode Projectile Lancé ---
            val tumbleRoll = flightTicks * 0.35f
            val mouthOpen = 0.85f + (powderCharge * 0.15f)

            boneSnapshots.get("mouth").ifPresent {
                it.setRotation(mouthOpen, 0.0f, tumbleRoll)
                it.setTranslation(shakeX, shakeY, shakeZ)
            }

            boneSnapshots.get("eye_left").ifPresent { it.setRotation(0.4f, 0.2f, 0.2f) }
            boneSnapshots.get("eye_right").ifPresent { it.setRotation(0.4f, -0.2f, -0.2f) }

            // Pattes écartées de panique en plein vol
            boneSnapshots.get("leg_front_left").ifPresent { it.setRotation(0.5f, 0f, 0.5f) }
            boneSnapshots.get("leg_front_right").ifPresent { it.setRotation(0.5f, 0f, -0.5f) }
            boneSnapshots.get("leg_back_left").ifPresent { it.setRotation(-0.5f, 0f, 0.5f) }
            boneSnapshots.get("leg_back_right").ifPresent { it.setRotation(-0.5f, 0f, -0.5f) }

            boneSnapshots.get("body").ifPresent {
                it.setTranslation(shakeX, shakeY, shakeZ)
                it.setRotation(0.0f, 0.0f, tumbleRoll)
            }
        }

        // Fourrure latérale
        boneSnapshots.get("fur_left").ifPresent { it.setRotation(0.05f, 0.0f, 0.0f) }
        boneSnapshots.get("fur_right").ifPresent { it.setRotation(0.05f, 0.0f, 0.0f) }
    }
}
