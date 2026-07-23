package com.howlite.cryoawakening.client

import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.client.particle.StylizedWindTrailParticle
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry

object CryoAwakeningClient : ClientModInitializer {
	override fun onInitializeClient() {
		// Enregistrement de la factory de particule StylizedWindTrailParticle
		ParticleProviderRegistry.getInstance().register(
			ModParticleTypes.STYLIZED_WIND,
			ParticleProviderRegistry.PendingParticleProvider { spriteSet ->
				StylizedWindTrailParticle.Factory(spriteSet)
			}
		)
	}
}