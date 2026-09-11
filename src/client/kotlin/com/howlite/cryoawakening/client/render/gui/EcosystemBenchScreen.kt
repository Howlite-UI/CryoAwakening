package com.howlite.cryoawakening.client.render.gui

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.client.render.cave.CaveDioramaRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
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

    // --- Rendu Croquis / Blueprint & Background ---
    private var sketchFilter: Boolean = true

    // --- Constantes Graphiques ---
    companion object {
        val DIAGRAM_TEXTURE: Identifier = CryoAwakening.id("textures/gui/ecosystem_bench_diagram.png")
        const val BG_STUDIO_CENTER: Int = 0xFF121B2A.toInt()         // Bleu ardoise studio central
        const val BG_STUDIO_EDGE: Int = 0xFF070B12.toInt()           // Bordure studio très sombre
        const val GRID_COLOR: Int = 0x1438BDF8.toInt()               // Grille cyan très subtile
        const val FRAME_BORDER: Int = 0xFF1E3A5F.toInt()             // Bordure technique bleu acier
        const val FRAME_ACCENT: Int = 0xFF38BDF8.toInt()             // Accent cyan vif
        const val PANEL_BG: Int = 0xE60A1322.toInt()                 // Fond panneaux glassmorphic
        const val TEXT_PRIMARY: Int = 0xFFE0F2FE.toInt()             // Blanc bleuté
        const val TEXT_MUTED: Int = 0xFF64748B.toInt()               // Gris ardoise atténué
        const val TEXT_ACCENT: Int = 0xFF38BDF8.toInt()              // Cyan

        // Thème Graphique Planche d'Ingénieur / Papier Millimétré (Mode Croquis)
        const val DIAGRAM_PANEL_BG: Int = 0xF2F6F1E6.toInt()         // Calque parchemin technique 95%
        const val DIAGRAM_BORDER: Int = 0xFF4A433A.toInt()           // Trait d'encre sombre sépia
        const val DIAGRAM_TEXT_MAIN: Int = 0xFF1F1D1A.toInt()        // Encre de Chine sombre
        const val DIAGRAM_TEXT_MUTED: Int = 0xFF6E6254.toInt()       // Sépia atténué
        const val DIAGRAM_TEXT_ACCENT: Int = 0xFF8A3B14.toInt()      // Ocre d'annotation
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

        // 1. Fond d'atelier / Planche d'architecte
        renderStudioAtmosphere(graphics, w, h)

        // 2. Zone de rendu du diorama isométrique
        val vpX = 96
        val vpY = 36
        val vpW = w - 276
        val vpH = h - 68

        if (vpW > 50 && vpH > 50) {
            graphics.enableScissor(vpX, vpY, vpX + vpW, vpY + vpH)

            // Fond papier diagramme technique sous le diorama
            if (sketchFilter) {
                graphics.blit(
                    DIAGRAM_TEXTURE,
                    vpX, vpY, vpX + vpW, vpY + vpH,
                    0.0f, 1.0f,
                    0.0f, 1.0f
                )
            }

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
                    cutawayRoof = false,
                    vpX = vpX,
                    vpY = vpY,
                    vpW = vpW,
                    vpH = vpH,
                    sketchMode = sketchFilter
                )
            }

            if (sketchFilter) {
                // Boussole d'orientation technique (dans les coins de la planche)
                renderCompass(graphics, vpX, vpY, vpW, vpH)
            } else {
                // Balise lumineuse discrète sur l'Ecosystem Bench au centre
                renderCenterBeacon(graphics, originX, originY)
            }

            graphics.disableScissor()
        }

        // 3. Cadre technique autour de la fenêtre d'observation
        renderViewportFrame(graphics, vpX, vpY, vpW, vpH)

        // 4. Barre de titre technique
        renderHeader(graphics, w)

        // 5. Barre latérale gauche (Légende technique, slider Y et boutons)
        renderLeftSidebar(graphics, h, mouseX, mouseY)

        // 6. Barre latérale droite (Télémétrie géologique & minerais)
        renderRightSidebar(graphics, w, h, mouseX, mouseY)

        // 7. Barre inférieure (Aide commandes & statut)
        renderFooter(graphics, w, h)

        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    // =========================================================================
    // FOND STUDIO & CADRE
    // =========================================================================

    private fun renderStudioAtmosphere(graphics: GuiGraphicsExtractor, w: Int, h: Int) {
        if (sketchFilter) {
            // Fond papier millimétré diagramme d'architecte
            graphics.blit(
                DIAGRAM_TEXTURE,
                0, 0, w, h,
                0.0f, w.toFloat() / 256.0f,
                0.0f, h.toFloat() / 192.0f
            )
            // Voile très léger pour adoucir le contraste
            graphics.fill(RenderPipelines.GUI, 0, 0, w, h, 0x121E293B.toInt())
        } else {
            graphics.fillGradient(0, 0, w, h, BG_STUDIO_CENTER, BG_STUDIO_EDGE)

            val gridSize = 28
            for (x in 0 until w step gridSize) {
                graphics.verticalLine(x, 0, h, GRID_COLOR)
            }
            for (y in 0 until h step gridSize) {
                graphics.horizontalLine(0, w, y, GRID_COLOR)
            }
        }
    }

    private fun renderViewportFrame(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        val border = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val accent = if (sketchFilter) 0xFF2A241F.toInt() else FRAME_ACCENT
        graphics.outline(x - 1, y - 1, w + 2, h + 2, border)

        val len = 14
        graphics.fill(RenderPipelines.GUI, x - 2, y - 2, x + len, y, accent)
        graphics.fill(RenderPipelines.GUI, x - 2, y - 2, x, y + len, accent)
        graphics.fill(RenderPipelines.GUI, x + w - len, y - 2, x + w + 2, y, accent)
        graphics.fill(RenderPipelines.GUI, x + w, y - 2, x + w + 2, y + len, accent)
        graphics.fill(RenderPipelines.GUI, x - 2, y + h, x + len, y + h + 2, accent)
        graphics.fill(RenderPipelines.GUI, x - 2, y + h - len, x, y + h + 2, accent)
        graphics.fill(RenderPipelines.GUI, x + w - len, y + h, x + w + 2, y + h + 2, accent)
        graphics.fill(RenderPipelines.GUI, x + w, y + h - len, x + w + 2, y + h + 2, accent)
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, w: Int) {
        val bg = if (sketchFilter) 0xF4F5EFE3.toInt() else PANEL_BG
        val border = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val textMuted = if (sketchFilter) DIAGRAM_TEXT_MUTED else TEXT_MUTED

        graphics.fill(RenderPipelines.GUI, 0, 0, w, 30, bg)
        graphics.horizontalLine(0, w, 30, border)

        val title = if (sketchFilter) {
            "§0ECOSYSTEM BENCH §8// §4RELEVÉ TOPOGRAPHIQUE EXPÉDITION §8(§1DIAGRAMME TECHNIQUE§8)"
        } else {
            "§bECOSYSTEM BENCH §8// §fSUBTERRANEAN REALISTIC SURVEY"
        }
        graphics.text(font, Component.literal(title), 14, 10, -1, false)

        val mesh = dioramaMesh
        val modeLabel = if (mesh?.isFullBiome == true) (if (sketchFilter) "§0BIOME ENTIER" else "§bBIOME ENTIER") else "§7${mesh?.radiusUsed}m"
        val spanInfo = if (mesh != null) "ENVERGURE: ${mesh.maxX - mesh.minX}x${mesh.maxZ - mesh.minZ}m §8($modeLabel§8)" else "SCANNING..."
        val coords = if (sketchFilter) {
            "§0STATION: §8X=${benchPos.x}, Y=${benchPos.y}, Z=${benchPos.z} §8| §0$spanInfo §8| §8AZIMUTH: §045° §8PITCH: §032° §8(§0ISO LOCK§8)"
        } else {
            "STATION: X=${benchPos.x}, Y=${benchPos.y}, Z=${benchPos.z} §8| §f$spanInfo §8| §7AZIMUTH: §f45° PITCH: §f32° §8(§bISO LOCK§8)"
        }
        graphics.text(font, Component.literal(coords), 340, 10, textMuted, false)

        val pulse = ((sin(animTicks.toDouble() * 3.0) + 1.0) * 0.5).toFloat()
        val pulseColor = if (sketchFilter) {
            0xFF1E7E34.toInt()
        } else {
            ARGB.color((150 + (pulse * 105).toInt()).coerceIn(0, 255), 34, 197, 94)
        }
        val statusX = w - 125
        graphics.fill(RenderPipelines.GUI, statusX, 12, statusX + 6, 18, pulseColor)
        val statusText = if (sketchFilter) "§0RELEVÉ EN DIRECT" else "LIVE STREAM"
        graphics.text(font, Component.literal(statusText), statusX + 10, 11, if (sketchFilter) 0xFF1F1D1A.toInt() else pulseColor, false)
    }

    private fun renderFooter(graphics: GuiGraphicsExtractor, w: Int, h: Int) {
        val bg = if (sketchFilter) 0xF4F5EFE3.toInt() else PANEL_BG
        val border = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val textMuted = if (sketchFilter) DIAGRAM_TEXT_MUTED else TEXT_MUTED

        graphics.fill(RenderPipelines.GUI, 0, h - 26, w, h, bg)
        graphics.horizontalLine(0, w, h - 26, border)

        val hints = if (sketchFilter) {
            "§0[§8Clic + Glisser§0] Déplacer §8| [§8Molette§0] Zoom (${String.format("%.1f", zoom)}px/bloc) §8| §0Vue: §8ISOMÉTRIQUE LOCK §8| §0Format: §8EXPEDITION ARCHITECTURAL DIAGRAM"
        } else {
            "§8[§7Clic + Glisser§8] §fDéplacer (Pan) §8| [§7Molette§8] §fZoom (${String.format("%.1f", zoom)}px/bloc) §8| §7Vue: §bISOMÉTRIQUE LOCK §8| §7Scan: §bCathédrale Complète"
        }
        graphics.text(font, Component.literal(hints), 14, h - 18, textMuted, false)
    }

    // =========================================================================
    // BARRE GAUCHE : LÉGENDE TECHNIQUE & COUPE Y
    // =========================================================================

    private fun renderLeftSidebar(graphics: GuiGraphicsExtractor, h: Int, mouseX: Int, mouseY: Int) {
        val panelW = 84
        val panelX = 8
        val panelY = 36
        val panelH = h - 68

        val bg = if (sketchFilter) DIAGRAM_PANEL_BG else PANEL_BG
        val border = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val textMain = if (sketchFilter) DIAGRAM_TEXT_MAIN else TEXT_PRIMARY
        val textAccent = if (sketchFilter) DIAGRAM_TEXT_ACCENT else TEXT_ACCENT

        graphics.fill(RenderPipelines.GUI, panelX, panelY, panelX + panelW, panelY + panelH, bg)
        graphics.outline(panelX, panelY, panelW, panelH, border)

        var curY = panelY + 8
        if (sketchFilter) {
            graphics.text(font, Component.literal("§0// LÉGENDE"), panelX + 7, curY, textMain, false)
            curY += 13

            renderLegendItem(graphics, panelX + 7, curY, 0xFFEDE4D0.toInt(), 0xFF2B2621.toInt(), "Roche/Sol")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFFCCE7F5.toInt(), 0xFF2B4455.toInt(), "Glace/Eau")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFFF9FAFC.toInt(), 0xFF353C45.toInt(), "Neige")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFF3D4046.toInt(), 0xFF141517.toInt(), "Piliers")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFFF59E0B.toInt(), 0xFF451A03.toInt(), "Établi")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFFF43F5E.toInt(), 0xFF4C0519.toInt(), "Gisements")
            curY += 12
            renderLegendItem(graphics, panelX + 7, curY, 0xFFC4B4A4.toInt(), 0xFF2C241E.toInt(), "Bois Lilas")
            curY += 15
            graphics.horizontalLine(panelX + 7, panelX + panelW - 7, curY, 0xFFB8AA98.toInt())
            curY += 6
        }

        graphics.text(font, Component.literal(if (sketchFilter) "§0COUPE Y" else "§7COUPE Y"), panelX + 8, curY, textAccent, false)

        val trackX = panelX + 42
        val sliderTop = getSliderTop(panelY)
        val sliderBottom = getSliderBottom(panelY, panelH)
        val trackH = sliderBottom - sliderTop

        val trackColor = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val trackLineColor = if (sketchFilter) 0xFF8C7D6D.toInt() else 0x5538BDF8.toInt()
        graphics.verticalLine(trackX, sliderTop, sliderBottom, trackColor)
        graphics.verticalLine(trackX + 1, sliderTop, sliderBottom, trackLineColor)

        val mesh = dioramaMesh
        val minY = mesh?.minY ?: -20
        val maxY = mesh?.maxY ?: 35
        val range = (maxY - minY).coerceAtLeast(1)

        for (i in 0..4) {
            val gy = (sliderTop + (i.toFloat() / 4.0f) * trackH).toInt()
            graphics.horizontalLine(trackX - 4, trackX + 6, gy, trackColor)
        }

        val ratio = 1.0f - ((sliceY - minY).toFloat() / range.toFloat()).coerceIn(0.0f, 1.0f)
        val thumbY = (sliderTop + ratio * trackH).toInt()

        val isHoverThumb = mouseX in (trackX - 12)..(trackX + 16) && mouseY in (thumbY - 6)..(thumbY + 6)
        val thumbColor = if (sketchFilter) {
            if (isHoverThumb || isDraggingSlider) 0xFF1F1D1A.toInt() else 0xFF4A433A.toInt()
        } else {
            if (isHoverThumb || isDraggingSlider) FRAME_ACCENT else 0xFF0284C7.toInt()
        }

        graphics.fill(RenderPipelines.GUI, trackX - 10, thumbY - 5, trackX + 12, thumbY + 5, thumbColor)
        graphics.outline(trackX - 10, thumbY - 5, 22, 10, if (sketchFilter) 0xFFF5EFE3.toInt() else -1)

        val cutText = if (sliceY >= 0) "+$sliceY" else "$sliceY"
        graphics.centeredText(font, cutText, trackX + 1, thumbY - 3, -1)

        val worldY = benchPos.y + sliceY
        val yLabel = if (sketchFilter) "§0Y: §8$worldY" else "§8Y: §f$worldY"
        graphics.text(font, Component.literal(yLabel), panelX + 12, sliderBottom + 6, textMain, false)

        val sketchBtnY = panelY + panelH - 44
        val sketchText = if (sketchFilter) "[✓] CROQUIS" else "[ ] RÉALISTE"
        renderButton(graphics, panelX + 6, sketchBtnY, panelW - 12, 16, sketchText, mouseX, mouseY) {
            sketchFilter = !sketchFilter
            minecraft.player?.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.3f)
        }

        val btnY = panelY + panelH - 24
        renderButton(graphics, panelX + 6, btnY, panelW - 12, 16, "RESET", mouseX, mouseY) {
            panX = 0f
            panY = 0f
            zoom = 2.2f
            sliceY = dioramaMesh?.maxY ?: 35
        }
    }

    private fun renderLegendItem(graphics: GuiGraphicsExtractor, x: Int, y: Int, fillColor: Int, borderColor: Int, label: String) {
        graphics.fill(RenderPipelines.GUI, x, y + 1, x + 7, y + 8, fillColor)
        graphics.outline(x, y + 1, 7, 7, borderColor)
        graphics.text(font, Component.literal("§0$label"), x + 11, y, DIAGRAM_TEXT_MAIN, false)
    }

    // =========================================================================
    // BARRE DROITE : TÉLÉMÉTRIE & OPTIONS
    // =========================================================================

    private fun renderRightSidebar(graphics: GuiGraphicsExtractor, w: Int, h: Int, mouseX: Int, mouseY: Int) {
        val panelW = 168
        val panelX = w - panelW - 8
        val panelY = 36
        val panelH = h - 68

        val bg = if (sketchFilter) DIAGRAM_PANEL_BG else PANEL_BG
        val border = if (sketchFilter) DIAGRAM_BORDER else FRAME_BORDER
        val textMain = if (sketchFilter) DIAGRAM_TEXT_MAIN else TEXT_PRIMARY
        val textAccent = if (sketchFilter) DIAGRAM_TEXT_ACCENT else TEXT_ACCENT

        graphics.fill(RenderPipelines.GUI, panelX, panelY, panelX + panelW, panelY + panelH, bg)
        graphics.outline(panelX, panelY, panelW, panelH, border)

        var textY = panelY + 12

        // 1. Analyse de la Grotte
        val h1 = if (sketchFilter) "§0// RELEVÉ CAVERNE" else "§b// CAVERNE DÉTECTÉE"
        graphics.text(font, Component.literal(h1), panelX + 8, textY, textAccent, false)
        textY += 15

        val air = dioramaMesh?.airBlockCount ?: 0
        val solid = dioramaMesh?.solidBlockCount ?: 0
        val tAir = if (sketchFilter) "§0Volume d'Air: §8%,d m³".format(air) else "§7Volume d'Air: §f%,d m³".format(air)
        val tSolid = if (sketchFilter) "§0Blocs Parois: §8%,d".format(solid) else "§7Blocs Parois: §f%,d".format(solid)
        val tAlt = if (sketchFilter) "§0Altitude Table: §8Y=${benchPos.y}" else "§7Altitude Table: §fY=${benchPos.y}"

        graphics.text(font, Component.literal(tAir), panelX + 8, textY, textMain, false)
        textY += 12
        graphics.text(font, Component.literal(tSolid), panelX + 8, textY, textMain, false)
        textY += 12
        graphics.text(font, Component.literal(tAlt), panelX + 8, textY, textMain, false)
        textY += 18

        graphics.horizontalLine(panelX + 8, panelX + panelW - 8, textY, if (sketchFilter) 0xFFB8AA98.toInt() else FRAME_BORDER)
        textY += 12

        // 2. Gisements Détectés
        val h2 = if (sketchFilter) "§0// GISEMENTS DÉTECTÉS" else "§b// GISEMENTS DÉTECTÉS"
        graphics.text(font, Component.literal(h2), panelX + 8, textY, textAccent, false)
        textY += 15

        val bCount = dioramaMesh?.bismuthCount ?: 0
        val tCount = dioramaMesh?.telluriumCount ?: 0
        val dCount = dioramaMesh?.diamondCount ?: 0
        val oCount = dioramaMesh?.otherOreCount ?: 0

        renderLegendRow(graphics, panelX + 8, textY, 0xFFF43F5E.toInt(), "Bismuth:", "$bCount filons")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFFF59E0B.toInt(), "Tellurium:", "$tCount filons")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFF06B6D4.toInt(), "Diamants:", "$dCount blocs")
        textY += 13
        renderLegendRow(graphics, panelX + 8, textY, 0xFF718096.toInt(), "Autres filons:", "$oCount filons")
        textY += 18

        graphics.horizontalLine(panelX + 8, panelX + panelW - 8, textY, if (sketchFilter) 0xFFB8AA98.toInt() else FRAME_BORDER)

        // Bouton rééchantillonner
        renderButton(graphics, panelX + 8, panelY + panelH - 26, panelW - 16, 18, if (sketchFilter) "ACTUALISER RELEVÉ" else "ACTUALISER SCAN", mouseX, mouseY) {
            rescan()
        }
    }

    private fun renderLegendRow(graphics: GuiGraphicsExtractor, x: Int, y: Int, color: Int, label: String, value: String) {
        graphics.fill(RenderPipelines.GUI, x, y + 2, x + 6, y + 8, color)
        val text = if (sketchFilter) "§0$label §8$value" else "§7$label §f$value"
        graphics.text(font, Component.literal(text), x + 10, y, if (sketchFilter) DIAGRAM_TEXT_MAIN else TEXT_PRIMARY, false)
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
        val bg = if (sketchFilter) {
            if (isHover) 0xFFDDD0BA.toInt() else 0xFFE8DECC.toInt()
        } else {
            if (isHover) 0xFF1E3A5F.toInt() else 0xFF0F1E32.toInt()
        }
        val border = if (sketchFilter) 0xFF4A433A.toInt() else if (isHover) FRAME_ACCENT else FRAME_BORDER
        val textColor = if (sketchFilter) 0xFF1F1D1A.toInt() else if (isHover) -1 else TEXT_PRIMARY

        graphics.fill(RenderPipelines.GUI, x, y, x + w, y + h, bg)
        graphics.outline(x, y, w, h, border)
        graphics.centeredText(font, text, x + w / 2, y + (h - 8) / 2, textColor)
    }

    // =========================================================================
    // REPÈRES TECHNIQUES (BOUSSOLE)
    // =========================================================================

    private fun renderCompass(graphics: GuiGraphicsExtractor, vpX: Int, vpY: Int, vpW: Int, vpH: Int) {
        val cx = vpX + vpW - 35
        val cy = vpY + 25

        // Boussole Nord/Sud
        graphics.centeredText(font, "§0N", cx, cy - 14, 0xFF1F1D1A.toInt())
        graphics.centeredText(font, "§8S", cx, cy + 8, 0xFF6B5E51.toInt())
        graphics.centeredText(font, "§8⤹ ⤸", cx, cy - 3, 0xFF4A433A.toInt())

        // Boussole Ouest/Est (en bas à droite)
        val bx = vpX + vpW - 35
        val by = vpY + vpH - 25
        graphics.centeredText(font, "§8W", bx - 12, by - 3, 0xFF6B5E51.toInt())
        graphics.centeredText(font, "§8E", bx + 12, by - 3, 0xFF6B5E51.toInt())
        graphics.fill(RenderPipelines.GUI, bx - 1, by - 1, bx + 2, by + 2, 0xFF4A433A.toInt())
    }

    // =========================================================================
    // REPÈRE CENTRAL (ÉTABLI EN MODE RÉALISTE - SANS TEXTE PAR DESSUS)
    // =========================================================================

    private fun renderCenterBeacon(graphics: GuiGraphicsExtractor, originX: Int, originY: Int) {
        val pulse = (animTicks * 2.0f) % 3.0f
        val r = (pulse * 8.0f).toInt().coerceAtLeast(2)
        val alpha = ((1.0f - (pulse / 3.0f)) * 255).toInt().coerceIn(0, 255)
        val color = ARGB.color(alpha, 245, 158, 11)

        graphics.outline(originX - r, originY - r / 2, r * 2, r, color)
        graphics.fill(RenderPipelines.GUI, originX - 1, originY - 1, originX + 2, originY + 2, 0xFFF59E0B.toInt())
    }

    private fun getSliderTop(panelY: Int): Int =
        panelY + (if (sketchFilter) 130 else 24)

    private fun getSliderBottom(panelY: Int, panelH: Int): Int =
        panelY + panelH - 85

    // =========================================================================
    // GESTION DES ÉVÉNEMENTS SOURIS (Minecraft 26.2 MouseButtonEvent)
    // =========================================================================

    override fun mouseClicked(event: MouseButtonEvent, isDouble: Boolean): Boolean {
        val mx = event.x()
        val my = event.y()
        val btn = event.button()

        lastMouseX = mx
        lastMouseY = my

        val panelH = height - 68
        val panelY = 36

        // 1. Slider Y-Cut (Gauche)
        val leftPanelW = 84
        val trackX = 8 + 42
        val sliderTop = getSliderTop(panelY)
        val sliderBottom = getSliderBottom(panelY, panelH)
        if (mx in (trackX - 18.0)..(trackX + 22.0) && my in (sliderTop.toDouble() - 5.0)..(sliderBottom.toDouble() + 5.0)) {
            isDraggingSlider = true
            updateSlider(my, sliderTop, sliderBottom)
            return true
        }

        // 2. Boutons de la barre gauche (CROQUIS & RESET)
        val sketchBtnY = panelY + panelH - 44
        if (mx in 14.0..86.0 && my in sketchBtnY.toDouble()..(sketchBtnY + 16.0)) {
            sketchFilter = !sketchFilter
            minecraft.player?.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.3f)
            return true
        }

        val leftBtnY = panelY + panelH - 24
        if (mx in 14.0..86.0 && my in leftBtnY.toDouble()..(leftBtnY + 16.0)) {
            panX = 0f
            panY = 0f
            zoom = 2.2f
            sliceY = dioramaMesh?.maxY ?: 35
            return true
        }

        // 3. Boutons de la barre droite (ACTUALISER SCAN)
        val rightPanelW = 168
        val rightX = width - rightPanelW - 8
        if (mx in (rightX + 8.0)..(width - 8.0)) {
            val rescanY = panelY + panelH - 26
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
            val panelY = 36
            val sliderTop = getSliderTop(panelY)
            val sliderBottom = getSliderBottom(panelY, panelH)
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
