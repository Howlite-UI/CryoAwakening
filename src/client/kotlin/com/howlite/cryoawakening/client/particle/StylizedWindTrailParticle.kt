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
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Particule StylizedWindTrailParticle — Ligne de vent blanche épurée se déployant en entonnoir (V-shape).
 *
 * Implémente :
 * - Trajectoire se déployant progressivement en V vers l'extérieur au fur et à mesure qu'elle monte.
 * - Nombre de particules réduit et épuré.
 * - Boucle 3D ascendantes (loop-the-loop) et rendu en ruban continu blanc pur.
 */
class StylizedWindTrailParticle(
    level: ClientLevel,
    x: Double, y: Double, z: Double,
    xSpeed: Double, ySpeed: Double, zSpeed: Double,
    sprites: FabricSpriteSet
) : SingleQuadParticle(level, x, y, z, sprites.first()) {

    companion object {
        private const val TRAIL_CAPACITY = 45
        private const val SUB_STEPS = 32
    }

    private data class Node(
        val x: Double, val y: Double, val z: Double,
        val oldX: Double, val oldY: Double, val oldZ: Double
    )

    private val trailNodes = ArrayDeque<Node>()

    // Direction radiale de l'entonnoir (V-shape)
    private val funnelAngle: Float = atan2(zSpeed, xSpeed).toFloat()

    private val phaseX: Float = random.nextFloat() * (2f * PI.toFloat())
    private val phaseZ: Float = random.nextFloat() * (2f * PI.toFloat())
    private val baseYSpeed: Double = ySpeed.coerceAtLeast(0.005)

    // Loop-the-loop 3D ascendant (~30% des traînées)
    private val hasLoop: Boolean = random.nextFloat() < 0.30f
    private val loopStartAge: Int = 18 + random.nextInt(18)
    private var isLooping: Boolean = false
    private var loopTicks: Int = 0
    private var loopAngle: Float = 0f
    private var loopAxisX: Float = 1f
    private var loopAxisZ: Float = 0f

    init {
        xd = xSpeed
        yd = baseYSpeed
        zd = zSpeed

        hasPhysics = false

        // Durée de vie longue pour s'élever haut (140 à 200 ticks)
        lifetime = 140 + random.nextInt(61)

        quadSize = 0.040f + random.nextFloat() * 0.010f

        rCol = 1.0f
        gCol = 1.0f
        bCol = 1.0f
        setAlpha(0.95f)

        gravity = 0f
        setSprite(sprites.first())

        pushNode(x, y, z, xo, yo, zo)
    }

    private fun pushNode(currX: Double, currY: Double, currZ: Double, prevX: Double, prevY: Double, prevZ: Double) {
        trailNodes.addLast(Node(currX, currY, currZ, prevX, prevY, prevZ))
        if (trailNodes.size > TRAIL_CAPACITY) {
            trailNodes.removeFirst()
        }
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z

        if (age++ >= lifetime) {
            remove()
            return
        }

        val t = age.toFloat()

        if (hasLoop && age == loopStartAge && !isLooping) {
            isLooping = true
            loopTicks = 0
            loopAngle = 0f
            if (random.nextBoolean()) {
                loopAxisX = 1.0f; loopAxisZ = 0.2f
            } else {
                loopAxisX = 0.2f; loopAxisZ = 1.0f
            }
        }

        if (!isLooping) {
            // Poussée continue radiale vers l'extérieur pour former l’entonnoir en V
            val funnelPush = 0.0032
            xd += cos(funnelAngle.toDouble()) * funnelPush
            zd += sin(funnelAngle.toDouble()) * funnelPush

            // Oscillation sinusoïdale organique
            xd += sin((t * 0.12f + phaseX).toDouble()) * 0.005
            zd += cos((t * 0.12f + phaseZ).toDouble()) * 0.005
            yd = baseYSpeed + sin((t * 0.07f).toDouble()) * 0.005
        } else {
            // Loop-the-loop 3D avec poursuite de l'évasement
            loopTicks++
            loopAngle += 15f
            val rad = Math.toRadians(loopAngle.toDouble())

            xd = cos(rad) * 0.08 * loopAxisX + cos(funnelAngle.toDouble()) * 0.015
            yd = baseYSpeed + (sin(rad) + 1.0) * 0.035
            zd = cos(rad) * 0.08 * loopAxisZ + sin(funnelAngle.toDouble()) * 0.015

            if (loopTicks >= 24 || loopAngle >= 360f) {
                isLooping = false
            }
        }

        // Déplacement direct ascendant
        x += xd
        y += yd
        z += zd

        pushNode(x, y, z, xo, yo, zo)

        // Amortissement horizontal très doux pour laisser le vent tourner loin
        xd *= 0.992
        zd *= 0.992

        val lifeRatio = age.toFloat() / lifetime.toFloat()
        if (lifeRatio > 0.80f) {
            setAlpha(Mth.lerp((lifeRatio - 0.80f) / 0.20f, 0.95f, 0.0f))
        }
    }

    override fun getBoundingBox(): AABB =
        AABB(x - 40.0, y - 10.0, z - 40.0, x + 40.0, y + 25.0, z + 40.0)

    private fun getInterpolatedPos(s: Float, partialTick: Float): Vec3 {
        val total = trailNodes.size
        if (total == 0) return Vec3(x, y, z)
        val list = trailNodes.toList()
        if (total == 1) {
            val n = list[0]
            return Vec3(
                Mth.lerp(partialTick.toDouble(), n.oldX, n.x),
                Mth.lerp(partialTick.toDouble(), n.oldY, n.y),
                Mth.lerp(partialTick.toDouble(), n.oldZ, n.z)
            )
        }

        val scaled = s.coerceIn(0.0f, 1.0f) * (total - 1)
        val idx1 = scaled.toInt().coerceIn(0, total - 1)
        val idx2 = (idx1 + 1).coerceIn(0, total - 1)
        val frac = scaled - idx1

        val n1 = list[idx1]
        val n2 = list[idx2]

        val x1 = Mth.lerp(partialTick.toDouble(), n1.oldX, n1.x)
        val y1 = Mth.lerp(partialTick.toDouble(), n1.oldY, n1.y)
        val z1 = Mth.lerp(partialTick.toDouble(), n1.oldZ, n1.z)

        val x2 = Mth.lerp(partialTick.toDouble(), n2.oldX, n2.x)
        val y2 = Mth.lerp(partialTick.toDouble(), n2.oldY, n2.y)
        val z2 = Mth.lerp(partialTick.toDouble(), n2.oldZ, n2.z)

        return Vec3(
            Mth.lerp(frac.toDouble(), x1, x2),
            Mth.lerp(frac.toDouble(), y1, y2),
            Mth.lerp(frac.toDouble(), z1, z2)
        )
    }

    override fun extract(renderState: QuadParticleRenderState, camera: Camera, partialTick: Float) {
        if (trailNodes.size < 2) return

        val camPos = camera.position()
        val light = getLightCoords(partialTick)
        val u0 = getU0()
        val u1 = getU1()
        val v0 = getV0()
        val v1 = getV1()

        val baseQuat = Quaternionf()
        facingCameraMode.setRotation(baseQuat, camera, partialTick)

        for (step in 0..SUB_STEPS) {
            val progress = step.toFloat() / SUB_STEPS

            val pA = getInterpolatedPos(progress, partialTick)
            val pB = getInterpolatedPos((progress + 0.03f).coerceAtMost(1.0f), partialTick)

            val dirX = (pB.x - pA.x).toFloat()
            val dirY = (pB.y - pA.y).toFloat()
            val dirZ = (pB.z - pA.z).toFloat()
            val len = Mth.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)

            if (len < 0.0001f) continue

            val taper = sin((progress * PI).toDouble()).toFloat()
            val segmentWidth = quadSize * (0.45f + 0.55f * taper)

            val nodeAlpha = alpha * (0.10f + 0.90f * progress)
            val colorARGB = ARGB.colorFromFloat(nodeAlpha, 1.0f, 1.0f, 1.0f)

            val relX = (pA.x - camPos.x).toFloat()
            val relY = (pA.y - camPos.y).toFloat()
            val relZ = (pA.z - camPos.z).toFloat()

            val quat = Quaternionf(baseQuat)
            val angle = java.lang.Math.atan2(dirY.toDouble(), Mth.sqrt(dirX * dirX + dirZ * dirZ).toDouble()).toFloat()
            quat.rotateZ(angle)

            renderState.add(
                layer,
                relX, relY, relZ,
                quat.x, quat.y, quat.z, quat.w,
                segmentWidth,
                u0, u1, v0, v1,
                colorARGB,
                light
            )
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
            return StylizedWindTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites)
        }
    }
}
