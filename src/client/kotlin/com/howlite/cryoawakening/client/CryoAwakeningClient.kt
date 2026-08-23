package com.howlite.cryoawakening.client

import com.howlite.cryoawakening.CryoAwakening
import com.howlite.cryoawakening.ModBlocks
import com.howlite.cryoawakening.ModParticleTypes
import com.howlite.cryoawakening.client.particle.StylizedWindTrailParticle
import com.howlite.cryoawakening.client.render.CryoTombBlockEntityRenderer
import com.howlite.cryoawakening.client.render.LumeshStemBlockEntityRenderer
import com.howlite.cryoawakening.client.render.armor.FossilizedHelmetRenderProvider
import com.howlite.cryoawakening.client.render.entity.GawkBombRenderer
import com.howlite.cryoawakening.client.render.entity.GawkerRenderer
import com.howlite.cryoawakening.client.render.entity.GlaciopodRenderer
import com.howlite.cryoawakening.client.render.entity.GaleBoomerangRenderer
import com.howlite.cryoawakening.entity.ModEntities
import com.howlite.cryoawakening.item.GeoArmorItem
import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.worldgen.CryoWorldGenConfig
import com.howlite.cryoawakening.worldgen.biome.ModBiomes
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.core.BlockPos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

object CryoAwakeningClient : ClientModInitializer {
	override fun onInitializeClient() {
		// Enregistrement du renderer du Glaciopod (cloporte géant articulé procédural)
		EntityRenderers.register(
			ModEntities.GLACIOPOD,
			::GlaciopodRenderer
		)

		// Enregistrement du renderer du Gawker (créature curieuse duveteuse)
		EntityRenderers.register(
			ModEntities.GAWKER,
			::GawkerRenderer
		)

		// Enregistrement du renderer de la Gawk-Bomb (bombe / mine GeckoLib)
		EntityRenderers.register(
			ModEntities.GAWK_BOMB,
			::GawkBombRenderer
		)

		// Enregistrement du renderer du Gale Boomerang (projectile 3D GeckoLib)
		EntityRenderers.register(
			ModEntities.GALE_BOOMERANG,
			::GaleBoomerangRenderer
		)

		// Enregistrement du gestionnaire de portage et lancer client du Gawker
		com.howlite.cryoawakening.client.event.GawkerClientCarryHandler.register()

		// Enregistrement du gestionnaire de ciblage et verrouillage multi-cibles du Gale Boomerang
		com.howlite.cryoawakening.client.event.BoomerangClientTargetHandler.register()

		// Enregistrement de l'élément HUD de la jauge de lancer au-dessus du crosshair
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
			net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CROSSHAIR,
			CryoAwakening.id("throw_bar"),
			com.howlite.cryoawakening.client.render.gui.ThrowBarHudElement
		)

