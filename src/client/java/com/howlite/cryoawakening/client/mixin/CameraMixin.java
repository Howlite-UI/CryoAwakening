package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.infiltration.InfiltrationPhase;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gère les animations de caméra fluides de la Perle d'Infiltration (style Zelda TOTK) :
 * - AIMING : léger abaissement caméra + zoom FOV doux vers le plafond.
 * - RISING_TO_CEILING : recul cinématique en 3ème personne pour voir le joueur s'envoler dans le plafond.
 * - SWIMMING_THROUGH / RETURNING : 1ère personne immersive dans le tunnel vortex + élargissement FOV.
 * - PEEKING : caméra 360° libre juste au-dessus du sol autour de la tête qui émerge.
 * - EMERGING : retour progressif fluide vers la caméra normale du joueur.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity entity;

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void move(float forwards, float up, float left);

    @Shadow
    private float getMaxZoom(float desiredDistance) {
        return desiredDistance;
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void cryoawakening$applyInfiltrationCamera(float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (this.entity == null || mc.player == null || this.entity != mc.player) return;

        InfiltrationPhase phase = InfiltrationClientHandler.INSTANCE.getPhase();
        float aimAnim = InfiltrationClientHandler.INSTANCE.getAimAnim(partialTick);
        float detach = InfiltrationClientHandler.INSTANCE.getCameraDetach(partialTick);

        if (phase == InfiltrationPhase.IDLE && aimAnim <= 0.005f && detach <= 0.005f) {
            return;
        }

        double px = Mth.lerp(partialTick, this.entity.xOld, this.entity.getX());
        double py = Mth.lerp(partialTick, this.entity.yOld, this.entity.getY());
        double pz = Mth.lerp(partialTick, this.entity.zOld, this.entity.getZ());

        switch (phase) {
            case AIMING -> {
                // Légère baisse de caméra en 1ère personne pour accentuer le regard vers le plafond
                if (!this.detached && aimAnim > 0.005f) {
                    this.setPosition(this.position.x, this.position.y - 0.16 * aimAnim, this.position.z);
                }
            }

            case RISING_TO_CEILING -> {
                // Recul cinématique en 3ème personne pour voir son personnage s'envoler dans le plafond !
                this.detached = true;
                this.setPosition(px, py + 1.15, pz);
                float desiredPitch = Mth.lerp(detach, this.xRot, -24.0f);
                this.setRotation(this.yRot, desiredPitch);
                float dist = this.getMaxZoom(3.5f * detach);
                this.move(-dist, -0.32f * detach, 0.0f);
            }

            case SWIMMING_THROUGH, RETURNING -> {
                // Vue subjective plein écran dédiée au vortex
                this.detached = false;
                this.setPosition(px, py + this.entity.getEyeHeight(), pz);
            }

            case PEEKING -> {
                // Caméra 360° juste au-dessus du sol autour de la tête qui dépasse (comme TOTK)
                this.detached = true;
                double floorY = InfiltrationClientHandler.INSTANCE.getExitFloorY();
                double tx = InfiltrationClientHandler.INSTANCE.getTargetX();
                double tz = InfiltrationClientHandler.INSTANCE.getTargetZ();

                this.setPosition(tx, floorY + 0.52, tz);
                float safePitch = Mth.clamp(this.xRot, -22.0f, 76.0f);
                this.setRotation(this.yRot, safePitch);
                float dist = this.getMaxZoom(2.4f * detach);
                this.move(-dist, 0.22f * detach, 0.0f);

                // Garantit que la caméra ne plonge jamais sous le sol pendant le coup d'œil
                if (this.position.y < floorY + 0.25) {
                    this.setPosition(this.position.x, floorY + 0.25, this.position.z);
                }
            }

            case EMERGING, IDLE -> {
                // Transition douce vers la caméra normale après émergence
                if (detach > 0.01f) {
                    this.detached = true;
                    this.setPosition(px, py + this.entity.getEyeHeight(), pz);
                    float dist = this.getMaxZoom(2.15f * detach);
                    this.move(-dist, 0.12f * detach, 0.0f);
                } else if (aimAnim > 0.005f && !this.detached) {
                    this.setPosition(this.position.x, this.position.y - 0.16 * aimAnim, this.position.z);
                }
            }
        }
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void cryoawakening$modifyInfiltrationFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        float offset = InfiltrationClientHandler.INSTANCE.getFovOffset(partialTick);
        if (Math.abs(offset) > 0.01f) {
            float adjusted = Mth.clamp(cir.getReturnValue() + offset, 30.0f, 130.0f);
            cir.setReturnValue(adjusted);
        }
    }
}
