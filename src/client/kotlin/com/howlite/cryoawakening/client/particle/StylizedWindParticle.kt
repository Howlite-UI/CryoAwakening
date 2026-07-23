package com.howlite.cryoawakening.client.particle

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.renderer.state.level.QuadParticleRenderState
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import org.joml.Quaternionf
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Particule StylizedWind — Ligne de vent procédurale cartoonish (Wind Waker style).
 *
 * Au lieu d'un simple sprite unique, cette particule génère et maintient un ruban
 * procédural composé de plusieurs nœuds d'historique de trajectoire. Le ruban s'évase
 * au centre et s'affine aux extrémités (tapering cartoon), tout en suivant une trajectoire
 * sinusoïdale fluide avec de rares boucles en spirale (loopings 3D).
 */
class StylizedWindParticle(
    level: ClientLevel,
    x: Double, y: Double, z: Double,
    xSpeed: Double, ySpeed: Double, zSpeed: Double,
    sprites: FabricSpriteSet
) : SingleQuadParticle(level, x, y, z, sprites.first()) {

    private data class TrailNode(
        val x: Double, val y: Double, val z: Double,
        val oldX: Double, val oldY: Double, val oldZ: Double
    )

    companion object {
        /** Nombre maximal de segments formant le ruban procédural */
        private const val MAX_TRAIL_NODES = 12
    }

    /** Historique des positions pour composer le ruban procédural de vent */
    private val trailNodes = ArrayDeque<TrailNode>()

    /** Déphasage sinusoïdal propre à cette particule */
    private val phaseX: Float = random.nextFloat() * (2f * PI.toFloat())
    private val phaseZ: Float = random.nextFloat() * (2f * PI.toFloat())

    /** Vitesse verticale de base */
    private val baseYSpeed: Double = ySpeed.coerceAtLeast(0.06)

    // --- Paramètres de looping procédural ---
    private var isLooping: Boolean = false
    private var loopTicks: Int = 0
    private var loopAngle: Float = 0f
    private var loopAxisX: Float = 1f
    private var loopAxisZ: Float = 0f

    init {
        xd = xSpeed
        yd = baseYSpeed
        zd = zSpeed

        // Durée de vie du ruban : 50 à 90 ticks
        lifetime = 50 + random.nextInt(41)

        // Epaisseur de base du ruban (style pixel-art cartoon)
        quadSize = 0.16f + random.nextFloat() * 0.08f

        // Couleur cyan glacial lumineuse avec cœur blanc
        rCol = 0.72f // ~184/255
        gCol = 0.92f // ~235/255
        bCol = 0.98f // ~250/255
        setAlpha(0.9f)

        gravity = 0f

        setSprite(sprites.first())
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z

        if (age++ >= lifetime) {
            remove()
            return
        }

        // Ajouter la position courante dans le ruban procédural
        trailNodes.addLast(TrailNode(x, y, z, xo, yo, zo))
        if (trailNodes.size > MAX_TRAIL_NODES) {
            trailNodes.removeFirst()
        }

        val t = age.toFloat()

        if (!isLooping) {
            // === Animation fluide sinusoïdale ===
            xd += sin((t * 0.16f + phaseX).toDouble()) * 0.012
            zd += cos((t * 0.16f + phaseZ).toDouble()) * 0.012
            yd = baseYSpeed + sin((t * 0.09f).toDouble()) * 0.005

            // ~2.5% de chance par tick de déclencher un looping en spirale
            if (age > 8 && age < lifetime - 20 && random.nextFloat() < 0.025f) {
                isLooping = true
                loopTicks = 0
                loopAngle = 0f
                if (random.nextBoolean()) {
                    loopAxisX = 1f; loopAxisZ = 0.3f
                } else {
                    loopAxisX = 0.3f; loopAxisZ = 1f
                }
            }
        } else {
            // === Mini-looping 3D procédural (spirale cartoon) ===
            loopTicks++
            loopAngle += 18f
            val rad = Math.toRadians(loopAngle.toDouble())
            val radius = 0.05

            xd = cos(rad) * radius * loopAxisX
            yd = sin(rad) * radius * 0.8 + baseYSpeed * 0.4
            zd = cos(rad) * radius * loopAxisZ

            if (loopTicks >= 20 || loopAngle >= 360f) {
                isLooping = false
            }
        }

        // Dissolution progressive à la fin
        val lifeRatio = age.toFloat() / lifetime.toFloat()
        if (lifeRatio > 0.75f) {
            setAlpha(Mth.lerp((lifeRatio - 0.75f) / 0.25f, 0.9f, 0.0f))
        }

        move(xd, yd, zd)

        // Amortissement des mouvements latéraux
        xd *= 0.91
        zd *= 0.91
    }

    /**
     * Rendu procédural personnalisé : extrait tous les quads du ruban le long de l'historique
     * de positions, avec profil de réduction cartoon (tapering).
     */
    override fun extract(renderState: QuadParticleRenderState, camera: Camera, partialTick: Float) {
        if (trailNodes.isEmpty()) return

        val camPos = camera.position()
        val quat = Quaternionf()
        facingCameraMode.setRotation(quat, camera, partialTick)

        if (roll != 0f) {
            quat.rotateZ(Mth.lerp(partialTick, oRoll, roll))
        }

        val total = trailNodes.size
        val light = getLightCoords(partialTick)
        val u0 = getU0()
        val u1 = getU1()
        val v0 = getV0()
        val v1 = getV1()

        var index = 0
        for (node in trailNodes) {
            // Normalized position along the ribbon: 0.0 (oldest/tail) to 1.0 (head)
            val progress = if (total > 1) index / (total - 1).toFloat() else 1.0f

            // Profil de réduction cartoonish (s'évase au milieu, pointe fine à la queue)
            val taper = sin((progress * PI).toDouble()).toFloat()
            val nodeSize = quadSize * (0.25f + 1.25f * taper)

            // Opacité progressive du ruban (fade-out vers la queue)
            val nodeAlpha = alpha * (0.2f + 0.8f * progress)
            val colorARGB = ARGB.colorFromFloat(nodeAlpha, rCol, gCol, bCol)

            // Position interpolée par rapport à la caméra
            val relX = (Mth.lerp(partialTick.toDouble(), node.oldX, node.x) - camPos.x).toFloat()
            val relY = (Mth.lerp(partialTick.toDouble(), node.oldY, node.y) - camPos.y).toFloat()
            val relZ = (Mth.lerp(partialTick.toDouble(), node.oldZ, node.z) - camPos.z).toFloat()

            // Soumettre le quad du segment au pipeline de rendu Minecraft
            renderState.add(
                layer,
                relX, relY, relZ,
                quat.x, quat.y, quat.z, quat.w,
                nodeSize,
                u0, u1, v0, v1,
                colorARGB,
                light
            )

            index++
        }
    }

    override fun getLayer(): SingleQuadParticle.Layer =
        SingleQuadParticle.Layer.TRANSLUCENT

    class Factory(private val sprites: FabricSpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            type: SimpleParticleType,
            level: ClientLevel,
            x: Double, y: Double, z: Double,
            xSpeed: Double, ySpeed: Double, zSpeed: Double,
            random: RandomSource
        ): Particle {
            return StylizedWindParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites)
        }
    }
}
