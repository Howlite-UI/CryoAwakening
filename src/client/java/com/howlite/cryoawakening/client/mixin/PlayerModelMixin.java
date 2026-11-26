package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.entity.GawkerEntity;
import com.howlite.cryoawakening.infiltration.InfiltrationPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void cryoawakening$customPlayerPoses(AvatarRenderState state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PlayerModel self = (PlayerModel) (Object) this;

        if (mc.player != null && state.id == mc.player.getId()) {
            InfiltrationPhase phase = InfiltrationClientHandler.INSTANCE.getPhase();
            float age = state.ageInTicks;

            // 1. Nage verticale dans le vortex (vers le HAUT en SWIMMING_THROUGH,
            //    et vers le BAS en RETURNING grâce au flip 180° dans AvatarRendererMixin !)
            if (phase == InfiltrationPhase.SWIMMING_THROUGH || phase == InfiltrationPhase.RETURNING) {
                float armCycle = age * 0.28f;
                float strokeReach = (float) Math.sin(armCycle);
                float strokeSpread = 0.5f + 0.5f * (float) Math.cos(armCycle);

                // Bras tendus dans le sens de la nage (au-dessus de la tête) avec brasse fluide
                self.rightArm.setRotation(
                    (float) (-Math.PI + 0.12f + strokeReach * 0.22f),
                    -0.18f * strokeSpread,
                    -0.08f - strokeSpread * 0.42f
                );
                self.leftArm.setRotation(
                    (float) (-Math.PI + 0.12f + strokeReach * 0.22f),
                    0.18f * strokeSpread,
                    0.08f + strokeSpread * 0.42f
                );

                // Battement de jambes fluide en nage verticale
                float legKick = (float) Math.sin(age * 0.40f);
                self.rightLeg.setRotation(0.14f + legKick * 0.52f, 0.0f, 0.07f);
                self.leftLeg.setRotation(0.14f - legKick * 0.52f, 0.0f, -0.07f);

                // Tête levée dans le sens de la nage (vers le haut en montée, vers le bas en redescente)
                self.head.xRot = -0.50f + (float) Math.sin(age * 0.18f) * 0.08f;
                self.head.yRot = (float) Math.sin(age * 0.12f) * 0.10f;

                self.body.zRot = (float) Math.sin(age * 0.20f) * 0.045f;
                syncOuterLayers(self);
                return;
            }

            // 2. Phase PEEKING : perso à moitié sorti du sol, bras calmement abaissés dans le portail,
            //    la tête suit naturellement le regard du joueur !
            if (phase == InfiltrationPhase.PEEKING) {
                float breath = (float) Math.sin(age * 0.10f) * 0.04f;
                self.rightArm.setRotation(-0.15f + breath, 0.0f, 0.14f);
                self.leftArm.setRotation(-0.15f - breath, 0.0f, -0.14f);
                self.rightLeg.setRotation(0.0f, 0.0f, 0.03f);
                self.leftLeg.setRotation(0.0f, 0.0f, -0.03f);
                self.body.zRot = 0.0f;
                syncOuterLayers(self);
                return;
            }

            // 3. Phases RISING_TO_CEILING / EMERGING : élan tendu vers le haut
            if (phase == InfiltrationPhase.RISING_TO_CEILING || phase == InfiltrationPhase.EMERGING) {
                float kick = (float) Math.sin(age * 0.5f) * 0.22f;
                self.rightArm.setRotation((float) -Math.PI, 0.0f, -0.14f);
                self.leftArm.setRotation((float) -Math.PI, 0.0f, 0.14f);
                self.rightLeg.setRotation(0.20f + kick, 0.0f, 0.06f);
                self.leftLeg.setRotation(-0.14f - kick, 0.0f, -0.06f);
                syncOuterLayers(self);
                return;
            }
        }

        // Posture de portage du Gawker au-dessus de la tête
        boolean isCarrying = mc.level.getEntitiesOfClass(
            GawkerEntity.class,
            new AABB(state.x - 2.0, state.y - 1.0, state.z - 2.0, state.x + 2.0, state.y + 3.0, state.z + 2.0),
            g -> g.isCarried() && g.getCarrierId() == state.id
        ).stream().findFirst().isPresent();

        if (isCarrying) {
            self.rightArm.setRotation((float) (-Math.PI + 0.35), -0.15f, 0.25f);
            self.leftArm.setRotation((float) (-Math.PI + 0.35), 0.15f, -0.25f);

            self.rightSleeve.setRotation(self.rightArm.xRot, self.rightArm.yRot, self.rightArm.zRot);
            self.leftSleeve.setRotation(self.leftArm.xRot, self.leftArm.yRot, self.leftArm.zRot);
        }
    }

    private static void syncOuterLayers(PlayerModel self) {
        self.rightSleeve.setRotation(self.rightArm.xRot, self.rightArm.yRot, self.rightArm.zRot);
        self.leftSleeve.setRotation(self.leftArm.xRot, self.leftArm.yRot, self.leftArm.zRot);
        self.rightPants.setRotation(self.rightLeg.xRot, self.rightLeg.yRot, self.rightLeg.zRot);
        self.leftPants.setRotation(self.leftLeg.xRot, self.leftLeg.yRot, self.leftLeg.zRot);
        self.jacket.setRotation(self.body.xRot, self.body.yRot, self.body.zRot);
        self.hat.setRotation(self.head.xRot, self.head.yRot, self.head.zRot);
    }
}
