package com.howlite.cryoawakening.client.render.armor

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.GeoRenderState
import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.item.GeoArmorItem
import net.minecraft.resources.Identifier

/**
 * Modèle GeckoLib pour le Fossilized Helmet.
 *
 * Dans GeckoLib 5 :
 * - Le fichier de modèle est placé dans `assets/cryo-awakening/geckolib/models/fossilized_helmet.geo.json`
 * - getModelResource doit renvoyer l'identifiant sans les préfixes/suffixes 'geo/' et '.geo.json'
 * - getTextureResource renvoie le chemin complet vers la texture png
 */
class FossilizedHelmetModel : GeoModel<GeoArmorItem>() {

    override fun getModelResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("fossilized_helmet")

    override fun getTextureResource(renderState: GeoRenderState): Identifier =
        CryoAwakening.id("textures/armor/fossilized_helmet.png")

    override fun getAnimationResource(animatable: GeoArmorItem): Identifier =
        CryoAwakening.id("fossilized_helmet")
}
