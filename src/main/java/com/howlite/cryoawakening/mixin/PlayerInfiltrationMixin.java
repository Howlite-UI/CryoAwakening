package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.event.InfiltrationServerHandler;
import com.howlite.cryoawakening.infiltration.InfiltrationClientBridge;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Maintient player.noPhysics = true et Pose.STANDING pendant les phases actives d'infiltration :
 * - Empêche Player.tick() de réinitialiser noPhysics.
 * - Empêche Player.updatePlayerPose() de passer le joueur en Pose.SWIMMING (position horizontale
 *   aplatie dans les blocs solides), garantissant une posture verticale dans le vortex et un perso
 *   bien visible à moitié sorti du sol en phase PEEKING !
 * - Empêche de jeter un objet accidentellement pendant la traversée ou le coup d'œil.
 */
@Mixin(Player.class)
public abstract class PlayerInfiltrationMixin {

    @Inject(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        )
    )
    private void cryoawakening$preserveNoPhysicsDuringInfiltration(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        boolean infiltrating = self.level().isClientSide()
            ? InfiltrationClientBridge.isClientPlayerInfiltrating(self)
            : InfiltrationServerHandler.isPlayerInfiltrating(self);

        if (infiltrating) {
            self.noPhysics = true;
            self.fallDistance = 0.0;
            self.setSwimming(false);
            if (self.getPose() != Pose.STANDING) {
                self.setPose(Pose.STANDING);
            }
        }
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$forceStandingPoseDuringInfiltration(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        boolean infiltrating = self.level().isClientSide()
            ? InfiltrationClientBridge.isClientPlayerInfiltrating(self)
            : InfiltrationServerHandler.isPlayerInfiltrating(self);

        if (infiltrating) {
            self.setSwimming(false);
            self.setPose(Pose.STANDING);
            ci.cancel();
        }
    }

    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cryoawakening$preventDropDuringInfiltration(ItemStack stack, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        Player self = (Player) (Object) this;
        boolean infiltrating = self.level().isClientSide()
            ? InfiltrationClientBridge.isClientPlayerInfiltrating(self)
            : InfiltrationServerHandler.isPlayerInfiltrating(self);

        if (infiltrating) {
            cir.setReturnValue(null);
        }
    }
}
