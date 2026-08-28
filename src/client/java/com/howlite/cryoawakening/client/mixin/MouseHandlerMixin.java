package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.ValueSettingsClientHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical != 0.0) {
            if (ValueSettingsClientHandler.INSTANCE.handleMouseScroll(vertical)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(double timeDelta, CallbackInfo ci) {
        if (ValueSettingsClientHandler.INSTANCE.isHolding()) {
            if (ValueSettingsClientHandler.INSTANCE.handleMouseMove(this.accumulatedDX, this.accumulatedDY)) {
                this.accumulatedDX = 0.0;
                this.accumulatedDY = 0.0;
                ci.cancel();
            }
        }
    }
}
