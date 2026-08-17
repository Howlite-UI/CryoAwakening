package com.howlite.cryoawakening.item

import com.geckolib.animatable.GeoItem
import com.geckolib.animatable.client.GeoRenderProvider
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import net.minecraft.world.item.Item
import java.util.function.Consumer

/**
 * Classe de base pour les pièces d'armure GeckoLib.
 * Les renderers clients sont enregistrés côté client via GeoArmorItem.registerRenderProvider().
 */
open class GeoArmorItem(
    properties: Properties
) : Item(properties), GeoItem {

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Possibilité d'enregistrer des animations futures ici
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        RENDER_PROVIDERS[this]?.let { provider ->
            consumer.accept(provider)
        }
    }

    companion object {
        private val RENDER_PROVIDERS: MutableMap<Item, GeoRenderProvider> = HashMap()

        /**
         * Enregistre le GeoRenderProvider côté client (appelé dans CryoAwakeningClient).
         */
        fun registerRenderProvider(item: Item, provider: GeoRenderProvider) {
            RENDER_PROVIDERS[item] = provider
        }
    }
}
