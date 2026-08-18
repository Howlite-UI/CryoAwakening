package com.howlite.cryoawakening.client.render.entity

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.entity.GlaciopodEntity
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * GlaciopodModel
 *
 * Modèle procédural GeckoLib pour le Glaciopod.
 * - En hibernation (t = 0) : forme le bloc rocheux 2x2x1 parfait avec la texture en spirale d'origine.
 * - Au réveil (t = 1) : se déplie en corps articulé allongé vers l'avant :
 *   - Tête connectée sans aucun écart à la carapace, tournant son regard (yaw/pitch) vers le joueur.
 *   - Carapace en 16 losanges articulés avec virage serpentin réaliste.
 */
class GlaciopodModel : GeoModel<GlaciopodEntity>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("glaciopod")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/entity/glaciopod.png")

    override fun getAnimationResource(animatable: GlaciopodEntity): Identifier =
        CryoAwakening.id("glaciopod")

    companion object {
        // Centres X des 16 sections dans la grille 4x4 compacte du bloc 2x2x1
        private val PIVOT_X = floatArrayOf(
            4f,   4f,  -4f,  -4f,  // 1, 2, 3, 4
           -4f,   4f,  12f,  12f,  // 5, 6, 7, 8
           12f,  12f,   4f,  -4f,  // 9, 10, 11, 12
          -12f, -12f, -12f, -12f   // 13, 14, 15, 16
        )

        // Centres Y des 16 sections dans la grille 4x4 compacte du bloc 2x2x1
        private val PIVOT_Y = floatArrayOf(
           12f, 20f, 20f, 12f,
            4f,  4f,  4f, 12f,
           20f, 28f, 28f, 28f,
           28f, 20f, 12f,  4f
        )

        // Delta de rotation pour amener chaque cube de sa rotation spirale d'origine vers 45° (losange <>)
        private val DELTA_ROT_Z = floatArrayOf(
            (PI / 4.0).toFloat(),       // 1 (base 0° -> 45°)
            (3.0 * PI / 4.0).toFloat(),  // 2 (base -90° -> 45°)
            (-3.0 * PI / 4.0).toFloat(), // 3 (base -180° -> 45°)
            (-PI / 4.0).toFloat(),      // 4 (base +90° -> 45°)
            (-PI / 4.0).toFloat(),      // 5 (base +90° -> 45°)
            (PI / 4.0).toFloat(),       // 6 (base 0° -> 45°)
            (PI / 4.0).toFloat(),       // 7 (base 0° -> 45°)
            (PI / 4.0).toFloat(),       // 8 (base 0° -> 45°)
            (3.0 * PI / 4.0).toFloat(),  // 9 (base -90° -> 45°)
            (3.0 * PI / 4.0).toFloat(),  // 10 (base -90° -> 45°)
            (3.0 * PI / 4.0).toFloat(),  // 11 (base -90° -> 45°)
            (-3.0 * PI / 4.0).toFloat(), // 12 (base -180° -> 45°)
            (-3.0 * PI / 4.0).toFloat(), // 13 (base -180° -> 45°)
            (-3.0 * PI / 4.0).toFloat(), // 14 (base -180° -> 45°)
            (-PI / 4.0).toFloat(),      // 15 (base +90° -> 45°)
            (-PI / 4.0).toFloat()       // 16 (base +90° -> 45°)
        )
    }

    /**
     * Applique les transformations procédurales sur les 16 segments du corps et la tête.
     */
    fun setCustomAnimations(
        renderPassInfo: RenderPassInfo<LivingEntityRenderState>,
        boneSnapshots: BoneSnapshots
    ) {
        val unfurl: Float = renderPassInfo.getOrDefaultGeckolibData(
            GlaciopodRenderer.UNFURL_PROGRESS,
            0.0f
        ) ?: 0.0f

        // Transition d'ouverture fluide (smoothstep)
        val t = unfurl * unfurl * (3.0f - 2.0f * unfurl)

        val state = renderPassInfo.renderState()
        val walkPos = state.walkAnimationPos
        val walkSpeed = state.walkAnimationSpeed
        val age = state.ageInTicks

        // Données d'orientation pour le regard et la courbure serpentine
        val netHeadYawDeg = renderPassInfo.getOrDefaultGeckolibData(GlaciopodRenderer.NET_HEAD_YAW, 0.0f) ?: 0.0f
        val headPitchDeg = renderPassInfo.getOrDefaultGeckolibData(GlaciopodRenderer.HEAD_PITCH, 0.0f) ?: 0.0f
        val segmentYaws = renderPassInfo.getOrDefaultGeckolibData(GlaciopodRenderer.SEGMENT_YAWS, null)

        val lookYawRad = Math.toRadians(netHeadYawDeg.toDouble()).toFloat().coerceIn(-1.1f, 1.1f)
        val lookPitchRad = Math.toRadians(headPitchDeg.toDouble()).toFloat().coerceIn(-0.6f, 0.6f)

        // Facteur d'activation de la marche (actif quand le mob est suffisamment déplié)
        val walkWaveBlend = if (unfurl > 0.7f) {
            ((unfurl - 0.7f) / 0.3f).coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }

        // Animation des 16 sections de carapace
        val segmentSpacing = 5.8f
        val startX = 45.0f
        val straightHeight = 8.0f

        // Calcul cumulatif du décalage latéral serpentin
        var cumulativeLateralOffset = 0.0f

        for (i in 0 until 16) {
            val boneOpt = boneSnapshots.get("section_${i + 1}")
            if (boneOpt.isEmpty) continue
            val bone = boneOpt.get()

            val pX = PIVOT_X[i]
            val pY = PIVOT_Y[i]

            // Courbure de virage serpentin (retard de rotation sur chaque segment)
            val segYawDeg = segmentYaws?.getOrNull(i) ?: 0.0f
            val segYawRad = Math.toRadians(segYawDeg.toDouble()).toFloat() * t

            cumulativeLateralOffset += sin(segYawRad) * (segmentSpacing * 0.45f)

            // Cible dépliée
            val targetX = startX - (i * segmentSpacing)
            val targetY = straightHeight
            val targetZ = cumulativeLateralOffset

            // Ondulation sinusoïdale de marche (locomotion type mille-pattes)
            val phase = walkPos * 0.60f - i * 0.28f
            val waveY = sin(phase) * walkSpeed * 0.22f * walkWaveBlend
            val waveX = cos(phase) * walkSpeed * 0.04f * walkWaveBlend
            val waveLateralZ = sin(phase) * walkSpeed * 1.4f * walkWaveBlend

            // Micro-mouvements de respiration au repos
            val idleSway = sin(age * 0.08f + i * 0.20f) * 0.02f * unfurl

            // Translations interpolées
            val transX = (targetX - pX) * t
            val transY = (targetY - pY) * t
            val transZ = targetZ + waveLateralZ

            // Rotations interpolées : 45° losange + virage serpentin + onde de marche
            val rotX = waveX
            val rotY = segYawRad + waveY + idleSway
            val rotZ = DELTA_ROT_Z[i] * t

            bone.setTranslation(transX, transY, transZ)
            bone.setRotation(rotX, rotY, rotZ)
        }

        // Animation de la tête : connectée directement à section_1 sans aucun espace
        val headOpt = boneSnapshots.get("head")
        if (headOpt.isPresent) {
            val head = headOpt.get()

            // À t = 0 : rentrée dans le bloc (invisible)
            // À t = 1 : placée juste contre la section 1 (X = +46.5f, Y = 4.5f)
            val headTransX = 46.5f * t
            val headTransY = (4.5f - 16.0f) * t
            val headTransZ = 0.0f

            // Rotation : regard indépendant vers le joueur (Yaw et Pitch) + onde de marche
            val headBaseRotY = (PI / 2.0).toFloat() * t
            val headWaveY = sin(walkPos * 0.60f) * walkSpeed * 0.12f * walkWaveBlend
            val headIdle = sin(age * 0.06f) * 0.03f * unfurl

            val headRotY = headBaseRotY + (lookYawRad * t) + headWaveY + headIdle
            val headRotZ = -lookPitchRad * t

            head.setTranslation(headTransX, headTransY, headTransZ)
            head.setRotation(0.0f, headRotY, headRotZ)
            head.setScale(t, t, t)
        }
    }
}
