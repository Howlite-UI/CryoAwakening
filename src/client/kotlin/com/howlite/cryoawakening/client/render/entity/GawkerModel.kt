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
 * - Le corps reste stable (ne pivote pas avec la tête).
 * - Les 4 petites pattes (leg_front_left, leg_front_right, leg_back_left, leg_back_right) trottinent en diagonale alternée.
 * - La bouche (mouth) s'ouvre grand vers le haut lors d'une attaque (morsure) ou lorsqu'il subit des dégâts.
 * - La fourrure ondule doucement.
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

        // Données d'ouverture de bouche (dégâts et attaque)
        val hurtProgress = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.HURT_PROGRESS, 0.0f) ?: 0.0f
        val attackProgress = renderPassInfo.getOrDefaultGeckolibData(GawkerRenderer.ATTACK_PROGRESS, 0.0f) ?: 0.0f

        // 1. Yeux d'escargot : s'orientent vers le joueur + micro-tressautements organiques indépendants
        val eyeLeftTwitch = sin(age * 0.09f) * 0.04f + cos(age * 0.04f) * 0.02f
        val eyeRightTwitch = cos(age * 0.08f + 1.2f) * 0.04f + sin(age * 0.05f) * 0.02f

        val eyeLeftOpt = boneSnapshots.get("eye_left")
        if (eyeLeftOpt.isPresent) {
            val eyeLeft = eyeLeftOpt.get()
            eyeLeft.setRotation(lookPitchRad + eyeLeftTwitch, lookYawRad, eyeLeftTwitch * 0.5f)
        }

        val eyeRightOpt = boneSnapshots.get("eye_right")
        if (eyeRightOpt.isPresent) {
            val eyeRight = eyeRightOpt.get()
            eyeRight.setRotation(lookPitchRad + eyeRightTwitch, lookYawRad, -eyeRightTwitch * 0.5f)
        }

        // 2. Bouche cubique : s'ouvre sur son pivot arrière [0, 1, 4] lors d'une attaque ou d'un dégât
        val hurtMouthOpen = sin(hurtProgress * PI.toFloat()) * 0.65f
        val attackMouthOpen = sin(attackProgress * PI.toFloat()) * 0.85f
        val mouthOpen = (hurtMouthOpen + attackMouthOpen).coerceIn(0.0f, 1.1f)
        val mouthIdleBreath = sin(age * 0.08f) * 0.015f

        val mouthOpt = boneSnapshots.get("mouth")
        if (mouthOpt.isPresent) {
            val mouth = mouthOpt.get()
            // Rotation X négative pour basculer vers le haut depuis le pivot arrière
            mouth.setRotation(-mouthOpen, 0.0f, 0.0f)
            mouth.setTranslation(0.0f, mouthIdleBreath + mouthOpen * 0.4f, 0.0f)
        }

        // 3. Animation des 4 petites pattes (trottinement en diagonale alternée)
        val legSpeed = 1.4f
        val legAngle = sin(walkPos * legSpeed) * walkSpeed * 0.75f
        val liftA = (cos(walkPos * legSpeed) * walkSpeed * 0.35f).coerceAtLeast(0.0f)
        val liftB = (-cos(walkPos * legSpeed) * walkSpeed * 0.35f).coerceAtLeast(0.0f)

        // Diagonale A : front_left + back_right
        val legFrontLeftOpt = boneSnapshots.get("leg_front_left")
        if (legFrontLeftOpt.isPresent) {
            val leg = legFrontLeftOpt.get()
            leg.setRotation(legAngle, 0.0f, 0.0f)
            leg.setTranslation(0.0f, liftA, 0.0f)
        }

        val legBackRightOpt = boneSnapshots.get("leg_back_right")
        if (legBackRightOpt.isPresent) {
            val leg = legBackRightOpt.get()
            leg.setRotation(legAngle, 0.0f, 0.0f)
            leg.setTranslation(0.0f, liftA, 0.0f)
        }

        // Diagonale B : front_right + back_left
        val legFrontRightOpt = boneSnapshots.get("leg_front_right")
        if (legFrontRightOpt.isPresent) {
            val leg = legFrontRightOpt.get()
            leg.setRotation(-legAngle, 0.0f, 0.0f)
            leg.setTranslation(0.0f, liftB, 0.0f)
        }

        val legBackLeftOpt = boneSnapshots.get("leg_back_left")
        if (legBackLeftOpt.isPresent) {
            val leg = legBackLeftOpt.get()
            leg.setRotation(-legAngle, 0.0f, 0.0f)
            leg.setTranslation(0.0f, liftB, 0.0f)
        }

        // 4. Base du corps : léger rebond de pas
        val bodyBounce = abs(sin(walkPos * legSpeed)) * walkSpeed * 0.2f
        val bodyOpt = boneSnapshots.get("body")
        if (bodyOpt.isPresent) {
            val body = bodyOpt.get()
            body.setTranslation(0.0f, bodyBounce, 0.0f)
            body.setRotation(0.0f, 0.0f, 0.0f)
        }

        // 5. Fourrure latérale : ondulation douce et flottement
        val furSway = sin(age * 0.12f) * 0.05f + sin(walkPos * 1.4f) * walkSpeed * 0.12f
        val furLeftOpt = boneSnapshots.get("fur_left")
        if (furLeftOpt.isPresent) {
            val fur = furLeftOpt.get()
            fur.setRotation(furSway, 0.0f, 0.0f)
        }

        val furRightOpt = boneSnapshots.get("fur_right")
        if (furRightOpt.isPresent) {
            val fur = furRightOpt.get()
            fur.setRotation(furSway, 0.0f, 0.0f)
        }
    }
}
