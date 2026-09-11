package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.model.property.ClawshotAimingProperty;
import com.howlite.cryoawakening.client.model.property.ClawshotFiredProperty;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConditionalItemModelProperties.class)
public class ConditionalItemModelPropertiesMixin {

    @Shadow
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void cryoawakening$registerClawshotProperties(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.fromNamespaceAndPath("cryo-awakening", "clawshot/fired"), ClawshotFiredProperty.Companion.getMAP_CODEC());
        ID_MAPPER.put(Identifier.fromNamespaceAndPath("cryo-awakening", "clawshot/aiming"), ClawshotAimingProperty.Companion.getMAP_CODEC());
    }
}
