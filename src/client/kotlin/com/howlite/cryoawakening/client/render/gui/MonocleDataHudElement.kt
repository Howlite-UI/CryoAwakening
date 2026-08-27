package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.block.BreezeFoundryBlock
import com.howlite.cryoawakening.block.GaleBellowsBlock
import com.howlite.cryoawakening.block.GaleTankBlock
import com.howlite.cryoawakening.block.entity.BreezeFoundryBlockEntity
import com.howlite.cryoawakening.block.entity.CryoVentBlockEntity
import com.howlite.cryoawakening.block.entity.GaleBellowsBlockEntity
import com.howlite.cryoawakening.block.entity.GaleTankBlockEntity
import com.howlite.cryoawakening.energy.IWindHolder
import com.howlite.cryoawakening.item.ModItems
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

/**
 * MonocleDataHudElement
 *
 * Projection en Réalité Augmentée (AR) ancrée dynamiquement sur les machines ciblées dans le monde 3D
 * (Cryo-Vent, Gale Tank) lorsque le joueur porte le Monocle de Bourrasque.
 *
 * - Animation fluide de gauche à droite : déploiement du bras, ouverture de la jauge, puis apparition dactylographique continue du texte
 * - Jauge verticale (info_arm_bar_gauge.png) intégrée dans le boîtier armaturé (info_arm_bar.png)
 * - Ancrage géométrique sur l'arête supérieure droite du bloc
 */
object MonocleDataHudElement : HudElement {

    private val ARM_BAR_TEXTURE: Identifier = CryoAwakening.id("textures/gui/info_arm_bar.png")
    private val GAUGE_TEXTURE: Identifier = CryoAwakening.id("textures/gui/info_arm_bar_gauge.png")

    // Gestion de l'animation de découverte
    private var lastTargetPos: BlockPos? = null
    private var targetStartTime: Long = 0L

    const val TOTAL_WIDTH = 57
    const val TOTAL_HEIGHT = 44
    const val GAUGE_OFFSET_X = 34
    const val GAUGE_OFFSET_Y = 3
    const val GAUGE_WIDTH = 16
    const val GAUGE_HEIGHT = 40
    const val ARM_ORIGIN_Y = 33

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return
        if (client.gui.hud.isHidden) return

        // 1. Vérification de l'équipement du Monocle
        val headStack = player.getItemBySlot(EquipmentSlot.HEAD)
        val isWearingMonocle = headStack.`is`(ModItems.GALE_MONOCLE) ||
                player.mainHandItem.`is`(ModItems.GALE_MONOCLE) ||
                player.offhandItem.`is`(ModItems.GALE_MONOCLE)

        if (!isWearingMonocle) {
            lastTargetPos = null
            return
        }

        // 2. Vérification du bloc ciblé sous le réticule
        val hit = client.hitResult as? BlockHitResult
        if (hit == null || hit.type != HitResult.Type.BLOCK) {
            lastTargetPos = null
            return
        }

        val pos = hit.blockPos
        val state = level.getBlockState(pos)
        val be = level.getBlockEntity(pos)

        var title = ""
        var windAmount = 0
        var windCapacity = 0
        var isProducing = false
        var normalizedTargetPos = pos

        if (be is CryoVentBlockEntity) {
            title = "cryo vent"
            windAmount = be.windStorage.wind
            windCapacity = be.windStorage.capacity
            isProducing = true
        } else if (state.block is BreezeFoundryBlock) {
            title = "breeze foundry"
            val foundryBe = be as? BreezeFoundryBlockEntity
            val storage = foundryBe?.windStorage
            if (storage != null) {
                windAmount = storage.wind
                windCapacity = storage.capacity
            } else {
                windCapacity = 10000
            }
        } else if (state.block is GaleBellowsBlock) {
            title = "gale bellows"
            val bellowsBe = be as? GaleBellowsBlockEntity
            val storage = bellowsBe?.windStorage
            if (storage != null) {
                windAmount = storage.wind
                windCapacity = storage.capacity
            } else {
                windCapacity = 500
            }
        } else if (state.block is GaleTankBlock) {
            title = "gale tank"
            val half = state.getValue(GaleTankBlock.HALF)
            val lowerPos = if (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) pos.below() else pos
            val upperPos = if (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) pos.above() else pos
            normalizedTargetPos = lowerPos

            val tankBe = (level.getBlockEntity(lowerPos) as? GaleTankBlockEntity)
                ?: (level.getBlockEntity(upperPos) as? GaleTankBlockEntity)
            val storage = tankBe?.getWindStorage(null)
            if (storage != null) {
                windAmount = storage.wind
                windCapacity = storage.capacity
            } else {
                windCapacity = 20000
            }
        } else if (be is IWindHolder) {
            title = state.block.name.string.lowercase()
            val storage = be.getWindStorage(hit.direction)
            if (storage != null) {
                windAmount = storage.wind
                windCapacity = storage.capacity
            } else {
                lastTargetPos = null
                return
            }
        } else {
            lastTargetPos = null
            return
        }

