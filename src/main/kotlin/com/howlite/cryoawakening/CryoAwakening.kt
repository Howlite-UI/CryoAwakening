package com.howlite.cryoawakening

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import com.howlite.cryoawakening.worldgen.feature.ModFeatures
import com.howlite.cryoawakening.worldgen.biome.ModBiomes

object CryoAwakening : ModInitializer {
	const val MOD_ID: String = "cryo-awakening"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModParticleTypes.register()
		ModSounds.register()
		ModBlocks.register()
		ModFeatures.register()  // WorldGen : Features custom (PillaredIceCave, etc.)
		ModBiomes.register()    // WorldGen : Biomes custom + BiomeModifications (CryoCaverns, etc.)

		LOGGER.info("Cryo Awakening initialized!")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
