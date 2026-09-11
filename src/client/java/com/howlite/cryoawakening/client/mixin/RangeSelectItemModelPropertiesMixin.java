package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.model.property.ClawshotAimingProgressProperty;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangeSelectItemModelProperties.class)
public class RangeSelectItemModelPropertiesMixin {

    @Shadow
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void cryoawakening$registerClawshotNumericProperties(CallbackInfo ci) {
        ID_MAPPER.put(
            Identifier.fromNamespaceAndPath("cryo-awakening", "clawshot/aiming_progress"),
            ClawshotAimingProgressProperty.Companion.getMAP_CODEC()
        );
    }
}