        // 3. Gestion de l'animation en chaîne (Bras -> Jauge -> Texte continu)
        val now = System.currentTimeMillis()
        if (lastTargetPos != normalizedTargetPos) {
            lastTargetPos = normalizedTargetPos
            targetStartTime = now
        }

        val elapsedMs = now - targetStartTime

        // Phase 1 : Déploiement du bras et du boîtier (0ms -> 180ms)
        val barProgress = (elapsedMs / 180.0f).coerceIn(0.0f, 1.0f)
        val easedBar = barProgress * barProgress * (3.0f - 2.0f * barProgress)
        val currentW = (TOTAL_WIDTH * easedBar).toInt().coerceIn(1, TOTAL_WIDTH)

        // Phase 2 : Révélation dactylographique du texte en continuité (160ms -> 380ms)
        val textProgress = ((elapsedMs - 160.0f) / 220.0f).coerceIn(0.0f, 1.0f)

        // 4. Projection 3D -> 2D (Monde vers Écran) - Ancrage sur l'angle supérieur droit du bloc
        val camera = client.gameRenderer.mainCamera()
        val camPos = camera.position()
        val camRot = camera.rotation()
        val forward = Vector3f(0.0f, 0.0f, -1.0f).rotate(camRot)

        val screenWidth = graphics.guiWidth()
        val screenHeight = graphics.guiHeight()

        // Hauteur et géométrie d'ancrage de la machine
        val topCorners = if (state.block is GaleBellowsBlock) {
            // Ancrage précis sur le nozzle (buse centrale de 4..12 px en X/Z et 17 px de haut)
            val nozzleMinX = pos.x + 4.0 / 16.0
            val nozzleMaxX = pos.x + 12.0 / 16.0
            val nozzleMinZ = pos.z + 4.0 / 16.0
            val nozzleMaxZ = pos.z + 12.0 / 16.0
            val nozzleTopY = pos.y + 17.0 / 16.0
            listOf(
                Vec3(nozzleMinX, nozzleTopY, nozzleMinZ),
                Vec3(nozzleMaxX, nozzleTopY, nozzleMinZ),
                Vec3(nozzleMinX, nozzleTopY, nozzleMaxZ),
                Vec3(nozzleMaxX, nozzleTopY, nozzleMaxZ)
            )
        } else if (state.block is GaleTankBlock) {
            val half = state.getValue(GaleTankBlock.HALF)
            val upperY = if (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) pos.y + 1 else pos.y
            val topBlockY = upperY + 1.0
            listOf(
                Vec3(pos.x.toDouble(), topBlockY, pos.z.toDouble()),
                Vec3(pos.x + 1.0, topBlockY, pos.z.toDouble()),
                Vec3(pos.x.toDouble(), topBlockY, pos.z + 1.0),
                Vec3(pos.x + 1.0, topBlockY, pos.z + 1.0)
            )
        } else {
            val topBlockY = pos.y + 1.0
            listOf(
                Vec3(pos.x.toDouble(), topBlockY, pos.z.toDouble()),
                Vec3(pos.x + 1.0, topBlockY, pos.z.toDouble()),
                Vec3(pos.x.toDouble(), topBlockY, pos.z + 1.0),
                Vec3(pos.x + 1.0, topBlockY, pos.z + 1.0)
            )
        }

        // Trouver le coin supérieur le plus à droite dans le champ de vision du joueur (angle du bloc)
        var anchorScreenX = -10000
        var anchorScreenY = 0
        var foundCorner = false

        for (corner in topCorners) {
            val rel = corner.subtract(camPos)
            val dotForward = (forward.x * rel.x + forward.y * rel.y + forward.z * rel.z).toFloat()
            if (dotForward > 0.2f) {
                val proj = client.gameRenderer.projectPointToScreen(corner)
                val sx = ((proj.x + 1.0) * 0.5 * screenWidth).toInt()
                val sy = ((1.0 - proj.y) * 0.5 * screenHeight).toInt()
                if (sx > anchorScreenX) {
                    anchorScreenX = sx
                    anchorScreenY = sy
                    foundCorner = true
                }
            }
        }

