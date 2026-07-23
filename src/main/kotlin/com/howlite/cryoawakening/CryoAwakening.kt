package com.howlite.cryoawakening

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object CryoAwakening : ModInitializer {
	const val MOD_ID: String = "cryo-awakening"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModParticleTypes.register()
		ModSounds.register()
		ModBlocks.register()

		LOGGER.info("Cryo Awakening initialized!")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
