package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.ValueSettingsClientHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cancelStartUseItemWhenHoldingValueSettings(CallbackInfo ci) {
        if (ValueSettingsClientHandler.INSTANCE.isHolding()) {
            ci.cancel();
        }
    }
}
