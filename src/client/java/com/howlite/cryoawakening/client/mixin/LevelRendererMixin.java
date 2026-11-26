package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.infiltration.InfiltrationPhase;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Isole complètement la scène 3D du vortex d'Infiltration pendant SWIMMING_THROUGH / RETURNING,
 * et assure un rendu parfait du personnage (vertical dans le vortex, bien éclairé et debout à
 * moitié sorti du sol en phase PEEKING / EMERGING).
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Inject(method = "render", at = @At("HEAD"))
    private void cryoawakening$prepareVortexBackdrop(
        GraphicsResourceAllocator allocator,
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        CameraRenderState cameraRenderState,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice fogUniform,
        Vector4f clearColor,
        boolean renderSky,
        CallbackInfo ci
    ) {
        if (!InfiltrationClientHandler.isInsideBlocks()) return;

        if (InfiltrationClientHandler.INSTANCE.getPhase() == InfiltrationPhase.RETURNING) {
            // Fond abyssal bleu-saphir (redescente vers l'origine)
            clearColor.set(0.015f, 0.068f, 0.165f, 1.0f);
        } else {
            // Fond abyssal émeraude-sarcelle (identique à l'Infiltration de Zelda TOTK)
            clearColor.set(0.012f, 0.145f, 0.155f, 1.0f);
        }

        // Vide les particules extérieures pour laisser briller uniquement nos lucioles 3D du vortex
        this.levelRenderState.particlesRenderState.reset();
    }

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipSkyInVortex(FrameGraphBuilder builder, CameraRenderState camera, GpuBufferSlice fog, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipCloudsInVortex(FrameGraphBuilder builder, CloudStatus status, Vec3 pos, long gameTime, float partialTick, int cloudColor, float cloudHeight, int range, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipWeatherInVortex(FrameGraphBuilder builder, GpuBufferSlice fog, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "submitBlockEntities", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipBlockEntitiesInVortex(PoseStack poseStack, LevelRenderState renderState, SubmitNodeCollector collector, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipBlockOutlineInVortex(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState renderState, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "submitBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$skipBlockDestroyInVortex(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState renderState, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) ci.cancel();
    }

    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void cryoawakening$configureInfiltratingPlayerRender(PoseStack poseStack, LevelRenderState renderState, SubmitNodeCollector collector, CallbackInfo ci) {
        if (!InfiltrationClientHandler.isActiveSequence()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int localId = mc.player.getId();
        InfiltrationPhase phase = InfiltrationClientHandler.INSTANCE.getPhase();

        // Dans le vortex, ne garder que le joueur local
        if (phase.isInsideBlocks()) {
            renderState.entityRenderStates.removeIf(state ->
                !(state instanceof AvatarRenderState avatar) || avatar.id != localId
            );
        }

        for (EntityRenderState state : renderState.entityRenderStates) {
            if (!(state instanceof AvatarRenderState avatar) || avatar.id != localId) continue;

            // Force strictement la posture verticale (empêche l'aplatissement horizontal de Pose.SWIMMING !)
            avatar.pose = Pose.STANDING;
            avatar.swimAmount = 0.0f;
            avatar.isVisuallySwimming = false;
            avatar.isCrouching = false;
            avatar.isFallFlying = false;
            state.shadowPieces.clear();

            if (phase.isInsideBlocks()) {
                // Pleinement éclairé dans le vortex + corps stable pour l'orbite libre 360°
                state.lightCoords = LightCoordsUtil.pack(15, 15);
                float gentleSpin = (float) Math.sin(avatar.ageInTicks * 0.08f) * 8.0f;
                avatar.bodyRot = InfiltrationClientHandler.INSTANCE.getSwimBodyYaw() + gentleSpin;
                avatar.yRot = 0.0f;
                avatar.xRot = -26.0f;
            } else if (phase == InfiltrationPhase.PEEKING || phase == InfiltrationPhase.EMERGING) {
                // Éclairage de la SURFACE au-dessus du sol (évite que le perso paraisse noir car ses pieds sont dans la pierre)
                if (mc.level != null) {
                    BlockPos surfacePos = BlockPos.containing(
                        InfiltrationClientHandler.INSTANCE.getTargetX(),
                        InfiltrationClientHandler.INSTANCE.getExitFloorY() + 0.65,
                        InfiltrationClientHandler.INSTANCE.getTargetZ()
                    );
                    int blockLight = Math.max(mc.level.getBrightness(LightLayer.BLOCK, surfacePos), 6);
                    int skyLight = mc.level.getBrightness(LightLayer.SKY, surfacePos);
                    state.lightCoords = LightCoordsUtil.pack(blockLight, skyLight);
                } else {
                    state.lightCoords = LightCoordsUtil.pack(15, 15);
                }
            }
        }
    }
}
