package com.howlite.cryoawakening.client.render

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler
import com.howlite.cryoawakening.infiltration.InfiltrationPhase
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.Mth
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * InfiltrationWorldRenderer
 *
 * Rendu 3D dans le monde (fidèle à Zelda: Tears of the Kingdom Ascend) :
 * 1. AIMING :
 *    - Projection lumineuse plate sous le plafond ciblé (vert émeraude si valide, rouge avec croix X si invalide)
 *      + faisceau vertical translucide et anneaux montants.
 * 2. RISING_TO_CEILING :
 *    - Portail d'entrée au plafond qui s'élargit + colonne lumineuse vibrante + onde de décollage.
 * 3. SWIMMING_THROUGH / RETURNING (Vortex 3D complet autour du personnage qui nage !) :
 *    - Colonne d'aurore verticale douce centrée sur le joueur (couches imbriquées).
 *    - 6 rubans d'énergie 3D en double/triple hélice qui s'entrelacent DEVANT et DERRIÈRE le personnage
 *      (double couche : halo émeraude néon large + filament blanc-menthe ultra-brillant), orientés vers la caméra
 *      pour rester magnifiques sous n'importe quel angle d'orbite 360° !
 *    - 110 lucioles/étincelles 3D qui défilent verticalement avec une vraie parallaxe 3D autour du joueur.
 * 4. PEEKING / EMERGING :
 *    - Portail lumineux circulaire posé au ras du sol autour des épaules du joueur.
 */
object InfiltrationWorldRenderer {

