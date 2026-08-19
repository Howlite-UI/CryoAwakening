package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.entity.GawkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(
        method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cryoawakening$renderCarryingGawkerHands(
        float partialTick,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        LocalPlayer localPlayer,
        int combinedLight,
        CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        boolean isCarrying = mc.level.getEntitiesOfClass(
            GawkerEntity.class,
            localPlayer.getBoundingBox().inflate(3.0),
            g -> g.isCarried() && g.getCarrierId() == localPlayer.getId()
        ).stream().findFirst().isPresent();

        if (isCarrying) {
            ci.cancel();

            AvatarRenderer avatarRenderer = mc.getEntityRenderDispatcher().getPlayerRenderer(localPlayer);
            Identifier skinTexture = localPlayer.getSkin().body().texturePath();
            boolean rightSleeve = localPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            boolean leftSleeve = localPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);

            // Balancement fluide lié aux mouvements de marche
            float walkPos = localPlayer.walkAnimation.position(partialTick);
            float walkSpeed = localPlayer.walkAnimation.speed(partialTick);
            float walkBob = (float) Math.sin(walkPos * 1.4f) * walkSpeed * 0.04f;
            float age = localPlayer.tickCount + partialTick;
            float breath = (float) Math.sin(age * 0.08f) * 0.015f;

            // Bras droit (Right Hand) : orienté vers le haut pour soutenir le Gawker
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(92.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(65.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-35.0f));
            poseStack.translate(0.32f, -0.95f + walkBob + breath, 0.40f);
            avatarRenderer.renderRightHand(poseStack, submitNodeCollector, combinedLight, skinTexture, rightSleeve);
            poseStack.popPose();

            // Bras gauche (Left Hand) : orienté vers le haut symétriquement
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(92.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(65.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(35.0f));
            poseStack.translate(-0.32f, -0.95f + walkBob + breath, 0.40f);
            avatarRenderer.renderLeftHand(poseStack, submitNodeCollector, combinedLight, skinTexture, leftSleeve);
            poseStack.popPose();
        }
    }
}
