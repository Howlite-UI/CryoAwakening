package com.howlite.cryoawakening.client.render

import com.howlite.cryoawakening.block.entity.CryoTombBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CryoTombRenderState : BlockEntityRenderState() {
    var thawProgress: Int = 0
    var entityTypeId: Identifier = Identifier.fromNamespaceAndPath("minecraft", "zombie")
    var light: Int = 15728880
}

/**
 * CryoTombBlockEntityRenderer
 *
 * Affiche dynamiquement le monstre ancien emprisonné dans la Cryo-Tomb.
 * Supporte TOUS les monstres du jeu (Zombie, Creeper, Squelette, etc.) avec adaptation
 * automatique de l'échelle pour éviter que les membres (mains, cornes, etc.) ne dépassent de la glace.
 */
class CryoTombBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<CryoTombBlockEntity, CryoTombRenderState> {

    val entityRenderer = context.entityRenderer
    private val entityCache: MutableMap<Identifier, Entity> = HashMap()
    private val dummyIdCounter = java.util.concurrent.atomic.AtomicInteger(500000)

    override fun createRenderState(): CryoTombRenderState {
        return CryoTombRenderState()
    }

    override fun extractRenderState(
        blockEntity: CryoTombBlockEntity,
        state: CryoTombRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.thawProgress = blockEntity.thawProgress
        state.entityTypeId = blockEntity.entityTypeId

        val level = blockEntity.level
        if (level != null) {
            val skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, blockEntity.blockPos.above())
            val blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockEntity.blockPos.above())
            state.light = (skyLight shl 20) or (blockLight shl 4)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Entity, S : EntityRenderState> renderEntityHelper(
        renderer: EntityRenderer<in T, *>,
        entity: T,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val typedRenderer = renderer as EntityRenderer<T, S>
        val renderState = typedRenderer.createRenderState()
        typedRenderer.extractRenderState(entity, renderState, 0.0f)
        typedRenderer.submit(renderState, poseStack, submitNodeCollector, camera)
    }

    private fun renderEntity(
        entity: Entity,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val renderer = entityRenderer.getRenderer(entity)
        renderEntityHelper<Entity, EntityRenderState>(
            renderer,
            entity,
            poseStack,
            submitNodeCollector,
            camera
        )
    }

    private fun getOrCreateEntity(typeId: Identifier): Entity? {
        val level = Minecraft.getInstance().level ?: return null
        return entityCache.getOrPut(typeId) {
            val opt = BuiltInRegistries.ENTITY_TYPE.get(typeId)
            val entityType = if (opt.isPresent) {
                opt.get().value()
            } else {
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "zombie"))
            }
            val created = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD)
                ?: return null
            created.id = dummyIdCounter.incrementAndGet()
            created
        }
    }

    override fun submit(
        state: CryoTombRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = getOrCreateEntity(state.entityTypeId) ?: return

        poseStack.pushPose()

        // Centrage horizontal et positionnement en bas du double bloc
        poseStack.translate(0.5, 0.08, 0.5)

        // Effet de tremblement dynamique lorsque la glace se fissure
        if (state.thawProgress > 0) {
            val shakeIntensity = (state.thawProgress.toFloat() / 60.0f) * 0.02f
            val time = System.currentTimeMillis() * 0.06
            val shakeX = sin(time).toFloat() * shakeIntensity
            val shakeZ = cos(time * 1.3).toFloat() * shakeIntensity
            poseStack.translate(shakeX.toDouble(), 0.0, shakeZ.toDouble())
        }

        // Calcul automatique de l'échelle pour empêcher tout dépassement (mains, têtes, ailes)
        val entityWidth = max(entity.boundingBox.xsize, entity.boundingBox.zsize).coerceAtLeast(0.4)
        val entityHeight = entity.boundingBox.ysize.coerceAtLeast(0.6)

        // Limites strictes : 0.65 de largeur/profondeur max (mains rentrées), 1.65 de hauteur max
        val scaleHorizontal = 0.65 / entityWidth
        val scaleVertical = 1.65 / entityHeight
        val finalScale = min(scaleHorizontal, min(scaleVertical, 0.75)).toFloat()

        poseStack.scale(finalScale, finalScale, finalScale)

        renderEntity(entity, poseStack, submitNodeCollector, camera)

        poseStack.popPose()
    }
}
