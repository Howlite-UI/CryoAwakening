package com.howlite.cryoawakening.energy

import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import kotlin.math.max
import kotlin.math.min

/**
 * WindStorage
 *
 * Conteneur d'énergie "Vent" (Volume) simple et performant.
 */
class WindStorage(
    var capacity: Int,
    var wind: Int = 0,
    var maxReceive: Int = capacity,
    var maxExtract: Int = capacity
) {
    fun receiveWind(maxReceiveAmount: Int, simulate: Boolean = false): Int {
        if (maxReceiveAmount <= 0) return 0
        val windReceived = min(capacity - wind, min(this.maxReceive, maxReceiveAmount))
        if (!simulate) {
            wind += windReceived
        }
        return windReceived
    }

    fun extractWind(maxExtractAmount: Int, simulate: Boolean = false): Int {
        if (maxExtractAmount <= 0) return 0
        val windExtracted = min(wind, min(this.maxExtract, maxExtractAmount))
        if (!simulate) {
            wind -= windExtracted
        }
        return windExtracted
    }

    val isFull: Boolean get() = wind >= capacity
    val isEmpty: Boolean get() = wind <= 0
    val space: Int get() = max(0, capacity - wind)

    fun save(output: ValueOutput, prefix: String = "wind_") {
        output.putInt(prefix + "amount", wind)
        output.putInt(prefix + "capacity", capacity)
    }

    fun load(input: ValueInput, prefix: String = "wind_") {
        wind = input.getInt(prefix + "amount").orElse(0)
        capacity = input.getInt(prefix + "capacity").orElse(capacity)
    }
}
