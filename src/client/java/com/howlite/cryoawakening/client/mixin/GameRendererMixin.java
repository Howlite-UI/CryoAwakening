package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
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

import java.util.Objects;

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
    private Identifier cryoawakening$activePostEffect = null;

    @Unique
    private static final Identifier KALEIDOSCOPE_SHADER =
        Identifier.fromNamespaceAndPath("cryo-awakening", "kaleidoscope");

    @Inject(method = "tick", at = @At("TAIL"))
    private void cryoawakening$handlePostShaders(CallbackInfo ci) {
        Identifier desired = null;

        if (minecraft.player != null) {
            // Priorité 1 : Shader Vortex de la Perle d'Infiltration (traversée des blocs)
            Identifier infiltrationShader = InfiltrationClientHandler.getActivePostEffectId();
            if (infiltrationShader != null) {
                desired = infiltrationShader;
            } else {
                // Priorité 2 : Shader Kaléidoscope
                boolean isUsingKaleidoscope = minecraft.player.isUsingItem() &&
                    minecraft.player.getUseItem().is(ModItems.INSTANCE.getKALEIDOSCOPE()) &&
                    minecraft.options.getCameraType().isFirstPerson();
                if (isUsingKaleidoscope) {
                    desired = KALEIDOSCOPE_SHADER;
                }
            }
        }

        if (!Objects.equals(desired, cryoawakening$activePostEffect)) {
            cryoawakening$activePostEffect = desired;
            if (desired != null) {
                this.setPostEffect(desired);
            } else {
                this.clearPostEffect();
            }
        }
    }
}
