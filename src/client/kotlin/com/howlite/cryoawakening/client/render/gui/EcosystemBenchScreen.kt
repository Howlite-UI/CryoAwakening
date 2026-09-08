package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.client.render.cave.CaveDioramaRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ARGB
import kotlin.math.*

/**
 * EcosystemBenchScreen (Diorama Isométrique de Grotte Complète)
 *
 * Interface graphique plein écran affichant la totalité de la caverne souterraine
 * (piliers géants, lacs de glace, voûte, terrasses rocheuses) avec vraies textures
 * Minecraft et un tri de profondeur ascendant sans artefact de superposition.
 */
class EcosystemBenchScreen(val benchPos: BlockPos) : Screen(
    Component.translatable("gui.cryo-awakening.ecosystem_bench.title")
) {

    // --- Caméra Isométrique (Verrouillée) ---
    private val yaw: Float = 45.0f           // Azimuth isométrique verrouillé (45°)
    private val pitch: Float = 32.0f         // Inclinaison isométrique verrouillée (32°)
    private var zoom: Float = 2.2f           // Zoom adapté pour embrasser les 210m de la caverne entière
    private var panX: Float = 0.0f           // Déplacement horizontal (Pan)
    private var panY: Float = 0.0f           // Déplacement vertical (Pan)

    // --- Paramètres de Scan & Coupe ---
    private var scanRadius: Int = -1         // -1 = MAX / BIOME ENTIER (100% de la cathédrale)
    private var sliceY: Int = 35             // Coupe de hauteur Y

    // --- États d'Interaction Souris ---
    private var isDraggingPan: Boolean = false
    private var isDraggingSlider: Boolean = false
    private var lastMouseX: Double = 0.0
    private var lastMouseY: Double = 0.0

    // --- Données du Maillage & Télémétrie ---
    private var dioramaMesh: CaveDioramaRenderer.DioramaMesh? = null
    private var animTicks: Float = 0.0f

    // --- Constantes Graphiques ---
    companion object {
        const val BG_STUDIO_CENTER: Int = 0xFF121B2A.toInt()         // Bleu ardoise studio central
        const val BG_STUDIO_EDGE: Int = 0xFF070B12.toInt()           // Bordure studio très sombre
        const val GRID_COLOR: Int = 0x1438BDF8.toInt()               // Grille cyan très subtile
        const val FRAME_BORDER: Int = 0xFF1E3A5F.toInt()             // Bordure technique bleu acier
        const val FRAME_ACCENT: Int = 0xFF38BDF8.toInt()             // Accent cyan vif
        const val PANEL_BG: Int = 0xE60A1322.toInt()                 // Fond panneaux glassmorphic
        const val TEXT_PRIMARY: Int = 0xFFE0F2FE.toInt()             // Blanc bleuté
        const val TEXT_MUTED: Int = 0xFF64748B.toInt()               // Gris ardoise atténué
        const val TEXT_ACCENT: Int = 0xFF38BDF8.toInt()              // Cyan
    }

    override fun init() {
        super.init()
        refreshDiorama()
    }

    fun refreshDiorama() {
        val level = minecraft.level ?: return
        val mesh = CaveDioramaRenderer.buildMesh(level, benchPos, scanRadius = scanRadius)
        dioramaMesh = mesh
        sliceY = mesh.maxY
    }

    fun rescan() {
        refreshDiorama()
        minecraft.player?.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f)
    }

    override fun tick() {
        super.tick()
        animTicks += 0.05f

        // Sécurité : fermeture automatique si le joueur s'éloigne à plus de 10 blocs
        val player = minecraft.player
        if (player != null && player.distanceToSqr(benchPos.x + 0.5, benchPos.y + 0.5, benchPos.z + 0.5) > 100.0) {
            onClose()
        }
    }

    override fun isPauseScreen(): Boolean = false

    // =========================================================================
    // RENDU PRINCIPAL
    // =========================================================================

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val w = width
        val h = height

        // 1. Fond studio atmosphérique (dégradé sombre + grille millimétrée)
        renderStudioAtmosphere(graphics, w, h)

        // 2. Zone de rendu du diorama isométrique texturé
        val vpX = 90
        val vpY = 36
        val vpW = w - 270
        val vpH = h - 68

        if (vpW > 50 && vpH > 50) {
            graphics.enableScissor(vpX, vpY, vpW, vpH)

            val originX = vpX + vpW / 2 + panX.toInt()
            val originY = vpY + vpH / 2 + panY.toInt()

            val mesh = dioramaMesh
            if (mesh != null) {
                CaveDioramaRenderer.renderDiorama(
                    graphics = graphics,
                    mesh = mesh,
                    centerX = originX,
                    centerY = originY,
                    yaw = yaw,
                    pitch = pitch,
                    zoom = zoom,
                    sliceY = sliceY,
                    cutawayRoof = false
                )
            }

            // Balise lumineuse sur l'Ecosystem Bench au centre
            renderCenterBeacon(graphics, originX, originY)

            graphics.disableScissor()
        }

        // 3. Cadre technique autour de la fenêtre d'observation
        renderViewportFrame(graphics, vpX, vpY, vpW, vpH)

        // 4. Barre de titre technique
        renderHeader(graphics, w)

        // 5. Barre latérale gauche (Slider de coupe Y et presets de caméra)
        renderLeftSidebar(graphics, h, mouseX, mouseY)

        // 6. Barre latérale droite (Télémétrie, rayon de scan et options)
        renderRightSidebar(graphics, w, h, mouseX, mouseY)

        // 7. Barre inférieure (Aide commandes & statut)
        renderFooter(graphics, w, h)

        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    // =========================================================================
    // FOND STUDIO & CADRE
    // =========================================================================

    private fun renderStudioAtmosphere(graphics: GuiGraphicsExtractor, w: Int, h: Int) {
        graphics.fillGradient(0, 0, w, h, BG_STUDIO_CENTER, BG_STUDIO_EDGE)

        val gridSize = 28
        for (x in 0 until w step gridSize) {
            graphics.verticalLine(x, 0, h, GRID_COLOR)
        }
        for (y in 0 until h step gridSize) {
            graphics.horizontalLine(0, w, y, GRID_COLOR)
        }
    }

    private fun renderViewportFrame(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        graphics.outline(x - 1, y - 1, w + 2, h + 2, FRAME_BORDER)

        val len = 14
        graphics.fill(RenderPipelines.GUI, x - 2, y - 2, x + len, y, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x - 2, y - 2, x, y + len, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x + w - len, y - 2, x + w + 2, y, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x + w, y - 2, x + w + 2, y + len, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x - 2, y + h, x + len, y + h + 2, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x - 2, y + h - len, x, y + h + 2, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x + w - len, y + h, x + w + 2, y + h + 2, FRAME_ACCENT)
        graphics.fill(RenderPipelines.GUI, x + w, y + h - len, x + w + 2, y + h + 2, FRAME_ACCENT)
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, w: Int) {
        graphics.fill(RenderPipelines.GUI, 0, 0, w, 30, PANEL_BG)
        graphics.horizontalLine(0, w, 30, FRAME_BORDER)

        val title = "§bECOSYSTEM BENCH §8// §fSUBTERRANEAN REALISTIC SURVEY"
        graphics.text(font, Component.literal(title), 14, 10, -1, false)

        val mesh = dioramaMesh
        val modeLabel = if (mesh?.isFullBiome == true) "§bBIOME ENTIER" else "§7${mesh?.radiusUsed}m"
        val spanInfo = if (mesh != null) "ENVERGURE: ${mesh.maxX - mesh.minX}x${mesh.maxZ - mesh.minZ}m §8($modeLabel§8)" else "SCANNING..."
        val coords = "STATION: X=${benchPos.x}, Y=${benchPos.y}, Z=${benchPos.z} §8| §f$spanInfo §8| §7AZIMUTH: §f45° PITCH: §f32° §8(§bISO LOCK§8)"
        graphics.text(font, Component.literal(coords), 310, 10, TEXT_MUTED, false)

        val pulse = ((sin(animTicks.toDouble() * 3.0) + 1.0) * 0.5).toFloat()
        val pulseColor = ARGB.color((150 + (pulse * 105).toInt()).coerceIn(0, 255), 34, 197, 94)
        val statusX = w - 125
        graphics.fill(RenderPipelines.GUI, statusX, 12, statusX + 6, 18, pulseColor)
        graphics.text(font, Component.literal("LIVE STREAM"), statusX + 10, 11, pulseColor, false)
    }

    private fun renderFooter(graphics: GuiGraphicsExtractor, w: Int, h: Int) {
        graphics.fill(RenderPipelines.GUI, 0, h - 26, w, h, PANEL_BG)
        graphics.horizontalLine(0, w, h - 26, FRAME_BORDER)

        val hints = "§8[§7Clic + Glisser§8] §fDéplacer (Pan) §8| [§7Molette§8] §fZoom (${String.format("%.1f", zoom)}px/bloc) §8| §7Vue: §bISOMÉTRIQUE LOCK §8| §7Scan: §bCathédrale Complète"
        graphics.text(font, Component.literal(hints), 14, h - 18, TEXT_MUTED, false)
    }

    // =========================================================================
    // BARRE GAUCHE : SLIDER Y-CUT & PRESETS
    // =========================================================================

    private fun renderLeftSidebar(graphics: GuiGraphicsExtractor, h: Int, mouseX: Int, mouseY: Int) {
        val panelW = 76
        val panelX = 8
        val panelY = 36
        val panelH = h - 68

        graphics.fill(RenderPipelines.GUI, panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG)
        graphics.outline(panelX, panelY, panelW, panelH, FRAME_BORDER)

        graphics.text(font, Component.literal("§7COUPE Y"), panelX + 8, panelY + 8, TEXT_ACCENT, false)

        val trackX = panelX + 38
        val sliderTop = panelY + 28
        val sliderBottom = panelY + panelH - 85
        val trackH = sliderBottom - sliderTop

        graphics.verticalLine(trackX, sliderTop, sliderBottom, FRAME_BORDER)
        graphics.verticalLine(trackX + 1, sliderTop, sliderBottom, 0x5538BDF8.toInt())

        val mesh = dioramaMesh
        val minY = mesh?.minY ?: -20
        val maxY = mesh?.maxY ?: 35
        val range = (maxY - minY).coerceAtLeast(1)

        for (i in 0..4) {
            val gy = (sliderTop + (i.toFloat() / 4.0f) * trackH).toInt()
            graphics.horizontalLine(trackX - 4, trackX + 6, gy, FRAME_BORDER)
        }

        val ratio = 1.0f - ((sliceY - minY).toFloat() / range.toFloat()).coerceIn(0.0f, 1.0f)
        val thumbY = (sliderTop + ratio * trackH).toInt()

        val isHoverThumb = mouseX in (trackX - 12)..(trackX + 16) && mouseY in (thumbY - 6)..(thumbY + 6)
        val thumbColor = if (isHoverThumb || isDraggingSlider) FRAME_ACCENT else 0xFF0284C7.toInt()

        graphics.fill(RenderPipelines.GUI, trackX - 10, thumbY - 5, trackX + 12, thumbY + 5, thumbColor)
        graphics.outline(trackX - 10, thumbY - 5, 22, 10, -1)

        val cutText = if (sliceY >= 0) "+$sliceY" else "$sliceY"
        graphics.centeredText(font, cutText, trackX + 1, thumbY - 3, -1)

        val worldY = benchPos.y + sliceY
        graphics.text(font, Component.literal("§8Y: §f$worldY"), panelX + 12, sliderBottom + 6, TEXT_PRIMARY, false)

        val btnY = panelY + panelH - 24
        renderButton(graphics, panelX + 6, btnY, 64, 16, "RESET", mouseX, mouseY) {
            panX = 0f
            panY = 0f
            zoom = 2.2f
            sliceY = dioramaMesh?.maxY ?: 35
        }
    }

    // =========================================================================
    // BARRE DROITE : TÉLÉMÉTRIE & OPTIONS
    // =========================================================================

    private fun renderRightSidebar(graphics: GuiGraphicsExtractor, w: Int, h: Int, mouseX: Int, mouseY: Int) {
        val panelW = 168
        val panelX = w - panelW - 8
        val panelY = 36
        val panelH = h - 68

        graphics.fill(RenderPipelines.GUI, panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG)
        graphics.outline(panelX, panelY, panelW, panelH, FRAME_BORDER)

        var textY = panelY + 12

        // 1. Analyse de la Grotte
        graphics.text(font, Component.literal("§b// CAVERNE DÉTECTÉE"), panelX + 8, textY, TEXT_ACCENT, false)
        textY += 15

        val air = dioramaMesh?.airBlockCount ?: 0
        val solid = dioramaMesh?.solidBlockCount ?: 0
        graphics.text(font, Component.literal("§7Volume d'Air: §f%,d m³".format(air)), panelX + 8, textY, TEXT_PRIMARY, false)
        textY += 12
        graphics.text(font, Component.literal("§7Blocs Parois: §f%,d".format(solid)), panelX + 8, textY, TEXT_PRIMARY, false)
        textY += 12
        graphics.text(font, Component.literal("§7Altitude Table: §fY=${benchPos.y}"), panelX + 8, textY, TEXT_PRIMARY, false)
        textY += 18

        graphics.horizontalLine(panelX + 8, panelX + panelW - 8, textY, FRAME_BORDER)
        textY += 12

        // 2. Gisements Détectés
        graphics.text(font, Component.literal("§b// GISEMENTS DÉTECTÉS"), panelX + 8, textY, TEXT_ACCENT, false)
        textY += 15

        val bCount = dioramaMesh?.bismuthCount ?: 0
        val tCount = dioramaMesh?.telluriumCount ?: 0
        val dCount = dioramaMesh?.diamondCount ?: 0
        val oCount = dioramaMesh?.otherOreCount ?: 0

        renderLegendRow(graphics, panelX + 8, textY, 0xFFFF2A85.toInt(), "Bismuth:", "$bCount filons")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFFFF9E0B.toInt(), "Tellurium:", "$tCount filons")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFF00E5FF.toInt(), "Diamants:", "$dCount blocs")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFFE2E8F0.toInt(), "Autres filons:", "$oCount filons")
        textY += 18

        graphics.horizontalLine(panelX + 8, panelX + panelW - 8, textY, FRAME_BORDER)

        // Bouton rééchantillonner
        renderButton(graphics, panelX + 8, panelY + panelH - 26, panelW - 16, 18, "ACTUALISER SCAN", mouseX, mouseY) {
            rescan()
        }
    }

    private fun renderLegendRow(graphics: GuiGraphicsExtractor, x: Int, y: Int, color: Int, label: String, value: String) {
        graphics.fill(RenderPipelines.GUI, x, y + 2, x + 6, y + 8, color)
        graphics.text(font, Component.literal("§7$label §f$value"), x + 10, y, TEXT_PRIMARY, false)
    }

    private fun renderButton(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        text: String,
        mouseX: Int,
        mouseY: Int,
        action: () -> Unit
    ) {
        val isHover = mouseX in x..(x + w) && mouseY in y..(y + h)
        val bg = if (isHover) 0xFF1E3A5F.toInt() else 0xFF0F1E32.toInt()
        val border = if (isHover) FRAME_ACCENT else FRAME_BORDER

        graphics.fill(RenderPipelines.GUI, x, y, x + w, y + h, bg)
        graphics.outline(x, y, w, h, border)
        graphics.centeredText(font, text, x + w / 2, y + (h - 8) / 2, if (isHover) -1 else TEXT_PRIMARY)
    }

    // =========================================================================
    // REPÈRE CENTRAL (ÉTABLI)
    // =========================================================================

    private fun renderCenterBeacon(graphics: GuiGraphicsExtractor, originX: Int, originY: Int) {
        val pulse = (animTicks * 2.0f) % 3.0f
        val r = (pulse * 8.0f).toInt().coerceAtLeast(2)
        val alpha = ((1.0f - (pulse / 3.0f)) * 255).toInt().coerceIn(0, 255)
        val color = ARGB.color(alpha, 245, 158, 11)

        graphics.outline(originX - r, originY - r / 2, r * 2, r, color)
        graphics.fill(RenderPipelines.GUI, originX - 1, originY - 1, originX + 2, originY + 2, 0xFFF59E0B.toInt())
        graphics.text(font, Component.literal("§6ÉTABLI"), originX + 8, originY - 4, -1, true)
    }

    // =========================================================================
    // GESTION DES ÉVÉNEMENTS SOURIS (Minecraft 26.2 MouseButtonEvent)
    // =========================================================================

    override fun mouseClicked(event: MouseButtonEvent, isDouble: Boolean): Boolean {
        val mx = event.x()
        val my = event.y()
        val btn = event.button()

        lastMouseX = mx
        lastMouseY = my

        // 1. Slider Y-Cut (Gauche)
        val trackX = 8 + 38
        val panelH = height - 68
        val sliderTop = 36 + 28
        val sliderBottom = 36 + panelH - 85
        if (mx in (trackX - 18.0)..(trackX + 22.0) && my in (sliderTop.toDouble() - 5.0)..(sliderBottom.toDouble() + 5.0)) {
            isDraggingSlider = true
            updateSlider(my, sliderTop, sliderBottom)
            return true
        }

        // 2. Boutons de la barre gauche (RESET)
        val leftBtnY = 36 + panelH - 24
        if (mx in 14.0..78.0 && my in leftBtnY.toDouble()..(leftBtnY + 16.0)) {
            panX = 0f
            panY = 0f
            zoom = 2.2f
            sliceY = dioramaMesh?.maxY ?: 35
            return true
        }

        // 3. Boutons de la barre droite (ACTUALISER SCAN)
        val panelW = 168
        val rightX = width - panelW - 8
        if (mx in (rightX + 8.0)..(width - 8.0)) {
            val rescanY = 36 + panelH - 26
            if (my in rescanY.toDouble()..(rescanY + 18.0)) {
                rescan()
                return true
            }
        }

        // 4. Zone centrale : Déplacement Panoramique (Pan) avec Clic Gauche ou Droit
        if (btn == 0 || btn == 1 || btn == 2) {
            isDraggingPan = true
            return true
        }

        return super.mouseClicked(event, isDouble)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        isDraggingPan = false
        isDraggingSlider = false
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val my = event.y()

        if (isDraggingSlider) {
            val panelH = height - 68
            val sliderTop = 36 + 28
            val sliderBottom = 36 + panelH - 85
            updateSlider(my, sliderTop, sliderBottom)
            return true
        }

        if (isDraggingPan) {
            panX += deltaX.toFloat()
            panY += deltaY.toFloat()
            return true
        }

        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount != 0.0) {
            val factor = if (verticalAmount > 0) 1.15f else 0.87f
            zoom = (zoom * factor).coerceIn(1.0f, 25.0f)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun updateSlider(my: Double, top: Int, bottom: Int) {
        val mesh = dioramaMesh ?: return
        val rangeH = bottom - top
        val clampedY = my.coerceIn(top.toDouble(), bottom.toDouble())
        val ratio = 1.0 - ((clampedY - top) / rangeH)
        val minY = mesh.minY
        val maxY = mesh.maxY
        sliceY = (minY + ratio * (maxY - minY)).roundToInt()
    }
}
