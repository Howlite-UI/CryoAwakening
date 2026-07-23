package com.howlite.cryoawakening

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry

object ModParticleTypes {

    val STYLIZED_WIND: SimpleParticleType = FabricParticleTypes.simple()

    fun register() {
        Registry.register(
            BuiltInRegistries.PARTICLE_TYPE,
            CryoAwakening.id("stylized_wind"),
            STYLIZED_WIND
        )
    }
}

