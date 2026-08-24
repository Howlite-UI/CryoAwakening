package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    public abstract void clearPostEffect();

    @Shadow
    private void setPostEffect(Identifier id) {}

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean cryoawakening$wasUsingKaleidoscope = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void cryoawakening$handleKaleidoscopeShader(CallbackInfo ci) {
        if (minecraft.player != null) {
            boolean isUsingKaleidoscope = minecraft.player.isUsingItem() &&
                minecraft.player.getUseItem().is(ModItems.INSTANCE.getKALEIDOSCOPE()) &&
                minecraft.options.getCameraType().isFirstPerson();

            if (isUsingKaleidoscope) {
                if (!cryoawakening$wasUsingKaleidoscope) {
                    cryoawakening$wasUsingKaleidoscope = true;
                    this.setPostEffect(Identifier.fromNamespaceAndPath("cryo-awakening", "kaleidoscope"));
                }
            } else if (cryoawakening$wasUsingKaleidoscope) {
                cryoawakening$wasUsingKaleidoscope = false;
                this.clearPostEffect();
            }
        }
    }
}
