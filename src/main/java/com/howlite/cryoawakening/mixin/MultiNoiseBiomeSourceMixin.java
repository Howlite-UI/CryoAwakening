package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.worldgen.CryoWorldGenConfig;
import com.howlite.cryoawakening.worldgen.biome.ModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MultiNoiseBiomeSourceMixin
 *
 * Aligne le biome CRYO_CAVERNS à 100% sur la cathédrale de glace.
 * Tout bloc 3D situé à l'intérieur de l'enveloppe de la cave renvoie automatiquement CRYO_CAVERNS.
 */
@Mixin(MultiNoiseBiomeSource.class)
public class MultiNoiseBiomeSourceMixin {

    @Unique
    private Holder<Biome> cryoCavernsHolder;

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void injectCryoCavernsBiome(
            int quartX, int quartY, int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        int blockX = quartX << 2;
        int blockY = quartY << 2;
        int blockZ = quartZ << 2;

        if (CryoWorldGenConfig.INSTANCE.isInsideCryoCavern(blockX, blockY, blockZ)) {
            if (this.cryoCavernsHolder == null) {
                MultiNoiseBiomeSource self = (MultiNoiseBiomeSource) (Object) this;
                for (Holder<Biome> holder : self.possibleBiomes()) {
                    if (holder.is(ModBiomes.INSTANCE.getCRYO_CAVERNS())) {
                        this.cryoCavernsHolder = holder;
                        break;
                    }
                }
            }
            if (this.cryoCavernsHolder != null) {
                cir.setReturnValue(this.cryoCavernsHolder);
            }
        }
    }
}
