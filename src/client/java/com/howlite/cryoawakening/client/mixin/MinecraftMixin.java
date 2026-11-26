package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.howlite.cryoawakening.client.event.ValueSettingsClientHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cancelStartUseItemWhenHoldingValueSettings(CallbackInfo ci) {
        if (ValueSettingsClientHandler.INSTANCE.isHolding()) {
            ci.cancel();
            return;
        }
        if (InfiltrationClientHandler.handleRightClick()) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$handleInfiltrationLeftClick(CallbackInfoReturnable<Boolean> cir) {
        if (InfiltrationClientHandler.handleLeftClick()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$cancelContinueAttackDuringInfiltration(boolean leftClick, CallbackInfo ci) {
        if (InfiltrationClientHandler.isAiming() || InfiltrationClientHandler.isActiveSequence()) {
            ci.cancel();
        }
    }
}
