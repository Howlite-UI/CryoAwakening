package com.howlite.cryoawakening.client.render.armor

import com.geckolib.renderer.GeoArmorRenderer
import com.howlite.cryoawakening.item.GeoArmorItem
import net.minecraft.client.renderer.entity.state.HumanoidRenderState

/**
 * Renderer GeckoLib pour l'armure / casque fossilisé.
 */
class FossilizedHelmetRenderer : GeoArmorRenderer<GeoArmorItem, HumanoidRenderState>(FossilizedHelmetModel())
