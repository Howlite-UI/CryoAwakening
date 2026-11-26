package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Masque entièrement le rendu du terrain (chunks solides/translucides) pendant la nage
 * dans les blocs avec la Perle d'Infiltration, supprimant tout artefact x-ray ou face interne.
 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$hideTerrainInVortex(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        if (InfiltrationClientHandler.isInsideBlocks()) {
            ci.cancel();
        }
    }
}
