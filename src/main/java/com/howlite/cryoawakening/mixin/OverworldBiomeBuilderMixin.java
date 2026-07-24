package com.howlite.cryoawakening.mixin;

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
 * Injection de `cryo_caverns` dans le MultiNoise de l'Overworld.
 *
 * Plage validée RTree :
 *  - Temperature : [-1.20,  1.20] (toutes les températures)
 *  - Humidity    : [-1.20,  1.20] (toutes les humidités)
 *  - Depth       : [ 0.70,  1.20] (STRICTEMENT sous-terrain profond / Y < -20)
 *  - Offset      : 0.0f (requis pour la compatibilité du RTree de /locatebiome)
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void injectCryoCaverns(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
            CallbackInfo ci
    ) {
        ResourceKey<Biome> biomeKey = ModBiomes.INSTANCE.getCRYO_CAVERNS();

        // Parameter points couvrant l'ensemble du sous-sol profond (depth >= 0.70)
        // Offset = 0.0f pour compatibilité 100% avec l'arbre RTree de /locatebiome
        mapper.accept(Pair.of(
            Climate.parameters(
                Climate.Parameter.span(-1.20f,  1.20f),  // temperature : toutes
                Climate.Parameter.span(-1.20f,  1.20f),  // humidity    : toutes
                Climate.Parameter.span(-1.20f,  1.20f),  // continentalness : toutes
                Climate.Parameter.span(-1.20f,  1.20f),  // erosion     : toutes
                Climate.Parameter.span( 0.70f,  1.20f),  // depth       : SOUTERRAIN PROFOND (Y < -20)
                Climate.Parameter.span(-1.20f,  1.20f),  // weirdness   : toutes
                0.0f                                     // offset validé RTree
            ),
            biomeKey
        ));
    }
}
