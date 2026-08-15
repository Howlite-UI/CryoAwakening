package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.worldgen.CryoWorldGenConfig;
import com.howlite.cryoawakening.worldgen.biome.ModBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * OverworldBiomeBuilderMixin
 *
 * Injection de `cryo_caverns` dans le MultiNoise de l'Overworld sous forme de poches ciblées.
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void injectCryoCaverns(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
            CallbackInfo ci
    ) {
        ResourceKey<Biome> biomeKey = ModBiomes.INSTANCE.getCRYO_CAVERNS();

        // Parameter points ciblés définis dans CryoWorldGenConfig
        // Crée des poches d'environ 200-300 blocs englobant précisément la cathédrale de glace
        mapper.accept(Pair.of(
            Climate.parameters(
                CryoWorldGenConfig.INSTANCE.getTEMPERATURE_SPAN(),
                CryoWorldGenConfig.INSTANCE.getHUMIDITY_SPAN(),
                CryoWorldGenConfig.INSTANCE.getCONTINENTALNESS_SPAN(),
                CryoWorldGenConfig.INSTANCE.getEROSION_SPAN(),
                CryoWorldGenConfig.INSTANCE.getDEPTH_SPAN(),
                CryoWorldGenConfig.INSTANCE.getWEIRDNESS_SPAN(),
                CryoWorldGenConfig.OFFSET
            ),
            biomeKey
        ));
    }
}