		// Enregistrement des réticules de verrouillage Zelda TP au-dessus des mobs
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
			CryoAwakening.id("throw_bar"),
			CryoAwakening.id("boomerang_targets"),
			com.howlite.cryoawakening.client.render.gui.BoomerangTargetHudElement
		)

		// Enregistrement des renderers d'armures GeckoLib
		GeoArmorItem.registerRenderProvider(
			ModItems.FOSSILIZED_HELMET,
			FossilizedHelmetRenderProvider
		)

		// Enregistrement du renderer de la Cryo-Tomb (affiche le mob capturé dans la glace)
		BlockEntityRenderers.register(
			ModBlocks.CRYO_TOMB_BLOCK_ENTITY_TYPE,
			::CryoTombBlockEntityRenderer
		)

		// Enregistrement du renderer animé des feuilles de la plante Lumesh
		BlockEntityRenderers.register(
			ModBlocks.LUMESH_STEM_BLOCK_ENTITY_TYPE,
			::LumeshStemBlockEntityRenderer
		)

		// Enregistrement de la factory de particule StylizedWindTrailParticle
		ParticleProviderRegistry.getInstance().register(
			ModParticleTypes.STYLIZED_WIND,
			ParticleProviderRegistry.PendingParticleProvider { spriteSet ->
				StylizedWindTrailParticle.Factory(spriteSet)
			}
		)

		// Enregistrement du gestionnaire de vent de la cathédrale (tourne en cercle sur la périphérie)
		ClientTickEvents.END_CLIENT_TICK.register { client ->
			val world = client.level ?: return@register
			val player = client.player ?: return@register
			val random = world.random

			// Ne s'active que dans le biome cryo_caverns
			val biomeHolder = world.getBiome(player.blockPosition())
			if (!biomeHolder.`is`(ModBiomes.CRYO_CAVERNS)) return@register

			// Trouver le centre de la cave déterministe le plus proche
			val px = player.x
			val pz = player.z

			val gridX = Math.floorDiv(px.toInt(), CryoWorldGenConfig.CAVE_GRID_SPACING)
			val gridZ = Math.floorDiv(pz.toInt(), CryoWorldGenConfig.CAVE_GRID_SPACING)

			val domeSeed = CryoWorldGenConfig.getDomeSeed(gridX, gridZ)
			val (centerX, centerZ) = CryoWorldGenConfig.getCaveCenter(gridX, gridZ)
			val (radX, radZ) = CryoWorldGenConfig.getCaveRadii(domeSeed)

			val phi1 = CryoWorldGenConfig.hash1D(domeSeed xor 0x7A8BL) * 6.2831853
			val phi2 = CryoWorldGenConfig.hash1D(domeSeed xor 0x9C0DL) * 6.2831853
			val phi3 = CryoWorldGenConfig.hash1D(domeSeed xor 0xB41FL) * 6.2831853

			val amp1 = 0.12 + CryoWorldGenConfig.hash1D(domeSeed xor 0x1111L) * 0.08
			val amp2 = 0.08 + CryoWorldGenConfig.hash1D(domeSeed xor 0x2222L) * 0.06
			val amp3 = 0.04 + CryoWorldGenConfig.hash1D(domeSeed xor 0x3333L) * 0.04

			// Spawner occasionnellement 1 particule longue par tick (réduit la densité globale)
			if (random.nextFloat() < 0.40f) {
				val playerAngle = atan2(pz - centerZ, px - centerX)
				// Choisir un angle sur un arc plus large autour du joueur
				val theta = playerAngle + (random.nextDouble() - 0.5) * 2.2

				val harmonicDeform = 1.0 + amp1 * sin(2.0 * theta + phi1) + amp2 * cos(3.0 * theta + phi2) + amp3 * sin(5.0 * theta + phi3)

				// Positionnement sur la périphérie (dOvoid ∈ [0.65..0.82])
				val dOvoid = 0.65 + random.nextDouble() * 0.17
				val rX = radX * dOvoid * harmonicDeform
				val rZ = radZ * dOvoid * harmonicDeform

				val spawnX = centerX + cos(theta) * rX
				val spawnZ = centerZ + sin(theta) * rZ

				// Spawner plus loin (rayon de 95 blocs) pour voir le vent arriver de loin
				val dx = spawnX - px
				val dz = spawnZ - pz
				if (dx * dx + dz * dz < 95.0 * 95.0) {
					// Trouver le sol à cette colonne
					var floorY = -50
					// Scan rapide client de la hauteur de la cave
					for (y in -54..-30) {
						if (world.getBlockState(BlockPos(spawnX.toInt(), y, spawnZ.toInt())).isAir) {
							floorY = y
							break
						}
					}

					val spawnY = floorY + 1.0 + random.nextDouble() * 10.0

					// Vitesse tangentielle pour faire tourner le vent en cercle
					val speed = 0.14 + random.nextDouble() * 0.08
					// -sin(theta), cos(theta) donne le sens anti-horaire
					val xSpeed = -sin(theta) * speed
					val zSpeed = cos(theta) * speed
					// Vitesse verticale extrêmement faible pour garder le vent horizontal
					val ySpeed = 0.002 + random.nextDouble() * 0.006

					world.addAlwaysVisibleParticle(
						ModParticleTypes.STYLIZED_WIND,
						true,
						spawnX, spawnY, spawnZ,
						xSpeed, ySpeed, zSpeed
					)
				}
			}
		}
	}
}