package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.infiltration.InfiltrationPhase;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gère les animations de caméra fluides de la Perle d'Infiltration :
 * - AIMING : léger abaissement caméra + zoom FOV doux vers le plafond.
 * - RISING_TO_CEILING : recul cinématique en 3ème personne (démarrant directement hors de la tête).
 * - SWIMMING_THROUGH / RETURNING : caméra orbitale libre à 360° autour du joueur qui nage dans le vortex 3D.
 * - PEEKING : caméra 360° libre centrée sur le buste/la tête à moitié sortis du sol.
 * - EMERGING -> IDLE : rapprochement fluide puis bascule nette en 1ère personne AVANT d'entrer dans la tête !
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    private static final float MIN_SAFE_HEAD_DISTANCE = 0.85f;

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
                if (!this.detached && aimAnim > 0.005f) {
                    this.setPosition(this.position.x, this.position.y - 0.16 * aimAnim, this.position.z);
                }
            }

            case RISING_TO_CEILING -> {
                float dist = this.getMaxZoom(3.6f * detach);
                if (dist >= MIN_SAFE_HEAD_DISTANCE) {
                    this.detached = true;
                    this.setPosition(px, py + 1.15, pz);
                    float desiredPitch = Mth.lerp(detach, this.xRot, -22.0f);
                    this.setRotation(this.yRot, desiredPitch);
                    this.move(-dist, -0.32f * detach, 0.0f);
                }
            }

            case SWIMMING_THROUGH, RETURNING -> {
                // Caméra orbitale 360° libre autour du personnage qui nage dans le vortex 3D !
                this.detached = true;
                double bobY = Math.sin((this.entity.tickCount + partialTick) * 0.22) * 0.09;
                this.setPosition(px, py + 0.96 + bobY, pz);

                float orbYaw = InfiltrationClientHandler.INSTANCE.getOrbitYaw(partialTick);
                float orbPitch = InfiltrationClientHandler.INSTANCE.getOrbitPitch(partialTick);
                this.setRotation(orbYaw, orbPitch);

                float orbitRadius = Math.max(MIN_SAFE_HEAD_DISTANCE, 4.35f * detach);
                this.move(-orbitRadius, 0.0f, 0.0f);
            }

            case PEEKING -> {
                // Caméra 360° centrée sur le buste/la tête qui dépasse à moitié du sol
                double floorY = InfiltrationClientHandler.INSTANCE.getExitFloorY();
                double tx = InfiltrationClientHandler.INSTANCE.getTargetX();
                double tz = InfiltrationClientHandler.INSTANCE.getTargetZ();

                this.setPosition(tx, floorY + 0.68, tz);
                float safePitch = Mth.clamp(this.xRot, -18.0f, 76.0f);
                this.setRotation(this.yRot, safePitch);
                float dist = this.getMaxZoom(2.85f * detach);

                if (dist >= MIN_SAFE_HEAD_DISTANCE) {
                    this.detached = true;
                    this.move(-dist, 0.28f * detach, 0.0f);
                    if (this.position.y < floorY + 0.30) {
                        this.setPosition(this.position.x, floorY + 0.30, this.position.z);
                    }
                } else {
                    // Espace trop étroit (1×1) : reste proprement aux yeux en 1ère personne, jamais dans le crâne !
                    this.detached = false;
                    this.setPosition(tx, floorY + 0.62, tz);
                }
            }

            case EMERGING -> {
                // Accompagne le bond de sortie puis bascule proprement en 1ère personne avant d'entrer dans la tête
                float desiredDist = 2.65f * detach;
                float dist = this.getMaxZoom(desiredDist);
                if (dist >= MIN_SAFE_HEAD_DISTANCE) {
                    this.detached = true;
                    this.setPosition(px, py + this.entity.getEyeHeight(), pz);
                    this.move(-dist, 0.14f * detach, 0.0f);
                } else {
                    // Bascule nette en 1ère personne (ou garde F5 si le joueur jouait déjà en 3ème personne)
                    if (mc.options.getCameraType().isFirstPerson()) {
                        this.detached = false;
                        this.setPosition(px, py + this.entity.getEyeHeight(), pz);
                    }
                }
            }

            case IDLE -> {
                if (aimAnim > 0.005f && !this.detached) {
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

    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$ignoreFluidFogInsideVortex(CallbackInfoReturnable<FogType> cir) {
        if (InfiltrationClientHandler.isInsideBlocks()) {
            cir.setReturnValue(FogType.NONE);
        }
    }
}
