package com.howlite.cryoawakening

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent

object ModSounds {

    val CRYO_VENT_AMBIENT_KEY: ResourceKey<SoundEvent> = ResourceKey.create(
        BuiltInRegistries.SOUND_EVENT.key(),
        CryoAwakening.id("block.cryo_vent.ambient")
    )

    // Atténuation spatiale et spatialisation 3D sur un rayon de 16 blocs
    val CRYO_VENT_AMBIENT: SoundEvent = SoundEvent.createFixedRangeEvent(
        CryoAwakening.id("block.cryo_vent.ambient"),
        16.0f
    )

    fun register() {
        Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            CryoAwakening.id("block.cryo_vent.ambient"),
            CRYO_VENT_AMBIENT
        )
    }
}
