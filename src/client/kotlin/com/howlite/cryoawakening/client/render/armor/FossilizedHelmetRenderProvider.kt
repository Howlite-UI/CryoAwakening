package com.howlite.cryoawakening.client.render.armor

import com.geckolib.animatable.client.GeoRenderProvider
import com.geckolib.renderer.GeoArmorRenderer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

/**
 * Fournisseur de rendu client GeckoLib pour le Fossilized Helmet.
 */
object FossilizedHelmetRenderProvider : GeoRenderProvider {

    private val renderer by lazy { FossilizedHelmetRenderer() }

    override fun getGeoArmorRenderer(stack: ItemStack, slot: EquipmentSlot): GeoArmorRenderer<*, *> {
        return renderer
    }
}
