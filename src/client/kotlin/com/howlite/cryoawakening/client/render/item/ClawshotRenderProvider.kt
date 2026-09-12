package com.howlite.cryoawakening.client.render.item

import com.geckolib.animatable.client.GeoRenderProvider
import com.geckolib.renderer.GeoItemRenderer

/**
 * Fournisseur de rendu client GeckoLib pour le Clawshot.
 */
object ClawshotRenderProvider : GeoRenderProvider {

    private val renderer by lazy { ClawshotItemRenderer() }

    override fun getGeoItemRenderer(): GeoItemRenderer<*> {
        return renderer
    }
}
