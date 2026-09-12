package com.howlite.cryoawakening.client.model.item

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.GeoRenderState
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.item.ClawshotItem
import net.minecraft.resources.Identifier

/**
 * Modèle GeckoLib haute fidélité pour le Clawshot.
 */
class ClawshotModel : GeoModel<ClawshotItem>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("clawshot")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/item/clawshot/clawshot.png")

    override fun getAnimationResource(animatable: ClawshotItem): Identifier =
        CryoAwakening.id("clawshot")
}
