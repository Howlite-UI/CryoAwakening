package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.infiltration.InfiltrationPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oriente verticalement le corps complet du joueur dans le vortex :
 * - SWIMMING_THROUGH : strictement vertical tête vers le HAUT avec un très léger balancement fluide.
 * - RETURNING : retourné verticalement tête vers le BAS (180°) pour plonger vers la position d'origine !
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(
        method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
        at = @At("TAIL")
    )
    private void cryoawakening$verticalSwimOrientation(
        AvatarRenderState state,
        PoseStack poseStack,
        float bodyRot,
        float scale,
        CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.id != mc.player.getId()) return;

        InfiltrationPhase phase = InfiltrationClientHandler.INSTANCE.getPhase();
        if (phase == InfiltrationPhase.SWIMMING_THROUGH) {
            float sway = (float) Math.sin(state.ageInTicks * 0.16f) * 3.0f;
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
            poseStack.translate(0.0, -0.9, 0.0);
        } else if (phase == InfiltrationPhase.RETURNING) {
            // Plongeon vertical tête la première vers le BAS !
            float sway = (float) Math.sin(state.ageInTicks * 0.18f) * 3.0f;
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
            poseStack.translate(0.0, -0.9, 0.0);
        }
    }
}