        if (!foundCorner) return
        if (anchorScreenX !in -100..(screenWidth + 100) || anchorScreenY !in -100..(screenHeight + 100)) return

        graphics.nextStratum()

        val barX = anchorScreenX
        val barY = anchorScreenY - ARM_ORIGIN_Y

        // 5. Remplissage vertical de la jauge (remplit de bas en haut : 40 pixels)
        val fillRatio = if (windCapacity > 0) (windAmount.toFloat() / windCapacity.toFloat()).coerceIn(0.0f, 1.0f) else 0.0f
        val fillHeight = (GAUGE_HEIGHT * fillRatio).toInt().coerceIn(0, GAUGE_HEIGHT)

        val gaugeRevealW = (currentW - GAUGE_OFFSET_X).coerceIn(0, GAUGE_WIDTH)

        // A. Dessin du liquide/vent texturé (le haut de la texture reste toujours le haut de la barre)
        if (gaugeRevealW > 0 && fillHeight > 0) {
            val filledY = barY + GAUGE_OFFSET_Y + (GAUGE_HEIGHT - fillHeight)
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GAUGE_TEXTURE,
                barX + GAUGE_OFFSET_X,
                filledY,
                0.0f,
                0.0f,
                gaugeRevealW,
                fillHeight,
                16,
                40
            )
        }

        // B. Dessin du cadre armaturé et du bras (info_arm_bar.png)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ARM_BAR_TEXTURE,
            barX,
            barY,
            0.0f,
            0.0f,
            currentW,
            TOTAL_HEIGHT,
            64,
            64
        )

        // C. Affichage des textes d'information dans la continuité de l'animation (Left-to-Right Typewriter)
        if (textProgress > 0.0f) {
            val textX = barX + TOTAL_WIDTH + 6
            val textY = barY + 13

            val formattedCurrent = formatCompact(windAmount)
            val formattedCapacity = formatCompact(windCapacity)

            val line1Progress = (textProgress / 0.6f).coerceIn(0.0f, 1.0f)
            val line2Progress = ((textProgress - 0.3f) / 0.7f).coerceIn(0.0f, 1.0f)

            // Ligne 1 : Nom de la machine (ex: "Gale Tank", "Cryo Vent")
            if (line1Progress > 0.0f) {
                val formattedTitle = title.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                val line1 = getFormattedPrefix("§f§l$formattedTitle", line1Progress)
                if (line1.isNotEmpty()) {
                    graphics.text(client.font, Component.literal(line1), textX, textY, -1, true)
                }
            }

            // Ligne 2 : Données compactes et claires (ex: "16k / 20k")
            if (line2Progress > 0.0f) {
                val fullLine2 = if (isProducing) {
                    "§e$formattedCurrent §7/ §f$formattedCapacity §a(+5/t)"
                } else {
                    "§e$formattedCurrent §7/ §f$formattedCapacity"
                }
                val line2 = getFormattedPrefix(fullLine2, line2Progress)
                if (line2.isNotEmpty()) {
                    graphics.text(client.font, Component.literal(line2), textX, textY + 12, -1, true)
                }
            }
        }
    }

    /**
     * Extrait une portion d'un texte contenant des codes de formatage § de manière sécurisée
     * pour produire un effet dactylographique AR fluide sans casser les couleurs.
     */
    private fun getFormattedPrefix(text: String, progress: Float): String {
        if (progress >= 1.0f) return text
        if (progress <= 0.0f) return ""

        var visibleLen = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                i += 2
            } else {
                visibleLen++
                i++
            }
        }

        val targetChars = (visibleLen * progress).toInt()
        if (targetChars <= 0) return ""

        var currentChars = 0
        i = 0
        while (i < text.length && currentChars < targetChars) {
            if (text[i] == '§' && i + 1 < text.length) {
                i += 2
            } else {
                currentChars++
                i++
            }
        }
        return text.substring(0, i)
    }

    private fun formatCompact(value: Int): String {
        return if (value >= 1000) {
            val k = value / 1000
            val remainder = (value % 1000) / 100
            if (remainder > 0 && value < 10000) "$k.${remainder}k" else "${k}k"
        } else {
            "$value"
        }
    }
}
