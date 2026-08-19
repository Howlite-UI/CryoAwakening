package com.howlite.cryoawakening

import com.howlite.cryoawakening.event.FossilPostureCombatHandler
import com.howlite.cryoawakening.event.GawkerCarryHandler
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.worldgen.biome.ModBiomes
import com.howlite.cryoawakening.worldgen.feature.ModFeatures
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object CryoAwakening : ModInitializer {
	const val MOD_ID: String = "cryo-awakening"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModParticleTypes.register()
		ModSounds.register()
		ModBlocks.register()
		ModItems.register()
		com.howlite.cryoawakening.entity.ModEntities.register()
		FossilPostureCombatHandler.register()
		GawkerCarryHandler.register() // Enregistre le payload et le receiver
		ModFeatures.register()       // WorldGen : Features custom (PillaredIceCave, etc.)
		ModBiomes.register()         // WorldGen : Biomes custom + BiomeModifications (CryoCaverns, etc.)

		LOGGER.info("Cryo Awakening initialized!")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