    fun register() {
        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            render(context)
        }
    }

    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val phase = InfiltrationClientHandler.phase
        if (phase == InfiltrationPhase.IDLE) return

        val camPos = context.levelState().cameraRenderState.pos
        val poseStack = context.poseStack()
        val collector = context.submitNodeCollector()
        val timeSec = (System.currentTimeMillis() % 1000000L) / 1000.0f
        val level = client.level ?: return

        when (phase) {
            InfiltrationPhase.AIMING -> {
                val target = InfiltrationClientHandler.currentTarget ?: return
                val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
                val aimProgress = InfiltrationClientHandler.getAimAnim(partialTick).coerceIn(0.05f, 1.0f)
                val isValid = target.valid
                val isErrorPulse = InfiltrationClientHandler.invalidFlashTicks > 0
                val hasSurface = target.ceilingBlockPos != null

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    renderProceduralTexelPortal(
                        pose = pose,
                        consumer = consumer,
                        level = level,
                        worldCx = target.columnX,
                        surfaceY = target.ceilingHitY,
                        worldCz = target.columnZ,
                        camX = camPos.x,
                        camY = camPos.y,
                        camZ = camPos.z,
                        isCeiling = true,
                        conformToSurface = hasSurface,
                        isValid = isValid,
                        errorPulse = isErrorPulse,
                        openProgress = aimProgress,
                        fadeAlpha = 1.0f,
                        timeSec = timeSec
                    )
                    if (isValid) {
                        renderPixelDrippingMotes(
                            pose, consumer,
                            worldCx = target.columnX,
                            surfaceY = target.ceilingHitY,
                            worldCz = target.columnZ,
                            playerY = player.y,
                            camX = camPos.x, camY = camPos.y, camZ = camPos.z,
                            isCeiling = true,
                            timeSec = timeSec,
                            intensity = aimProgress
                        )
                    }
                }
                poseStack.popPose()
            }

            InfiltrationPhase.RISING_TO_CEILING -> {
                val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
                val progress = InfiltrationClientHandler.getPhaseProgress(partialTick)

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    // Portail plafond qui s'illumine à pleine puissance à l'entrée
                    renderProceduralTexelPortal(
                        pose = pose,
                        consumer = consumer,
                        level = level,
                        worldCx = InfiltrationClientHandler.targetX,
                        surfaceY = InfiltrationClientHandler.ceilingHitY,
                        worldCz = InfiltrationClientHandler.targetZ,
                        camX = camPos.x,
                        camY = camPos.y,
                        camZ = camPos.z,
                        isCeiling = true,
                        conformToSurface = true,
                        isValid = true,
                        errorPulse = false,
                        openProgress = 1.0f,
                        fadeAlpha = 1.0f,
                        timeSec = timeSec * 1.45f
                    )
                    // Portail de décollage qui se dissipe au sol sous les pieds
                    renderProceduralTexelPortal(
                        pose = pose,
                        consumer = consumer,
                        level = level,
                        worldCx = InfiltrationClientHandler.originX,
                        surfaceY = InfiltrationClientHandler.originY,
                        worldCz = InfiltrationClientHandler.originZ,
                        camX = camPos.x,
                        camY = camPos.y,
                        camZ = camPos.z,
                        isCeiling = false,
                        conformToSurface = true,
                        isValid = true,
                        errorPulse = false,
                        openProgress = 0.78f,
                        fadeAlpha = (1.0f - progress).coerceIn(0.0f, 1.0f),
                        timeSec = timeSec
                    )
                    // Particules pixel-art qui filent entre le départ et le plafond
                    renderPixelDrippingMotes(
                        pose, consumer,
                        worldCx = InfiltrationClientHandler.targetX,
                        surfaceY = InfiltrationClientHandler.ceilingHitY,
                        worldCz = InfiltrationClientHandler.targetZ,
                        playerY = InfiltrationClientHandler.originY,
                        camX = camPos.x, camY = camPos.y, camZ = camPos.z,
                        isCeiling = true,
                        timeSec = timeSec * 1.6f,
                        intensity = 1.0f
                    )
                }
                poseStack.popPose()
            }

            InfiltrationPhase.SWIMMING_THROUGH, InfiltrationPhase.RETURNING -> {
                val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
                val px = Mth.lerp(partialTick.toDouble(), player.xOld, player.x)
                val py = Mth.lerp(partialTick.toDouble(), player.yOld, player.y) + 0.95
                val pz = Mth.lerp(partialTick.toDouble(), player.zOld, player.z)

                val isReverse = (phase == InfiltrationPhase.RETURNING)

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    renderPixelArt3DVortex(
                        pose = pose,
                        consumer = consumer,
                        playerWorldX = px,
                        playerWorldY = py,
                        playerWorldZ = pz,
                        camX = camPos.x,
                        camY = camPos.y,
                        camZ = camPos.z,
                        timeSec = timeSec,
                        isReverse = isReverse,
                        seed = InfiltrationClientHandler.vortexSeed
                    )
                }
                poseStack.popPose()
            }

            InfiltrationPhase.PEEKING, InfiltrationPhase.EMERGING -> {
                val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
                val fade = if (phase == InfiltrationPhase.EMERGING) {
                    (1.0f - InfiltrationClientHandler.getPhaseProgress(partialTick)).coerceIn(0.0f, 1.0f)
                } else {
                    1.0f
                }

                poseStack.pushPose()
                collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, consumer ->
                    renderProceduralTexelPortal(
                        pose = pose,
                        consumer = consumer,
                        level = level,
                        worldCx = InfiltrationClientHandler.targetX,
                        surfaceY = InfiltrationClientHandler.exitFloorY,
                        worldCz = InfiltrationClientHandler.targetZ,
                        camX = camPos.x,
                        camY = camPos.y,
                        camZ = camPos.z,
                        isCeiling = false,
                        conformToSurface = true,
                        isValid = true,
                        errorPulse = false,
                        openProgress = 1.0f,
                        fadeAlpha = fade,
                        timeSec = timeSec
                    )
                    renderPixelDrippingMotes(
                        pose, consumer,
                        worldCx = InfiltrationClientHandler.targetX,
                        surfaceY = InfiltrationClientHandler.exitFloorY,
                        worldCz = InfiltrationClientHandler.targetZ,
                        playerY = InfiltrationClientHandler.exitFloorY + 1.8,
                        camX = camPos.x, camY = camPos.y, camZ = camPos.z,
                        isCeiling = false,
                        timeSec = timeSec,
                        intensity = fade
                    )
                }
                poseStack.popPose()
            }

            InfiltrationPhase.IDLE -> {}
        }
    }

    // =========================================================================
    // VORTEX 3D PIXEL-ART / VOXEL ALIGNÉ SUR LA GRILLE DU MONDE MINECRAFT
    // =========================================================================

    private data class PixelPalette(
        val deepR: Int, val deepG: Int, val deepB: Int,       // Halo sombre crénelé extérieur
        val midR: Int, val midG: Int, val midB: Int,          // Émeraude / Azur intermédiaire
        val brightR: Int, val brightG: Int, val brightB: Int, // Turquoise / Cyan vif
        val coreR: Int, val coreG: Int, val coreB: Int        // Cœur blanc-menthe / blanc-glace
    )

    private val EMERALD_PIXEL_PALETTE = PixelPalette(
        deepR = 10, deepG = 72, deepB = 68,
        midR = 16, midG = 168, midB = 132,
        brightR = 46, brightG = 248, brightB = 186,
        coreR = 224, coreG = 255, coreB = 244
    )

    private val SAPPHIRE_PIXEL_PALETTE = PixelPalette(
        deepR = 14, deepG = 42, deepB = 96,
        midR = 24, midG = 122, midB = 220,
        brightR = 68, brightG = 204, brightB = 255,
        coreR = 228, coreG = 246, coreB = 255
    )

    private fun renderPixelArt3DVortex(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        playerWorldX: Double,
        playerWorldY: Double,
        playerWorldZ: Double,
        camX: Double,
        camY: Double,
        camZ: Double,
        timeSec: Float,
        isReverse: Boolean,
        seed: Int
    ) {
        val pal = if (isReverse) SAPPHIRE_PIXEL_PALETTE else EMERALD_PIXEL_PALETTE
        val flowDir = if (isReverse) -1.0f else 1.0f

        val cx = (playerWorldX - camX).toFloat()
        val cy = (playerWorldY - camY).toFloat()
        val cz = (playerWorldZ - camZ).toFloat()

        // 1. Cône & colonne lumineuse procédurale asymétrique (fumée/aurore en pixels irréguliers)
        drawProceduralPixelPillar(pose, consumer, cx, cy, cz, timeSec, flowDir, seed, pal)

        // 2. Anneau pixel-art organique et légèrement fissuré à la base du courant
        val baseRingY = if (isReverse) (playerWorldY + 3.9) else (playerWorldY - 3.6)
        drawProceduralPixelPortalRing(
            pose, consumer,
            worldCx = playerWorldX, worldY = baseRingY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            outerRadiusBlocks = 1.65f,
            timeSec = timeSec,
            seed = seed,
            pal = pal
        )

        // 3. 5 courants d'énergie voxel asymétriques, turbulents et imparfaits (bruit FBM + éclats détachés + coupures organiques)
        // Courant héroïque 1 : épais, sinueux et instable
        drawProceduralVoxelStrand(
            pose, consumer,
            worldCx = playerWorldX, worldCy = playerWorldY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            voxelSize = 0.125f,
            baseAngle = hashFloat(seed + 11) * (PI * 2.0).toFloat(),
            baseRadius = 1.32f,
            twistRate = 0.46f,
            spinSpeed = 1.75f * flowDir,
            scrollSpeed = 4.3f * flowDir,
            turbulence = 0.62f,
            isHero = true,
            breakThreshold = 0.12f,
            strandSeed = seed xor 0x1A3B,
            timeSec = timeSec,
            pal = pal
        )
        // Courant héroïque 2 : asymétrique (pas exactement opposé à π, vitesse et rayon décalés)
        drawProceduralVoxelStrand(
            pose, consumer,
            worldCx = playerWorldX, worldCy = playerWorldY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            voxelSize = 0.125f,
            baseAngle = hashFloat(seed + 11) * (PI * 2.0).toFloat() + 2.65f,
            baseRadius = 1.48f,
            twistRate = 0.54f,
            spinSpeed = 2.05f * flowDir,
            scrollSpeed = 4.9f * flowDir,
            turbulence = 0.72f,
            isHero = true,
            breakThreshold = 0.16f,
            strandSeed = seed xor 0x4F91,
            timeSec = timeSec,
            pal = pal
        )
        // Filament vaporeux 3 : fin, large, se fragmente parfois en braises pixelisées
        drawProceduralVoxelStrand(
            pose, consumer,
            worldCx = playerWorldX, worldCy = playerWorldY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            voxelSize = 0.125f,
            baseAngle = hashFloat(seed + 29) * (PI * 2.0).toFloat() + 1.1f,
            baseRadius = 1.86f,
            twistRate = 0.37f,
            spinSpeed = 2.4f * flowDir,
            scrollSpeed = 5.4f * flowDir,
            turbulence = 0.90f,
            isHero = false,
            breakThreshold = 0.34f,
            strandSeed = seed xor 0x7C2D,
            timeSec = timeSec,
            pal = pal
        )
        // Filament erratique 4 : contre-courant nerveux proche du personnage
        drawProceduralVoxelStrand(
            pose, consumer,
            worldCx = playerWorldX, worldCy = playerWorldY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            voxelSize = 0.125f,
            baseAngle = hashFloat(seed + 47) * (PI * 2.0).toFloat() + 4.2f,
            baseRadius = 1.04f,
            twistRate = 0.66f,
            spinSpeed = -1.45f * flowDir,
            scrollSpeed = 3.7f * flowDir,
            turbulence = 0.55f,
            isHero = false,
            breakThreshold = 0.38f,
            strandSeed = seed xor 0x2E85,
            timeSec = timeSec,
            pal = pal
        )
        // Filament lointain 5 : traînée éparse qui danse sur l'extérieur du vortex
        drawProceduralVoxelStrand(
            pose, consumer,
            worldCx = playerWorldX, worldCy = playerWorldY, worldCz = playerWorldZ,
            camX = camX, camY = camY, camZ = camZ,
            voxelSize = 0.125f,
            baseAngle = hashFloat(seed + 83) * (PI * 2.0).toFloat() + 5.1f,
            baseRadius = 2.25f,
            twistRate = 0.29f,
            spinSpeed = 1.15f * flowDir,
            scrollSpeed = 3.2f * flowDir,
            turbulence = 1.05f,
            isHero = false,
            breakThreshold = 0.45f,
            strandSeed = seed xor 0x6B17,
            timeSec = timeSec,
            pal = pal
        )

        // 4. Particules pixel-art turbulentes (regroupements naturels, trajectoires sinueuses et traînées rapides)
        drawProceduralPixelMotes(
            pose, consumer,
            cx = cx, cy = cy, cz = cz,
            count = 115,
            timeSec = timeSec,
            flowDir = flowDir,
            seed = seed,
            pal = pal
        )
    }

    /**
     * Faisceau central et brume d'arrière-plan en pixel-art procédural :
     * bords gauche et droit indépendants et crénelés par du bruit 2D, poches de lumière
     * irrégulières, et panaches pixelisés qui montent de façon organique (zéro symétrie mécanique).
     */
    private fun drawProceduralPixelPillar(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        timeSec: Float,
        flowDir: Float,
        seed: Int,
        pal: PixelPalette
    ) {
        var rx = -cz
        var rz = cx
        val len = sqrt(rx * rx + rz * rz)
        if (len < 1e-4f) {
            rx = 1.0f
            rz = 0.0f
        } else {
            rx /= len
            rz /= len
        }

        val toCamX = -cx
        val toCamZ = -cz
        val tcLen = sqrt(toCamX * toCamX + toCamZ * toCamZ).coerceAtLeast(1e-4f)
        val backX = cx - (toCamX / tcLen) * 0.78f
        val backZ = cz - (toCamZ / tcLen) * 0.78f

        val seedOffset = (seed and 0x3FF) * 0.137f

        // 1. Silhouette extérieure en brume pixelisée asymétrique (gauche et droite perturbées séparément)
        val pixelUnit = 0.1875f
        val sliceH = 0.42f
        val sliceCount = 40
        val startY = cy - (sliceCount * sliceH) * 0.5f

        for (i in 0 until sliceCount) {
            val u = i.toFloat() / (sliceCount - 1)
            val y0 = startY + i * sliceH
            val y1 = y0 + sliceH
            val edgeFade = sin(u * PI).toFloat()
            if (edgeFade < 0.08f) continue

            val flare = if (flowDir > 0f) u else (1.0f - u)
            val scrollY = i * 0.28f + timeSec * 2.8f * flowDir + seedOffset

            // Largeurs gauche et droite indépendantes grâce au bruit FBM -> silhouettes vivantes et irrégulières !
            val leftNoise = fbm1D(scrollY, seed + 101)
            val rightNoise = fbm1D(scrollY * 1.07f + 7.3f, seed + 211)

            val leftCols = (4.5f + flare * 9.5f + (leftNoise - 0.5f) * 6.5f).toInt().coerceIn(3, 18)
            val rightCols = (4.5f + flare * 9.5f + (rightNoise - 0.5f) * 6.5f).toInt().coerceIn(3, 18)

            // Légère variation d'opacité par strate (effet courant d'air)
            val density = 0.70f + 0.45f * fbm1D(scrollY * 0.65f, seed + 307)
            val a = (52.0f * edgeFade * density).toInt().coerceIn(0, 255)
            emitVerticalSteppedQuad(
                pose, consumer, backX, backZ, rx, rz,
                -leftCols * pixelUnit, rightCols * pixelUnit,
                y0, y1,
                pal.deepR, pal.deepG, pal.deepB, a
            )
        }

        // 2. Colonnes de flux lumineux irrégulières pilotées par un champ de bruit 2D (trous, sursauts et volutes !)
        val beamStep = 0.125f
        val bandH = 0.375f
        val numBands = 38
        val bandStartY = cy - (numBands * bandH) * 0.5f
        val flowShift = timeSec * 5.2f * flowDir

        for (col in -8..7) {
            val colCenter = col + 0.5f
            val x0 = col * beamStep
            val x1 = (col + 1) * beamStep

            for (bIdx in 0 until numBands) {
                val u = bIdx.toFloat() / (numBands - 1)
                val env = sin(u * PI).toFloat()
                if (env < 0.08f) continue

                // Le centre du faisceau ondule doucement selon Y (ne forme plus un tube rigide droit)
                val swayCenter = (fbm1D(bIdx * 0.16f + timeSec * 1.4f * flowDir + seedOffset, seed + 409) - 0.5f) * 3.0f
                val dist = kotlin.math.abs(colCenter - swayCenter)

                // Champ de bruit 2D quantifié pour des traînées lumineuses irrégulières qui filent et s'effilochent
                val sampleY = bIdx * 0.24f + flowShift + seedOffset
                val turb = fbm2D(col * 0.42f, sampleY, seed + 523)

                // Seuil de visibilité : crée des trous naturels et des mèches d'énergie sur les côtés
                val radialFalloff = (1.0f - dist / 7.5f).coerceIn(0.0f, 1.0f)
                val energy = radialFalloff * 0.72f + turb * 0.58f - 0.28f
                if (energy < 0.18f) continue

                val y0 = bandStartY + bIdx * bandH
                val y1 = y0 + bandH

                // 4 paliers pixel-art nets selon l'énergie locale (pas de dégradé lisse)
                when {
                    energy > 0.78f -> {
                        val a = (138.0f * env).toInt().coerceIn(0, 255)
                        emitVerticalSteppedQuad(pose, consumer, backX, backZ, rx, rz, x0, x1, y0, y1, pal.coreR, pal.coreG, pal.coreB, a)
                    }
                    energy > 0.56f -> {
                        val a = (108.0f * env).toInt().coerceIn(0, 255)
                        emitVerticalSteppedQuad(pose, consumer, backX, backZ, rx, rz, x0, x1, y0, y1, pal.brightR, pal.brightG, pal.brightB, a)
                    }
                    energy > 0.36f -> {
                        val a = (76.0f * env).toInt().coerceIn(0, 255)
                        emitVerticalSteppedQuad(pose, consumer, backX, backZ, rx, rz, x0, x1, y0, y1, pal.midR, pal.midG, pal.midB, a)
                    }
                    else -> {
                        val a = (44.0f * env).toInt().coerceIn(0, 255)
                        emitVerticalSteppedQuad(pose, consumer, backX, backZ, rx, rz, x0, x1, y0, y1, pal.deepR, pal.deepG, pal.deepB, a)
                    }
                }
            }
        }
    }

    private fun emitVerticalSteppedQuad(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        originX: Float, originZ: Float,
        rightX: Float, rightZ: Float,
        offsetLeft: Float, offsetRight: Float,
        y0: Float, y1: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val xL = originX + rightX * offsetLeft
        val zL = originZ + rightZ * offsetLeft
        val xR = originX + rightX * offsetRight
        val zR = originZ + rightZ * offsetRight

        consumer.addVertex(pose, xL, y0, zL).setColor(r, g, b, a)
        consumer.addVertex(pose, xR, y0, zR).setColor(r, g, b, a)
        consumer.addVertex(pose, xR, y1, zR).setColor(r, g, b, a)
        consumer.addVertex(pose, xL, y1, zL).setColor(r, g, b, a)
    }

    /**
     * Courant d'énergie voxel 3D procédural et imparfait :
     * - Trajectoire perturbée par du bruit FBM en rayon, en angle et en dérive latérale (plus de ressort régulier !).
     * - Épaisseur et intensité variables : sursauts brillants, amincissements et micro-coupures naturelles.
     * - Éclats de pixels détachés qui s'écaillent autour du courant pour un vrai ressenti pixel-art organique.
     */
    private fun drawProceduralVoxelStrand(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        worldCx: Double,
        worldCy: Double,
        worldCz: Double,
        camX: Double,
        camY: Double,
        camZ: Double,
        voxelSize: Float,
        baseAngle: Float,
        baseRadius: Float,
        twistRate: Float,
        spinSpeed: Float,
        scrollSpeed: Float,
        turbulence: Float,
        isHero: Boolean,
        breakThreshold: Float,
        strandSeed: Int,
        timeSec: Float,
        pal: PixelPalette
    ) {
        val invVoxel = 1.0 / voxelSize.toDouble()
        val halfSpanY = 10.5
        val sampleSteps = 165

        var lastIx = Int.MIN_VALUE
        var lastIy = Int.MIN_VALUE
        var lastIz = Int.MIN_VALUE

        val halfCore = voxelSize * 0.50f
        val halfShell = voxelSize * 0.90f

        for (i in 0..sampleSteps) {
            val u = i.toFloat() / sampleSteps
            val localY = -halfSpanY + u * (halfSpanY * 2.0)

            // Coordonnée de bruit qui défile le long du courant
            val flowCoord = localY.toFloat() + timeSec * scrollSpeed

            // Perturbations FBM organiques (rayon, torsion angulaire, décalage latéral X/Z)
            val radNoise = (fbm1D(flowCoord * 0.28f, strandSeed + 13) - 0.5f) * 2.0f
            val angleWarp = (fbm1D(flowCoord * 0.22f + 11.4f, strandSeed + 37) - 0.5f) * 2.2f * turbulence
            val driftX = (fbm1D(flowCoord * 0.19f + 23.1f, strandSeed + 71) - 0.5f) * 0.70f * turbulence
            val driftZ = (fbm1D(flowCoord * 0.19f + 41.7f, strandSeed + 97) - 0.5f) * 0.70f * turbulence

            // Modulation d'intensité / coupures naturelles le long du filament
            val intensityNoise = fbm1D(flowCoord * 0.34f + 5.2f, strandSeed + 149)
            if (intensityNoise < breakThreshold) {
                // Coupure organique du courant : réinitialise la déduplication pour la reprise
                lastIx = Int.MIN_VALUE
                continue
            }

            val theta = flowCoord * twistRate + baseAngle + timeSec * spinSpeed + angleWarp
            val bulge = 0.20f * sin(u * PI).toFloat()
            val rad = (baseRadius + radNoise * 0.58f * turbulence + bulge).coerceAtLeast(0.58f)

            val wx = worldCx + cos(theta) * rad + driftX
            val wy = worldCy + localY
            val wz = worldCz + sin(theta) * rad + driftZ

            val ix = kotlin.math.floor(wx * invVoxel).toInt()
            val iy = kotlin.math.floor(wy * invVoxel).toInt()
            val iz = kotlin.math.floor(wz * invVoxel).toInt()

            if (ix == lastIx && iy == lastIy && iz == lastIz) continue
            lastIx = ix
            lastIy = iy
            lastIz = iz

            val edgeFade = Mth.clamp(sin(u * PI).toFloat() * 1.65f, 0.0f, 1.0f)
            if (edgeFade <= 0.05f) continue

            // Énergie locale normalisée au-dessus du seuil de coupure (0..1)
            val localEnergy = ((intensityNoise - breakThreshold) / (1.0f - breakThreshold)).coerceIn(0.0f, 1.0f)
            val cellRand = hashFloat(ix * 73856093 xor iy * 19349663 xor iz * 83492791 xor strandSeed)

            val vx = ((ix + 0.5) * voxelSize - camX).toFloat()
            val vy = ((iy + 0.5) * voxelSize - camY).toFloat()
            val vz = ((iz + 0.5) * voxelSize - camZ).toFloat()

            if (isHero) {
                // Épaisseur irrégulière selon les sursauts d'énergie
                val isSurge = localEnergy > 0.62f
                val isThinning = localEnergy < 0.24f

                if (!isThinning) {
                    val shellScale = if (isSurge) halfShell * 1.12f else halfShell * 0.88f
                    val shellAlpha = (edgeFade * (75.0f + localEnergy * 55.0f)).toInt().coerceIn(0, 255)
                    emitVoxelCube(pose, consumer, vx, vy, vz, shellScale, pal.brightR, pal.brightG, pal.brightB, shellAlpha)
                }

                val coreAlpha = (edgeFade * (175.0f + localEnergy * 75.0f)).toInt().coerceIn(0, 255)
                when {
                    isSurge || cellRand > 0.42f -> emitVoxelCube(pose, consumer, vx, vy, vz, halfCore, pal.coreR, pal.coreG, pal.coreB, coreAlpha)
                    !isThinning -> emitVoxelCube(pose, consumer, vx, vy, vz, halfCore, pal.brightR, pal.brightG, pal.brightB, coreAlpha)
                    else -> emitVoxelCube(pose, consumer, vx, vy, vz, halfCore * 0.8f, pal.midR, pal.midG, pal.midB, (coreAlpha * 0.8f).toInt())
                }

                // Imperfections pixel-art : éclats/braises de 1 voxel qui se détachent de façon irrégulière !
                if (cellRand > 0.72f) {
                    val offX = if (hashFloat(i * 19 + strandSeed) > 0.5f) 1 else -1
                    val offY = ((hashFloat(i * 31 + strandSeed) * 3.0f).toInt() - 1)
                    val offZ = if (hashFloat(i * 43 + strandSeed) > 0.5f) 1 else -1
                    val distStep = if (cellRand > 0.88f) 2 else 1
                    val fx = (((ix + offX * distStep) + 0.5) * voxelSize - camX).toFloat()
                    val fy = (((iy + offY) + 0.5) * voxelSize - camY).toFloat()
                    val fz = (((iz + offZ * distStep) + 0.5) * voxelSize - camZ).toFloat()
                    val flakeAlpha = (edgeFade * (120.0f + localEnergy * 95.0f)).toInt().coerceIn(0, 255)
                    if (cellRand > 0.90f) {
                        emitVoxelCube(pose, consumer, fx, fy, fz, halfCore * 0.78f, pal.coreR, pal.coreG, pal.coreB, flakeAlpha)
                    } else {
                        emitVoxelCube(pose, consumer, fx, fy, fz, halfCore * 0.82f, pal.brightR, pal.brightG, pal.brightB, (flakeAlpha * 0.78f).toInt())
                    }
                }
            } else {
                // Filaments secondaires organiques : traînées pointillées et éclats délicats
                val a = (edgeFade * (115.0f + localEnergy * 115.0f)).toInt().coerceIn(0, 255)
                if (localEnergy > 0.70f && cellRand > 0.55f) {
                    emitVoxelCube(pose, consumer, vx, vy, vz, halfCore * 0.92f, pal.coreR, pal.coreG, pal.coreB, a)
                } else if (cellRand > 0.22f) {
                    emitVoxelCube(pose, consumer, vx, vy, vz, halfCore * 0.82f, pal.brightR, pal.brightG, pal.brightB, (a * 0.82f).toInt())
                } else {
                    // Micro-trou ponctuel donnant un rendu pixel-art effiloché
                    emitVoxelCube(pose, consumer, vx, vy, vz, halfCore * 0.65f, pal.midR, pal.midG, pal.midB, (a * 0.55f).toInt())
                }
            }
        }
    }

    /**
     * Cercle pixel-art organique à la base du vortex : rayon légèrement déformé par du bruit angulaire,
     * brèches imparfaites dans l'anneau extérieur et quelques pixels qui s'en échappent vers le haut.
     */
    private fun drawProceduralPixelPortalRing(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        worldCx: Double,
        worldY: Double,
        worldCz: Double,
        camX: Double,
        camY: Double,
        camZ: Double,
        outerRadiusBlocks: Float,
        timeSec: Float,
        seed: Int,
        pal: PixelPalette
    ) {
        val pixelSize = 0.125f
        val halfP = pixelSize * 0.5f
        val halfH = pixelSize * 0.24f
        val gridRad = kotlin.math.ceil(((outerRadiusBlocks + 0.35f) / pixelSize).toDouble()).toInt()

        val baseIx = kotlin.math.floor(worldCx / pixelSize).toInt()
        val baseIz = kotlin.math.floor(worldCz / pixelSize).toInt()
        val ry = (worldY - camY).toFloat()

        for (dx in -gridRad..gridRad) {
            for (dz in -gridRad..gridRad) {
                val distBlocks = sqrt((dx * dx + dz * dz).toFloat()) * pixelSize
                val angle = kotlin.math.atan2(dz.toFloat(), dx.toFloat())

                // Légère déformation angulaire organique du cercle (non-parfait)
                val wobble = (fbm1D((angle + PI.toFloat()) * 1.4f + timeSec * 1.2f, seed + 613) - 0.5f) * 0.32f
                val gapNoise = fbm1D((angle + PI.toFloat()) * 2.3f - timeSec * 1.6f, seed + 719)
                val localOuter = outerRadiusBlocks + wobble

                val isCoreCenter = distBlocks <= 0.36f + wobble * 0.45f
                val isMidGlow = !isCoreCenter && distBlocks <= 0.72f + wobble * 0.55f && gapNoise > 0.28f
                val isOuterRing = distBlocks >= (localOuter - 0.24f) && distBlocks <= localOuter && gapNoise > 0.30f

                if (!isCoreCenter && !isMidGlow && !isOuterRing) continue

                val vx = ((baseIx + dx + 0.5) * pixelSize - camX).toFloat()
                val vz = ((baseIz + dz + 0.5) * pixelSize - camZ).toFloat()
                val cellHash = hashFloat((dx + 64) * 31 + (dz + 64) * 97 + seed)

                // Quelques pixels de la couronne flottent légèrement en hauteur (+1 ou +2 pixels)
                val liftY = if (isOuterRing && cellHash > 0.78f) pixelSize * (if (cellHash > 0.90f) 2.0f else 1.0f) else 0.0f

                when {
                    isCoreCenter -> emitVoxelBox(pose, consumer, vx, ry, vz, halfP, halfH, halfP, pal.coreR, pal.coreG, pal.coreB, 235)
                    isMidGlow -> emitVoxelBox(pose, consumer, vx, ry, vz, halfP, halfH, halfP, pal.brightR, pal.brightG, pal.brightB, 165)
                    isOuterRing -> {
                        if (gapNoise > 0.62f || cellHash > 0.55f) {
                            emitVoxelBox(pose, consumer, vx, ry + liftY, vz, halfP, halfH, halfP, pal.brightR, pal.brightG, pal.brightB, 210)
                        } else {
                            emitVoxelBox(pose, consumer, vx, ry + liftY, vz, halfP, halfH, halfP, pal.midR, pal.midG, pal.midB, 138)
                        }
                    }
                }
            }
        }
    }

    /**
     * Particules pixel-art procédurales :
     * - Regroupement naturel en essaims / tourbillons (non uniforme).
     * - Dérive latérale sinueuse pilotée par du bruit (ne tombe plus en lignes droites mécaniques).
     * - Tailles variées : poussières 1px, braises 2px, carrés encadrés, et traînées verticales rapides de 2 pixels !
     */
    private fun drawProceduralPixelMotes(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        count: Int,
        timeSec: Float,
        flowDir: Float,
        seed: Int,
        pal: PixelPalette
    ) {
        val spanY = 24.0f
        val halfSpanY = spanY * 0.5f

        val toCam = Vector3f()
        val right = Vector3f()
        val up = Vector3f(0.0f, 1.0f, 0.0f)

        for (i in 0 until count) {
            val s = seed xor (i * 2654435761L).toInt()
            val h1 = hashFloat(s + 3)
            val h2 = hashFloat(s + 11)
            val h3 = hashFloat(s + 23)
            val h4 = hashFloat(s + 37)
            val h5 = hashFloat(s + 53)

            // Regroupement organique : certaines particules forment de petits essaims autour d'angles privilégiés
            val clusterId = i % 5
            val clusterAngle = hashFloat(seed + clusterId * 97) * (PI * 2.0).toFloat()
            val angleSpread = if (h5 > 0.35f) (h2 - 0.5f) * 1.4f else (h2 * (PI * 2.0).toFloat())
            val swirlSpeed = (0.06f + h4 * 0.24f) * (if (h1 > 0.15f) 1.0f else -0.7f) * flowDir

            val baseRadius = 1.45f + h1 * h1 * 12.2f
            val speed = 1.6f + h3 * h3 * 6.2f

            var rawRelY = ((h4 * spanY - timeSec * speed * flowDir) % spanY)
            if (rawRelY < 0.0f) rawRelY += spanY
            rawRelY -= halfSpanY
            val relY = (kotlin.math.floor((rawRelY * 16.0f).toDouble()) / 16.0).toFloat()

            // Dérive turbulente naturelle (chaque luciole serpente légèrement dans le vent du vortex)
            val wanderR = (fbm1D(timeSec * 0.9f + i * 0.73f, seed + 809) - 0.5f) * 1.15f
            val wanderA = (fbm1D(timeSec * 0.75f + i * 1.19f, seed + 911) - 0.5f) * 0.65f
            val radius = (baseRadius + wanderR).coerceAtLeast(1.05f)
            val angle = clusterAngle + angleSpread + timeSec * swirlSpeed + wanderA

            val mx = cx + cos(angle) * radius
            val my = cy + relY
            val mz = cz + sin(angle) * radius

            val edgeFade = sin(((relY + halfSpanY) / spanY) * PI).toFloat()
            val flicker = fbm1D(timeSec * (2.2f + h2 * 3.0f) + i * 1.7f, seed + 997)
            // Paliers nets de scintillement pixel-art
            val twinkle = when {
                flicker > 0.62f -> 1.0f
                flicker > 0.42f -> 0.72f
                flicker > 0.26f -> 0.38f
                else -> 0.0f // Disparaît brièvement de façon naturelle !
            }
            val alpha = (edgeFade * twinkle * 245.0f).toInt().coerceIn(0, 255)
            if (alpha <= 10) continue

            toCam.set(-mx, -my, -mz)
            val dist = sqrt(toCam.x * toCam.x + toCam.y * toCam.y + toCam.z * toCam.z)
            if (dist < 0.5f) continue
            toCam.div(dist)

            right.set(toCam.z, 0.0f, -toCam.x)
            if (right.lengthSquared() < 1e-4f) right.set(1.0f, 0.0f, 0.0f) else right.normalize()
            val billboardUp = Vector3f(right).cross(toCam).normalize()
            billboardUp.lerp(up, 0.55f).normalize()

            when {
                // 1. Gros pixel lumineux encadré (braise héroïque rare ~22%)
                h3 > 0.78f -> {
                    val coreHalf = 0.055f + h5 * 0.025f
                    val borderHalf = coreHalf * 2.0f
                    val bx = mx - toCam.x * 0.015f
                    val by = my - toCam.y * 0.015f
                    val bz = mz - toCam.z * 0.015f
                    emitCrispPixelSquare(pose, consumer, bx, by, bz, right, billboardUp, borderHalf, pal.brightR, pal.brightG, pal.brightB, (alpha * 0.46f).toInt())
                    emitCrispPixelSquare(pose, consumer, mx, my, mz, right, billboardUp, coreHalf, pal.coreR, pal.coreG, pal.coreB, alpha)
                }
                // 2. Étincelle rapide avec micro-traînée pixelisée de 2 pixels (~23%)
                h3 > 0.55f -> {
                    val pixHalf = 0.042f
                    emitCrispPixelSquare(pose, consumer, mx, my, mz, right, billboardUp, pixHalf, pal.coreR, pal.coreG, pal.coreB, alpha)
                    // Pixel de traînée juste derrière le mouvement
                    val tailY = my + (0.095f * flowDir)
                    emitCrispPixelSquare(pose, consumer, mx, tailY, mz, right, billboardUp, pixHalf * 0.85f, pal.brightR, pal.brightG, pal.brightB, (alpha * 0.55f).toInt())
                }
                // 3. Poussières 1×1 pixel de tailles et teintes variées (~55%)
                else -> {
                    val pixHalf = 0.026f + h5 * 0.024f
                    if (h2 > 0.55f) {
                        emitCrispPixelSquare(pose, consumer, mx, my, mz, right, billboardUp, pixHalf, pal.coreR, pal.coreG, pal.coreB, (alpha * 0.90f).toInt())
                    } else if (h2 > 0.22f) {
                        emitCrispPixelSquare(pose, consumer, mx, my, mz, right, billboardUp, pixHalf, pal.brightR, pal.brightG, pal.brightB, (alpha * 0.80f).toInt())
                    } else {
                        emitCrispPixelSquare(pose, consumer, mx, my, mz, right, billboardUp, pixHalf, pal.midR, pal.midG, pal.midB, (alpha * 0.72f).toInt())
                    }
                }
            }
        }
    }

    // =========================================================================
    // BRUIT PROCÉDURAL (VALUE NOISE 1D/2D & FBM) POUR EFFETS ORGANIQUES
    // =========================================================================

    private fun valueNoise1D(x: Float, seed: Int): Float {
        val xi = kotlin.math.floor(x.toDouble()).toInt()
        val f = x - xi
        val u = f * f * (3.0f - 2.0f * f)
        val a = hashFloat(xi * 374761393 xor seed)
        val b = hashFloat((xi + 1) * 374761393 xor seed)
        return a + (b - a) * u
    }

    private fun fbm1D(x: Float, seed: Int): Float {
        var total = 0.0f
        var amp = 0.55f
        var freq = 1.0f
        var norm = 0.0f
        for (o in 0 until 3) {
            total += valueNoise1D(x * freq, seed + o * 1013) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2.07f
        }
        return total / norm
    }

    private fun valueNoise2D(x: Float, y: Float, seed: Int): Float {
        val xi = kotlin.math.floor(x.toDouble()).toInt()
        val yi = kotlin.math.floor(y.toDouble()).toInt()
        val fx = x - xi
        val fy = y - yi
        val ux = fx * fx * (3.0f - 2.0f * fx)
        val uy = fy * fy * (3.0f - 2.0f * fy)

        val n00 = hashFloat((xi * 374761393) xor (yi * 668265263) xor seed)
        val n10 = hashFloat(((xi + 1) * 374761393) xor (yi * 668265263) xor seed)
        val n01 = hashFloat((xi * 374761393) xor ((yi + 1) * 668265263) xor seed)
        val n11 = hashFloat(((xi + 1) * 374761393) xor ((yi + 1) * 668265263) xor seed)

        val nx0 = n00 + (n10 - n00) * ux
        val nx1 = n01 + (n11 - n01) * ux
        return nx0 + (nx1 - nx0) * uy
    }

    private fun fbm2D(x: Float, y: Float, seed: Int): Float {
        var total = 0.0f
        var amp = 0.60f
        var freq = 1.0f
        var norm = 0.0f
        for (o in 0 until 3) {
            total += valueNoise2D(x * freq, y * freq, seed + o * 1619) * amp
            norm += amp
            amp *= 0.48f
            freq *= 2.03f
        }
        return total / norm
    }

    private fun emitCrispPixelSquare(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        x: Float, y: Float, z: Float,
        right: Vector3f, up: Vector3f,
        halfSize: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        val rx = right.x * halfSize
        val ry = right.y * halfSize
        val rz = right.z * halfSize
        val ux = up.x * halfSize
        val uy = up.y * halfSize
        val uz = up.z * halfSize

        consumer.addVertex(pose, x - rx - ux, y - ry - uy, z - rz - uz).setColor(r, g, b, a)
        consumer.addVertex(pose, x + rx - ux, y + ry - uy, z + rz - uz).setColor(r, g, b, a)
        consumer.addVertex(pose, x + rx + ux, y + ry + uy, z + rz + uz).setColor(r, g, b, a)
        consumer.addVertex(pose, x - rx + ux, y - ry + uy, z - rz + uz).setColor(r, g, b, a)
    }

    private fun emitVoxelCube(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float, cy: Float, cz: Float,
        half: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        emitVoxelBox(pose, consumer, cx, cy, cz, half, half, half, r, g, b, a)
    }

    /**
     * Émet un mini-voxel 3D aligné sur les axes X/Y/Z du monde Minecraft avec un très léger
     * ombrage directionnel façon bloc Minecraft (dessus 100%, côtés 92%, dessous 84%).
     */
    private fun emitVoxelBox(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        cx: Float, cy: Float, cz: Float,
        hx: Float, hy: Float, hz: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        if (a <= 4) return
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        val sr = (r * 0.92f).toInt(); val sg = (g * 0.92f).toInt(); val sb = (b * 0.92f).toInt()
        val dr = (r * 0.84f).toInt(); val dg = (g * 0.84f).toInt(); val db = (b * 0.84f).toInt()

        // +Y (haut)
        consumer.addVertex(pose, x0, y1, z0).setColor(r, g, b, a)
        consumer.addVertex(pose, x0, y1, z1).setColor(r, g, b, a)
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a)
        consumer.addVertex(pose, x1, y1, z0).setColor(r, g, b, a)

        // -Y (bas)
        consumer.addVertex(pose, x0, y0, z0).setColor(dr, dg, db, a)
        consumer.addVertex(pose, x1, y0, z0).setColor(dr, dg, db, a)
        consumer.addVertex(pose, x1, y0, z1).setColor(dr, dg, db, a)
        consumer.addVertex(pose, x0, y0, z1).setColor(dr, dg, db, a)

        // -Z (nord)
        consumer.addVertex(pose, x0, y0, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y1, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y1, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y0, z0).setColor(sr, sg, sb, a)

        // +Z (sud)
        consumer.addVertex(pose, x1, y0, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y1, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y1, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y0, z1).setColor(sr, sg, sb, a)

        // -X (ouest)
        consumer.addVertex(pose, x0, y0, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y1, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y1, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x0, y0, z0).setColor(sr, sg, sb, a)

        // +X (est)
        consumer.addVertex(pose, x1, y0, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y1, z0).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y1, z1).setColor(sr, sg, sb, a)
        consumer.addVertex(pose, x1, y0, z1).setColor(sr, sg, sb, a)
    }

    private fun hashFloat(seed: Int): Float {
        var n = seed * 374761393 + 668265263
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return ((n and 0x7FFFFFFF).toDouble() / 0x7FFFFFFF.toDouble()).toFloat()
    }

    // =========================================================================
    // PORTAIL PROCÉDURAL ALIGNÉ AU PIXEL PRÈS SUR LA GRILLE 16×16 DES BLOCS
    // (Fusion Twilight Portal sombre/circuits 90° + grille & spirale pixel-art)
    // =========================================================================

    private const val PORTAL_GRID_SIZE: Int = 48 // 48×48 texels = 3×3 blocs Minecraft exacts
    private const val PORTAL_HALF: Int = PORTAL_GRID_SIZE / 2
    private const val TEXEL: Float = 1.0f / 16.0f

    // Masques de couches stockés dans chaque cellule du portail généré
    private const val CELL_SHADOW: Int = 1           // Ombre crénelée noire/abyssale (style Twilight Portal)
    private const val CELL_SHADOW_EDGE: Int = 2      // Bordure semi-opaque des blocs d'ombre
    private const val CELL_GRID: Int = 4             // Fine grille alignée sur les blocs (8px / 16px)
    private const val CELL_GRID_NODE: Int = 8        // Croisement lumineux "+" de la grille
    private const val CELL_CIRCUIT_GLOW: Int = 16    // Halo de 1 pixel autour des circuits orthogonaux
    private const val CELL_CIRCUIT_CORE: Int = 32    // Ligne de circuit 90° lumineuse procédurale
    private const val CELL_CIRCUIT_NODE: Int = 64    // Nœud / angle / terminaison brillante de circuit

    private class CachedPortalPattern(
        val keyX: Int,
        val keyY: Int,
        val keyZ: Int,
        val isCeiling: Boolean,
        val flags: IntArray,
        val circuitDist: ByteArray,
        val maxCircuitDist: Int
    )

    private var cachedCeilingPortal: CachedPortalPattern? = null
    private var cachedFloorPortal: CachedPortalPattern? = null

    private fun getOrBuildPortalPattern(bx: Int, by: Int, bz: Int, isCeiling: Boolean): CachedPortalPattern {
        val existing = if (isCeiling) cachedCeilingPortal else cachedFloorPortal
        if (existing != null && existing.keyX == bx && existing.keyY == by && existing.keyZ == bz) {
            return existing
        }

        val flags = IntArray(PORTAL_GRID_SIZE * PORTAL_GRID_SIZE)
        val circuitDist = ByteArray(PORTAL_GRID_SIZE * PORTAL_GRID_SIZE)
        val seed = (bx * 73856093) xor (by * 19349663) xor (bz * 83492791) xor (if (isCeiling) 0x5A17 else 0x3C91)

        fun idx(gx: Int, gz: Int): Int = gz * PORTAL_GRID_SIZE + gx
        fun inBounds(gx: Int, gz: Int): Boolean = gx in 1..(PORTAL_GRID_SIZE - 2) && gz in 1..(PORTAL_GRID_SIZE - 2)

        // 1. Silhouette sombre crénelée en rectangles imbriqués (style Twilight Portal de l'image 1)
        // Disque central sombre
        for (gx in 0 until PORTAL_GRID_SIZE) {
            for (gz in 0 until PORTAL_GRID_SIZE) {
                val dx = gx - PORTAL_HALF + 0.5f
                val dz = gz - PORTAL_HALF + 0.5f
                val d = sqrt(dx * dx + dz * dz)
                val ang = kotlin.math.atan2(dz, dx)
                val jitter = (fbm1D((ang + PI.toFloat()) * 2.1f, seed + 17) - 0.5f) * 5.0f
                if (d <= 12.0f + jitter) {
                    flags[idx(gx, gz)] = flags[idx(gx, gz)] or CELL_SHADOW
                }
            }
        }

        // Tamponnage procédural de 26 blocs rectangulaires sombres irréguliers autour du centre
        for (rIdx in 0 until 26) {
            val h1 = hashFloat(seed + rIdx * 31 + 3)
            val h2 = hashFloat(seed + rIdx * 31 + 11)
            val h3 = hashFloat(seed + rIdx * 31 + 19)
            val h4 = hashFloat(seed + rIdx * 31 + 29)

            val angle = h1 * (PI * 2.0).toFloat()
            val dist = 7.5f + h2 * 11.5f
            val rcx = (PORTAL_HALF + cos(angle) * dist).toInt()
            val rcz = (PORTAL_HALF + sin(angle) * dist).toInt()
            val rw = 2 + (h3 * 5.0f).toInt()
            val rh = 2 + (h4 * 5.0f).toInt()

            for (x in (rcx - rw / 2)..(rcx + rw / 2)) {
                for (z in (rcz - rh / 2)..(rcz + rh / 2)) {
                    if (inBounds(x, z)) {
                        flags[idx(x, z)] = flags[idx(x, z)] or CELL_SHADOW
                    }
                }
            }
        }

        // Quelques petits îlots noirs détachés en périphérie (1×2 ou 2×2 texels)
        for (sIdx in 0 until 14) {
            val a = hashFloat(seed + 900 + sIdx * 17) * (PI * 2.0).toFloat()
            val d = 16.0f + hashFloat(seed + 900 + sIdx * 23) * 6.5f
            val sx = (PORTAL_HALF + cos(a) * d).toInt()
            val sz = (PORTAL_HALF + sin(a) * d).toInt()
            val sw = 1 + (hashFloat(seed + 900 + sIdx * 37) * 2.0f).toInt()
            val sh = 1 + (hashFloat(seed + 900 + sIdx * 41) * 2.0f).toInt()
            for (x in sx..(sx + sw - 1)) {
                for (z in sz..(sz + sh - 1)) {
                    if (inBounds(x, z)) {
                        flags[idx(x, z)] = flags[idx(x, z)] or CELL_SHADOW_EDGE
                    }
                }
            }
        }

        // 2. Grille lumineuse subtile alignée sur les demi-blocs (8 texels) à l'intérieur de la zone sombre (Image 2)
        for (gx in 2 until PORTAL_GRID_SIZE - 2) {
            for (gz in 2 until PORTAL_GRID_SIZE - 2) {
                if ((flags[idx(gx, gz)] and CELL_SHADOW) == 0) continue
                val dx = gx - PORTAL_HALF
                val dz = gz - PORTAL_HALF
                val dist = sqrt((dx * dx + dz * dz).toFloat())
                if (dist < 7.8f || dist > 19.5f) continue

                // Alignement exact sur les bords/milieux de blocs Minecraft (8 et 16 texels)
                val onXLine = (gx % 8 == 0)
                val onZLine = (gz % 8 == 0)
                if (onXLine || onZLine) {
                    flags[idx(gx, gz)] = flags[idx(gx, gz)] or CELL_GRID
                    if (onXLine && onZLine) {
                        flags[idx(gx, gz)] = flags[idx(gx, gz)] or CELL_GRID_NODE
                        // Petite croix "+" de 1 pixel autour de chaque nœud d'intersection (Image 2)
                        if (inBounds(gx - 1, gz)) flags[idx(gx - 1, gz)] = flags[idx(gx - 1, gz)] or CELL_GRID_NODE
                        if (inBounds(gx + 1, gz)) flags[idx(gx + 1, gz)] = flags[idx(gx + 1, gz)] or CELL_GRID_NODE
                        if (inBounds(gx, gz - 1)) flags[idx(gx, gz - 1)] = flags[idx(gx, gz - 1)] or CELL_GRID_NODE
                        if (inBounds(gx, gz + 1)) flags[idx(gx, gz + 1)] = flags[idx(gx, gz + 1)] or CELL_GRID_NODE
                    }
                }
            }
        }

        // 3. Circuits orthogonaux 90° procéduraux + petites boucles carrées qui rayonnent depuis l'anneau central (Image 1)
        val dirX = intArrayOf(1, 0, -1, 0)
        val dirZ = intArrayOf(0, 1, 0, -1)
        var maxDistRecorded = 1

        fun stampCircuit(gx: Int, gz: Int, step: Int, isNode: Boolean = false) {
            if (!inBounds(gx, gz)) return
            val i = idx(gx, gz)
            flags[i] = flags[i] or CELL_CIRCUIT_CORE or CELL_SHADOW
            if (isNode) flags[i] = flags[i] or CELL_CIRCUIT_NODE
            val clampedStep = step.coerceIn(1, 120).toByte()
            if (circuitDist[i].toInt() == 0 || clampedStep < circuitDist[i]) {
                circuitDist[i] = clampedStep
            }
            if (step > maxDistRecorded) maxDistRecorded = step

            // Ajoute un halo de 1 texel + soutien d'ombre autour du tracé du circuit
            for (ox in -1..1) {
                for (oz in -1..1) {
                    if (ox == 0 && oz == 0) continue
                    val nx = gx + ox
                    val nz = gz + oz
                    if (inBounds(nx, nz)) {
                        val ni = idx(nx, nz)
                        if (ox == 0 || oz == 0) {
                            flags[ni] = flags[ni] or CELL_CIRCUIT_GLOW or CELL_SHADOW
                            if (circuitDist[ni].toInt() == 0) circuitDist[ni] = clampedStep
                        } else {
                            flags[ni] = flags[ni] or CELL_SHADOW_EDGE
                        }
                    }
                }
            }
        }

        val branchCount = 7 + (hashFloat(seed + 503) * 3.0f).toInt() // 7 à 9 branches asymétriques
        for (b in 0 until branchCount) {
            val baseA = (b.toFloat() / branchCount) * (PI * 2.0).toFloat() + (hashFloat(seed + b * 43) - 0.5f) * 0.55f
            var curX = (PORTAL_HALF + cos(baseA) * 7.8f).toInt()
            var curZ = (PORTAL_HALF + sin(baseA) * 7.8f).toInt()

            // Direction initiale pointant globalement vers l'extérieur
            var dir = when {
                kotlin.math.abs(cos(baseA)) >= kotlin.math.abs(sin(baseA)) -> if (cos(baseA) >= 0f) 0 else 2
                else -> if (sin(baseA) >= 0f) 1 else 3
            }

            var stepCounter = 1
            val segments = 4 + (hashFloat(seed + b * 71 + 7) * 4.0f).toInt() // 4 à 7 segments coudés

            for (seg in 0 until segments) {
                val segLen = 2 + (hashFloat(seed + b * 113 + seg * 19) * 4.5f).toInt()
                for (s in 0 until segLen) {
                    val nextX = curX + dirX[dir]
                    val nextZ = curZ + dirZ[dir]
                    if (!inBounds(nextX, nextZ)) break
                    val distFromCenter = sqrt(((nextX - PORTAL_HALF) * (nextX - PORTAL_HALF) + (nextZ - PORTAL_HALF) * (nextZ - PORTAL_HALF)).toFloat())
                    if (distFromCenter < 7.2f || distFromCenter > 22.0f) break

                    curX = nextX
                    curZ = nextZ
                    stepCounter++
                    stampCircuit(curX, curZ, stepCounter, isNode = (s == segLen - 1))
                }

                // Probabilité de former une boucle rectangulaire fermée (signature visuelle de l'image 1 !)
                val loopRoll = hashFloat(seed + b * 151 + seg * 37)
                if (loopRoll > 0.66f && seg in 1..(segments - 1)) {
                    val turnSign = if (hashFloat(seed + b * 197 + seg * 13) > 0.5f) 1 else 3
                    val lw = 3 + (hashFloat(seed + b * 211 + seg * 29) * 3.0f).toInt()
                    val lh = 3 + (hashFloat(seed + b * 229 + seg * 41) * 3.0f).toInt()
                    var lx = curX
                    var lz = curZ
                    var lDir = (dir + turnSign) and 3
                    var loopStep = stepCounter
                    val sides = intArrayOf(lw, lh, lw, lh)
                    for (sideIdx in 0 until 4) {
                        for (k in 0 until sides[sideIdx]) {
                            val nx = lx + dirX[lDir]
                            val nz = lz + dirZ[lDir]
                            if (!inBounds(nx, nz)) break
                            val dc = sqrt(((nx - PORTAL_HALF) * (nx - PORTAL_HALF) + (nz - PORTAL_HALF) * (nz - PORTAL_HALF)).toFloat())
                            if (dc < 7.5f || dc > 22.5f) break
                            lx = nx
                            lz = nz
                            loopStep++
                            stampCircuit(lx, lz, loopStep, isNode = (k == sides[sideIdx] - 1))
                        }
                        lDir = (lDir + turnSign) and 3
                    }
                }

                // Virage net à 90° (gauche ou droite) en privilégiant l'expansion vers l'extérieur
                val turn = if (hashFloat(seed + b * 263 + seg * 53) > 0.5f) 1 else 3
                val candidateDir = (dir + turn) and 3
                val testX = curX + dirX[candidateDir] * 2 - PORTAL_HALF
                val testZ = curZ + dirZ[candidateDir] * 2 - PORTAL_HALF
                val curDistSq = (curX - PORTAL_HALF) * (curX - PORTAL_HALF) + (curZ - PORTAL_HALF) * (curZ - PORTAL_HALF)
                dir = if (testX * testX + testZ * testZ >= curDistSq - 6) candidateDir else ((dir + (4 - turn)) and 3)
            }
            // Terminaison brillante au bout de la branche
            stampCircuit(curX, curZ, stepCounter + 1, isNode = true)
        }

        val built = CachedPortalPattern(bx, by, bz, isCeiling, flags, circuitDist, maxDistRecorded)
        if (isCeiling) cachedCeilingPortal = built else cachedFloorPortal = built
        return built
    }

    /**
     * Rendu du portail procédural aligné 1-pour-1 sur la grille 16×16 pixels des blocs Minecraft :
     * - Vérifie les vrais blocs voisins pour épouser uniquement les faces solides existantes (zéro débordement dans le vide).
     * - Déploiement progressif pixel-par-pixel à l'ouverture (openProgress).
     * - Spirale pixel-art rotative au centre sur fond abyssal (ou croix X rouge si invalide).
     * - Impulsion lumineuse qui parcourt en temps réel les circuits coudés à 90° !
     */
    private fun renderProceduralTexelPortal(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        level: net.minecraft.world.level.Level,
        worldCx: Double,
        surfaceY: Double,
        worldCz: Double,
        camX: Double,
        camY: Double,
        camZ: Double,
        isCeiling: Boolean,
        conformToSurface: Boolean,
        isValid: Boolean,
        errorPulse: Boolean,
        openProgress: Float,
        fadeAlpha: Float,
        timeSec: Float
    ) {
        if (fadeAlpha <= 0.02f) return

        // Ancrage STRICT sur la grille entière de blocs Minecraft (16 texels par bloc)
        val centerBlockX = kotlin.math.floor(worldCx).toInt()
        val centerBlockZ = kotlin.math.floor(worldCz).toInt()
        val surfaceBlockY = if (isCeiling) {
            kotlin.math.floor(surfaceY + 0.05).toInt()
        } else {
            kotlin.math.floor(surfaceY - 0.05).toInt()
        }

        // Origine du coin (0, 0) de la grille 48×48 texels = 1 bloc avant le bloc central (exactement aligné !)
        val originTexelX = (centerBlockX - 1) * 16
        val originTexelZ = (centerBlockZ - 1) * 16

        val pattern = getOrBuildPortalPattern(centerBlockX, surfaceBlockY, centerBlockZ, isCeiling)

        // Pré-calcul de présence de surface sur les 3×3 blocs autour du centre (évite d'appeler getBlockState par texel !)
        val blockHasSurface = BooleanArray(9) { true }
        if (conformToSurface) {
            for (obz in 0 until 3) {
                for (obx in 0 until 3) {
                    val bPos = net.minecraft.core.BlockPos(centerBlockX - 1 + obx, surfaceBlockY, centerBlockZ - 1 + obz)
                    val openPos = if (isCeiling) bPos.below() else bPos.above()
                    val solidShape = level.getBlockState(bPos).getCollisionShape(level, bPos)
                    val openShape = level.getBlockState(openPos).getCollisionShape(level, openPos)
                    blockHasSurface[obz * 3 + obx] = !solidShape.isEmpty && openShape.isEmpty
                }
            }
            // Toujours autoriser le bloc central même sur dalles/formes particulières
            blockHasSurface[4] = true
        }

        // Palettes : Émeraude-Turquoise (Valide) vs Cramoisi (Invalide)
        val shadowR = if (isValid) 4 else 28
        val shadowG = if (isValid) 14 else 4
        val shadowB = if (isValid) 16 else 8

        val dimR = if (isValid) 12 else 155
        val dimG = if (isValid) 115 else 24
        val dimB = if (isValid) 104 else 36

        val brightR = if (isValid) 42 else 255
        val brightG = if (isValid) 242 else if (errorPulse) 45 else 72
        val brightB = if (isValid) 196 else if (errorPulse) 55 else 88

        val coreR = if (isValid) 216 else 255
        val coreG = if (isValid) 255 else 190
        val coreB = if (isValid) 246 else 196

        val dirSign = if (isCeiling) -1.0f else 1.0f
        val yShadow = (surfaceY - camY).toFloat() + dirSign * 0.007f
        val yLines = (surfaceY - camY).toFloat() + dirSign * 0.011f
        val yCore = (surfaceY - camY).toFloat() + dirSign * 0.015f

        val maxOpenRadius = 4.0f + openProgress * 22.0f
        val pulseHead = (timeSec * 14.0f) % (pattern.maxCircuitDist + 10.0f)
        val rotAngle = timeSec * (if (isCeiling) 2.2f else -1.8f)

        // Passe A : Rendu de la grille procédurale 48×48 (ombre + grille + circuits 90° animés)
        for (gz in 0 until PORTAL_GRID_SIZE) {
            val blockRow = gz / 16
            for (gx in 0 until PORTAL_GRID_SIZE) {
                val blockCol = gx / 16
                if (!blockHasSurface[blockRow * 3 + blockCol]) continue

                val cellIdx = gz * PORTAL_GRID_SIZE + gx
                val cellFlag = pattern.flags[cellIdx]

                val dx = gx - PORTAL_HALF + 0.5f
                val dz = gz - PORTAL_HALF + 0.5f
                val dist = sqrt(dx * dx + dz * dz)

                // Déploiement pixel-par-pixel à l'ouverture
                if (dist > maxOpenRadius) continue

                val isCenterZone = dist <= 8.2f
                if (cellFlag == 0 && !isCenterZone) continue

                val x0 = ((originTexelX + gx) * TEXEL - camX).toFloat()
                val z0 = ((originTexelZ + gz) * TEXEL - camZ).toFloat()
                val x1 = x0 + TEXEL
                val z1 = z0 + TEXEL

                // 1. Couche d'ombre abyssale crénelée (Image 1)
                if ((cellFlag and CELL_SHADOW) != 0 || isCenterZone) {
                    val shadowAlpha = (205.0f * fadeAlpha).toInt().coerceIn(0, 255)
                    drawFlatQuad(pose, consumer, x0, yShadow, z0, x1, z1, shadowR, shadowG, shadowB, shadowAlpha)
                } else if ((cellFlag and CELL_SHADOW_EDGE) != 0) {
                    val edgeAlpha = (140.0f * fadeAlpha).toInt().coerceIn(0, 255)
                    drawFlatQuad(pose, consumer, x0, yShadow, z0, x1, z1, shadowR, shadowG, shadowB, edgeAlpha)
                }

                // 2. Couche grille fine alignée sur les blocs & croisements "+" (Image 2)
                if (!isCenterZone && (cellFlag and (CELL_GRID or CELL_GRID_NODE)) != 0) {
                    val radialFade = (1.0f - (dist - 7.5f) / 14.0f).coerceIn(0.15f, 1.0f)
                    if ((cellFlag and CELL_GRID_NODE) != 0) {
                        val a = (190.0f * radialFade * fadeAlpha).toInt().coerceIn(0, 255)
                        drawFlatQuad(pose, consumer, x0, yLines, z0, x1, z1, brightR, brightG, brightB, a)
                    } else {
                        val a = (95.0f * radialFade * fadeAlpha).toInt().coerceIn(0, 255)
                        drawFlatQuad(pose, consumer, x0, yLines, z0, x1, z1, dimR, dimG, dimB, a)
                    }
                }

                // 3. Couche circuits orthogonaux 90° animés par onde d'énergie (Image 1)
                if (!isCenterZone && (cellFlag and (CELL_CIRCUIT_CORE or CELL_CIRCUIT_GLOW)) != 0) {
                    val cDist = pattern.circuitDist[cellIdx].toInt()
                    val waveDelta = kotlin.math.abs(cDist - pulseHead)
                    val waveBoost = (1.0f - waveDelta / 3.8f).coerceIn(0.0f, 1.0f)

                    if ((cellFlag and CELL_CIRCUIT_CORE) != 0) {
                        val isNode = (cellFlag and CELL_CIRCUIT_NODE) != 0
                        if (waveBoost > 0.45f || (isNode && waveBoost > 0.15f)) {
                            val a = ((210.0f + 45.0f * waveBoost) * fadeAlpha).toInt().coerceIn(0, 255)
                            drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, coreR, coreG, coreB, a)
                        } else {
                            val a = ((175.0f + 55.0f * waveBoost) * fadeAlpha).toInt().coerceIn(0, 255)
                            drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, brightR, brightG, brightB, a)
                        }
                    } else if ((cellFlag and CELL_CIRCUIT_GLOW) != 0) {
                        val a = ((75.0f + 55.0f * waveBoost) * fadeAlpha).toInt().coerceIn(0, 255)
                        drawFlatQuad(pose, consumer, x0, yLines, z0, x1, z1, dimR, dimG, dimB, a)
                    }
                }

                // 4. Emblème central pixel-art (spirale Zonai animée + anneaux concentriques ou croix X rouge)
                if (isCenterZone) {
                    if (isValid) {
                        val angle = kotlin.math.atan2(dz, dx)
                        val isOuterRing = dist in 6.3f..7.6f
                        val isInnerRing = dist in 3.9f..4.75f
                        // 3 bras en spirale pixel-art (Image 2) qui tournent pixel par pixel !
                        val spiralPhase = sin((angle * 3.0f + dist * 0.72f - rotAngle).toDouble()).toFloat()
                        val isSpiralArm = dist in 1.3f..6.4f && spiralPhase > 0.62f
                        val isVoidCore = dist < 1.8f

                        when {
                            isOuterRing -> {
                                val rimPulse = 0.80f + 0.20f * sin((angle * 2.0f + timeSec * 4.0f).toDouble()).toFloat()
                                val isHighlight = rimPulse > 0.90f
                                val a = (235.0f * fadeAlpha).toInt().coerceIn(0, 255)
                                if (isHighlight) {
                                    drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, coreR, coreG, coreB, a)
                                } else {
                                    drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, brightR, brightG, brightB, a)
                                }
                            }
                            isSpiralArm -> {
                                val a = (240.0f * fadeAlpha).toInt().coerceIn(0, 255)
                                if (spiralPhase > 0.84f) {
                                    drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, coreR, coreG, coreB, a)
                                } else {
                                    drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, brightR, brightG, brightB, a)
                                }
                            }
                            isInnerRing -> {
                                val a = (145.0f * fadeAlpha).toInt().coerceIn(0, 255)
                                drawFlatQuad(pose, consumer, x0, yLines, z0, x1, z1, dimR, dimG, dimB, a)
                            }
                            isVoidCore -> {
                                // Cœur noir profond (rappel du centre abyssal de l'image 1)
                                val a = (235.0f * fadeAlpha).toInt().coerceIn(0, 255)
                                drawFlatQuad(pose, consumer, x0, yLines, z0, x1, z1, 2, 6, 8, a)
                            }
                        }
                    } else {
                        // État invalide : anneau rouge pixel-art + croix X nette
                        val isRing = dist in 6.2f..7.5f
                        val isCross = dist <= 5.2f && (kotlin.math.abs(kotlin.math.abs(dx) - kotlin.math.abs(dz)) <= 0.85f)
                        val pulse = if (errorPulse) 1.0f else (0.76f + 0.24f * sin(timeSec * 6.5f))
                        if (isRing || isCross) {
                            val a = (235.0f * pulse * fadeAlpha).toInt().coerceIn(0, 255)
                            drawFlatQuad(pose, consumer, x0, yCore, z0, x1, z1, brightR, brightG, brightB, a)
                        }
                    }
                }
            }
        }
    }

    /**
     * Petits cubes pixel-art (1/16 de bloc exact !) qui gouttent depuis le portail au plafond
     * ou s'élèvent doucement autour du portail de sortie au sol (comme en bas à droite de l'image 2 !).
     */
    private fun renderPixelDrippingMotes(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        worldCx: Double,
        surfaceY: Double,
        worldCz: Double,
        playerY: Double,
        camX: Double,
        camY: Double,
        camZ: Double,
        isCeiling: Boolean,
        timeSec: Float,
        intensity: Float
    ) {
        if (intensity <= 0.05f) return

        val baseX = kotlin.math.floor(worldCx).toInt()
        val baseZ = kotlin.math.floor(worldCz).toInt()
        val seed = (baseX * 374761393) xor (baseZ * 668265263)
        val count = if (isCeiling) 22 else 14
        val travelSpan = if (isCeiling) {
            (surfaceY - playerY).toFloat().coerceIn(1.6f, 6.5f)
        } else {
            1.6f
        }

        val halfPix = TEXEL * 0.5f // Exactement 1 pixel Minecraft (1/16 bloc) !
        val pal = EMERALD_PIXEL_PALETTE

        for (i in 0 until count) {
            val h1 = hashFloat(seed + i * 29 + 3)
            val h2 = hashFloat(seed + i * 29 + 11)
            val h3 = hashFloat(seed + i * 29 + 19)

            // Position X/Z quantifiée sur la grille de texels 1/16 de bloc !
            val texOffX = ((h1 - 0.5f) * 26.0f).toInt()
            val texOffZ = ((h2 - 0.5f) * 26.0f).toInt()
            val wx = (baseX * 16 + 8 + texOffX + 0.5) * TEXEL
            val wz = (baseZ * 16 + 8 + texOffZ + 0.5) * TEXEL

            val speed = 0.65f + h3 * 1.15f
            val cycle = ((timeSec * speed + h1 * 4.0f) % 1.0f)
            val distY = cycle * travelSpan

            val rawY = if (isCeiling) (surfaceY - 0.08 - distY) else (surfaceY + 0.08 + distY)
            // Quantifie aussi l'altitude au pas de 1/16 bloc pour un mouvement pixel-art super propre
            val wy = kotlin.math.floor(rawY * 16.0) / 16.0

            val fade = sin(cycle * PI).toFloat() * intensity
            val alpha = (fade * 235.0f).toInt().coerceIn(0, 255)
            if (alpha <= 10) continue

            val vx = (wx - camX).toFloat()
            val vy = (wy - camY).toFloat()
            val vz = (wz - camZ).toFloat()

            if (h3 > 0.55f) {
                emitVoxelCube(pose, consumer, vx, vy, vz, halfPix, pal.coreR, pal.coreG, pal.coreB, alpha)
            } else {
                emitVoxelCube(pose, consumer, vx, vy, vz, halfPix, pal.brightR, pal.brightG, pal.brightB, alpha)
            }
        }
    }

    private fun drawFlatQuad(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        minX: Float, y: Float, minZ: Float,
        maxX: Float, maxZ: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        consumer.addVertex(pose, minX, y, minZ).setColor(r, g, b, a)
        consumer.addVertex(pose, minX, y, maxZ).setColor(r, g, b, a)
        consumer.addVertex(pose, maxX, y, maxZ).setColor(r, g, b, a)
        consumer.addVertex(pose, maxX, y, minZ).setColor(r, g, b, a)
    }
}
