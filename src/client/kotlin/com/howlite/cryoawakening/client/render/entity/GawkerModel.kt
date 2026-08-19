package com.howlite.cryoawakening.client.render.entity

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.GawkerEntity
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * GawkerModel
 *
 * Modèle GeckoLib pour le Gawker avec animations procédurales précises :
 * - Les 2 yeux d'escargot (eye_left, eye_right) s'orientent vers le joueur avec de légers tressautements indépendants.
 * - Le corps reste stable lors des déplacements au sol, ou tremble violemment s'il est gavé de Gunpowder.
 * - Les 4 petites pattes trottinent en diagonale alternée.
 * - La bouche s'ouvre grand vers le haut lors d'une attaque, dégâts, ou selon le niveau de charge de Gunpowder (jusqu'à 3x).
 * - État porté (au-dessus de la tête) : pattes qui gigotent et bouche entrouverte.
 * - État lancé (en vol) : vrille et panique aérienne avec traînée de particules.
 */
class GawkerModel : GeoModel<GawkerEntity>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("gawker")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/entity/gawker.png")

    override fun getAnimationResource(animatable: GawkerEntity): Identifier =
        CryoAwakening.id("gawker")

    fun setCustomAnimations(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        val state = renderPassInfo.renderState()
        val walkPos = state.walkAnimationPos
        val walkSpeed = state.walkAnimationSpeed
        val age = state.ageInTicks

        // Données d'orientation pour les yeux d'escargot
        val netHeadYawDeg = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.NET_HEAD_YAW, 0.0f) ?: 0.0f
        val headPitchDeg = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.HEAD_PITCH, 0.0f) ?: 0.0f

        val lookYawRad = Math.toRadians(netHeadYawDeg.toDouble()).toFloat().coerceIn(-0.65f, 0.65f)
        val lookPitchRad = Math.toRadians(headPitchDeg.toDouble()).toFloat().coerceIn(-0.45f, 0.45f)

        // Données d'état spécial
        val hurtProgress = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.HURT_PROGRESS, 0.0f) ?: 0.0f
        val attackProgress = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.ATTACK_PROGRESS, 0.0f) ?: 0.0f
        val isCarried = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.IS_CARRIED, false) ?: false
        val isThrown = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.IS_THROWN, false) ?: false
        val flightTicks = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.FLIGHT_TICKS, 0) ?: 0
        val powderCharge = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.POWDER_CHARGE, 0) ?: 0

        // Calcul des vibrations/tremblements proportionnels à la charge de Gunpowder (0 à 3)
        val shakeFreq = 16.0f + (powderCharge * 10.0f)
        val shakeIntensity = powderCharge * 0.035f
        val shakeX = if (powderCharge > 0) sin(age * shakeFreq) * shakeIntensity else 0.0f
        val shakeY = if (powderCharge > 0) cos(age * (shakeFreq + 3.0f)) * (shakeIntensity * 0.6f) else 0.0f
        val shakeZ = if (powderCharge > 0) sin(age * (shakeFreq + 5.0f)) * shakeIntensity else 0.0f
        val shakeRotZ = if (powderCharge > 0) sin(age * shakeFreq * 0.7f) * (powderCharge * 0.035f) else 0.0f

        // 1. Yeux d'escargot : orientation vers la cible + micro-tressautements + panique si surchargé
        val eyeLeftTwitch = sin(age * 0.09f) * 0.04f + cos(age * 0.04f) * 0.02f
        val eyeRightTwitch = cos(age * 0.08f + 1.2f) * 0.04f + sin(age * 0.05f) * 0.02f
        val anxiousTwitch = if (powderCharge >= 2) sin(age * 24.0f) * (powderCharge * 0.05f) else 0.0f

        val eyeLeftOpt = boneSnapshots.get("eye_left")
        if (eyeLeftOpt.isPresent) {
            val eyeLeft = eyeLeftOpt.get()
            eyeLeft.setRotation(lookPitchRad + eyeLeftTwitch + anxiousTwitch, lookYawRad + anxiousTwitch, eyeLeftTwitch * 0.5f)
            eyeLeft.setTranslation(shakeX * 0.5f, shakeY * 0.5f, shakeZ * 0.5f)
        }

        val eyeRightOpt = boneSnapshots.get("eye_right")
        if (eyeRightOpt.isPresent) {
            val eyeRight = eyeRightOpt.get()
            eyeRight.setRotation(lookPitchRad + eyeRightTwitch - anxiousTwitch, lookYawRad - anxiousTwitch, -eyeRightTwitch * 0.5f)
            eyeRight.setTranslation(-shakeX * 0.5f, shakeY * 0.5f, shakeZ * 0.5f)
        }

        // 2. Bouche cubique : s'ouvre de plus en plus avec la Gunpowder (jusqu'à 1.0 rad !)
        val hurtMouthOpen = sin(hurtProgress * PI.toFloat()) * 0.65f
        val attackMouthOpen = sin(attackProgress * PI.toFloat()) * 0.85f
        val carriedMouthOpen = if (isCarried) 0.30f else 0.0f
        val thrownMouthOpen = if (isThrown) 0.70f else 0.0f
        val powderMouthOpen = powderCharge * 0.32f

        val mouthOpen = (hurtMouthOpen + attackMouthOpen + carriedMouthOpen + thrownMouthOpen + powderMouthOpen).coerceIn(0.0f, 1.25f)
        val mouthIdleBreath = sin(age * 0.08f) * 0.015f

        val mouthOpt = boneSnapshots.get("mouth")
        if (mouthOpt.isPresent) {
            val mouth = mouthOpt.get()
            val tumbleRoll = if (isThrown) flightTicks * 0.35f else 0.0f
            mouth.setRotation(mouthOpen, 0.0f, tumbleRoll + shakeRotZ)
            mouth.setTranslation(shakeX * 0.6f, mouthIdleBreath + shakeY, shakeZ * 0.6f)
        }

        // 3. Animation des 4 pattes
        if (isCarried) {
            // Gigotement aérien des pattes lorsque porté au-dessus de la tête
            val flail1 = sin(age * 0.6f) * 0.6f
            val flail2 = cos(age * 0.6f) * 0.6f

            boneSnapshots.get("leg_front_left").ifPresent { it.setRotation(flail1, 0f, 0.2f); it.setTranslation(shakeX, 0f, shakeZ) }
            boneSnapshots.get("leg_back_right").ifPresent { it.setRotation(-flail1, 0f, -0.2f); it.setTranslation(-shakeX, 0f, -shakeZ) }
            boneSnapshots.get("leg_front_right").ifPresent { it.setRotation(flail2, 0f, -0.2f); it.setTranslation(shakeX, 0f, -shakeZ) }
            boneSnapshots.get("leg_back_left").ifPresent { it.setRotation(-flail2, 0f, 0.2f); it.setTranslation(-shakeX, 0f, shakeZ) }
        } else if (isThrown) {
            // Pattes écartées de stupeur en plein vol
            boneSnapshots.get("leg_front_left").ifPresent { it.setRotation(0.5f, 0f, 0.5f) }
            boneSnapshots.get("leg_front_right").ifPresent { it.setRotation(0.5f, 0f, -0.5f) }
            boneSnapshots.get("leg_back_left").ifPresent { it.setRotation(-0.5f, 0f, 0.5f) }
            boneSnapshots.get("leg_back_right").ifPresent { it.setRotation(-0.5f, 0f, -0.5f) }
        } else {
            // Trottinement au sol normal
            val legSpeed = 1.4f
            val legAngle = sin(walkPos * legSpeed) * walkSpeed * 0.75f
            val liftA = (cos(walkPos * legSpeed) * walkSpeed * 0.35f).coerceAtLeast(0.0f)
            val liftB = (-cos(walkPos * legSpeed) * walkSpeed * 0.35f).coerceAtLeast(0.0f)

            // Diagonale A : front_left + back_right
            boneSnapshots.get("leg_front_left").ifPresent {
                it.setRotation(legAngle, 0.0f, 0.0f)
                it.setTranslation(shakeX, liftA + shakeY, shakeZ)
            }
            boneSnapshots.get("leg_back_right").ifPresent {
                it.setRotation(legAngle, 0.0f, 0.0f)
                it.setTranslation(-shakeX, liftA + shakeY, -shakeZ)
            }

            // Diagonale B : front_right + back_left
            boneSnapshots.get("leg_front_right").ifPresent {
                it.setRotation(-legAngle, 0.0f, 0.0f)
                it.setTranslation(shakeX, liftB + shakeY, -shakeZ)
            }
            boneSnapshots.get("leg_back_left").ifPresent {
                it.setRotation(-legAngle, 0.0f, 0.0f)
                it.setTranslation(-shakeX, liftB + shakeY, shakeZ)
            }
        }

        // 4. Base du corps : léger rebond de pas + vibrations si gavé
        val bodyBounce = if (!isCarried && !isThrown) abs(sin(walkPos * 1.4f)) * walkSpeed * 0.2f else 0.0f
        val bodyOpt = boneSnapshots.get("body")
        if (bodyOpt.isPresent) {
            val body = bodyOpt.get()
            body.setTranslation(shakeX, bodyBounce + shakeY, shakeZ)
            body.setRotation(0.0f, 0.0f, shakeRotZ)
        }

        // 5. Fourrure latérale : ondulation douce et flottement
        val furSway = sin(age * 0.12f) * 0.05f + sin(walkPos * 1.4f) * walkSpeed * 0.12f
        boneSnapshots.get("fur_left").ifPresent { it.setRotation(furSway, 0.0f, 0.0f) }
        boneSnapshots.get("fur_right").ifPresent { it.setRotation(furSway, 0.0f, 0.0f) }
    }
}
